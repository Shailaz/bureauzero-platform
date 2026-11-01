import sys
import asyncio

# CRITICAL WINDOWS FIX: Force Selector event loop policy for Playwright subprocess communication
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

import os
import json
import base64
import logging
from typing import Literal
from pathlib import Path
from dotenv import load_dotenv
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, AliasChoices, ConfigDict
from google import genai
from google.genai import types
from playwright.async_api import async_playwright

# Resolve the shared repository-level .env no matter which directory starts
# the worker (PowerShell, cmd.exe, macOS, Linux, or an IDE).
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

app = FastAPI(title="BureauZero Python AI & Playwright State Engine")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

@app.on_event("startup")
async def startup_event():
    if sys.platform == "win32":
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    if not (os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")):
        logging.getLogger(__name__).warning(
            "GEMINI_API_KEY/GOOGLE_API_KEY is unset; document extraction and legacy DOB correction are unavailable. "
            "Live myScheme discovery can still run."
        )

def get_llm_client():
    api_key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
    if not api_key:
        raise HTTPException(
            status_code=503,
            detail="Set GEMINI_API_KEY (or GOOGLE_API_KEY) on the AI worker to enable document extraction and DOB correction.",
        )
    return genai.Client(api_key=api_key)

def get_llm_model():
    return os.environ.get("GEMINI_MODEL", "gemini-3.8-flash")

def extract_json_content(response):
    content = response.text
    if not content:
        raise RuntimeError("The selected Gemini model returned an empty response.")
    text = content.strip()
    if text.startswith("```"):
        text = text.removeprefix("```json").removeprefix("```").removesuffix("```").strip()
    return json.loads(text)

class CitizenSchema(BaseModel):
    fullName: str = Field(min_length=1)
    dob: str = Field(min_length=1)
    age: int = Field(ge=0, le=130)
    gender: Literal["Male", "Female", "Other", "Transgender"]
    domicileState: str = Field(min_length=1)
    socialCategory: Literal["General", "OBC", "SC", "ST"]
    annualIncome: int = Field(ge=0)
    citizenIdNumber: str = Field(min_length=1)

    recommendedScheme: Literal[
        "National Post-Matric Merit Scholarship",
        "Digital India Student Hardware Grant",
        "Mukhyamantri Yuva Swarojgar Subsidy",
    ] = Field(
        validation_alias=AliasChoices(
            "recommendedScheme",
            "recommended_scheme"
        )
    )

    model_config = ConfigDict(extra="forbid", populate_by_name=True, str_strip_whitespace=True)

class ExtractedCitizenSchema(BaseModel):
    fullName: str
    dob: str
    age: int
    gender: Literal["Male", "Female", "Other"]
    domicileState: str
    socialCategory: Literal["General", "OBC", "SC", "ST"]
    annualIncome: int
    citizenIdNumber: str
    recommendedScheme: Literal[
        "National Post-Matric Merit Scholarship",
        "Digital India Student Hardware Grant",
        "Mukhyamantri Yuva Swarojgar Subsidy",
    ] = Field(
        validation_alias=AliasChoices("recommendedScheme", "recommended_scheme")
    )

    # Google's structured-output API rejects additionalProperties in the
    # response schema, so keep this extraction schema to supported fields.
    model_config = ConfigDict(populate_by_name=True, str_strip_whitespace=True)

class SelfCorrectionSchema(BaseModel):
    reasoning: str
    corrected_dob: str

@app.post("/ai/extract-profile")
async def extract_profile(file: UploadFile = File(...)):
    raw_bytes = await file.read()
    client = get_llm_client()
    prompt = (
        "Extract the citizen's demographic details from this official document or ID card. "
        "Do not guess or invent personal details. If a value is not visible, return an empty string for text fields and 0 for numeric fields so the user can fill it in. "
        "Map gender to exactly one of: 'Male', 'Female', or 'Other'. "
        "Map domicileState to one of: 'Assam', 'Delhi', 'Karnataka', or 'Maharashtra'. "
        "Map socialCategory to one of: 'General', 'OBC', 'SC', or 'ST'. "
        "Map recommendedScheme to one of: 'National Post-Matric Merit Scholarship', "
        "'Digital India Student Hardware Grant', or 'Mukhyamantri Yuva Swarojgar Subsidy'."
    )

    mime_type = file.content_type
    if not mime_type or mime_type == "application/octet-stream":
        import mimetypes
        mime_type = mimetypes.guess_type(file.filename or "")[0] or mime_type or "application/octet-stream"
    if not raw_bytes:
        raise HTTPException(status_code=400, detail="The uploaded document is empty.")
    if mime_type != "application/pdf" and not mime_type.startswith("image/"):
        raise HTTPException(status_code=415, detail="Upload a PDF or image file for profile extraction.")
    response = client.models.generate_content(
        model=get_llm_model(),
        contents=[prompt, types.Part.from_bytes(data=raw_bytes, mime_type=mime_type)],
        config=types.GenerateContentConfig(
            response_mime_type="application/json",
            response_schema=ExtractedCitizenSchema,
            temperature=0.1,
        ),
    )
    profile = ExtractedCitizenSchema.model_validate(extract_json_content(response))
    return {"status": "EXTRACTED", "profile": profile.model_dump()}

class AgentJobRequest(BaseModel):
    profile: CitizenSchema
    target_mode: Literal["spring_legacy_portal", "myscheme_live_discovery"] = "spring_legacy_portal"

@app.post("/ai/execute-workflow")
async def execute_workflow(req: AgentJobRequest):
    p = req.profile.model_dump()
    if req.target_mode == "spring_legacy_portal":
        if p["gender"] not in {"Male", "Female", "Other"}:
            raise HTTPException(status_code=422, detail="The legacy portal accepts Male, Female, or Other for gender.")
        if p["domicileState"] not in {"Assam", "Delhi", "Karnataka", "Maharashtra"}:
            raise HTTPException(status_code=422, detail="The legacy portal accepts Assam, Delhi, Karnataka, or Maharashtra for domicile state.")
    trace_logs = []
    headless = os.environ.get("PLAYWRIGHT_HEADLESS", "true").lower() not in {"0", "false", "no"}

    async def record_step(page, step_num: int, reasoning_text: str):
        png_bytes = await page.screenshot(type="png")
        b64_img = base64.b64encode(png_bytes).decode("utf-8")
        trace_logs.append({
            "step": step_num,
            "reasoning": reasoning_text,
            "url": page.url,
            "screenshot": b64_img
        })

    async with async_playwright() as pw:
        browser = await pw.chromium.launch(headless=headless, slow_mo=600)
        page = await browser.new_page(viewport={"width": 1100, "height": 820})

        if req.target_mode == "myscheme_live_discovery":
            await page.goto("https://www.myscheme.gov.in/find-scheme", timeout=30000)
            await record_step(page, 1, "Opened the live Government of India myScheme scheme finder.")

            # The public myScheme finder asks for gender and age first, then
            # domicile state. Use the portal's own wizard and scrape its result
            # cards instead of treating a successful page load as a match.
            gender = str(p["gender"]).strip().title()
            age = str(int(p["age"]))
            state = str(p["domicileState"]).strip()
            allowed_genders = {"Male", "Female", "Transgender"}
            allowed_states = {
                "Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar",
                "Chandigarh", "Chhattisgarh", "Dadra & Nagar Haveli and Daman & Diu", "Delhi", "Goa",
                "Gujarat", "Haryana", "Himachal Pradesh", "Jammu and Kashmir", "Jharkhand", "Karnataka",
                "Kerala", "Ladakh", "Lakshadweep", "Madhya Pradesh", "Maharashtra", "Manipur",
                "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Puducherry", "Punjab", "Rajasthan",
                "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
            }
            if gender not in allowed_genders:
                raise ValueError(f"myScheme does not support the profile gender value '{gender}'.")
            if state not in allowed_states:
                raise ValueError(f"myScheme does not support the profile domicile state '{state}'.")

            # The portal puts a styled label/card over each visually-hidden
            # radio input. Clicking the input directly is intercepted by that
            # card in Chromium; click its associated label instead.
            gender_input = page.get_by_role("radio", name=gender).first
            gender_input_id = await gender_input.get_attribute("id")
            if not gender_input_id:
                raise RuntimeError(f"Could not identify the myScheme {gender} option.")
            await page.locator(f'label[for="{gender_input_id}"]').click()
            await page.locator("select").first.select_option(label=age)
            await page.get_by_role("button", name="Next", exact=True).click()
            await page.get_by_role("heading", name="Please select your state").wait_for(timeout=15000)
            await page.locator("select").first.select_option(label=state)
            await record_step(page, 2, f"Entered the demo profile's age ({age}), gender ({gender}), and domicile state ({state}) in the myScheme finder.")

            # Area of residence is not part of the dashboard profile. The
            # portal allows this optional refinement to be skipped.
            await page.get_by_role("button", name="Skip to results", exact=True).click()
            await page.get_by_text("based on your preferences").wait_for(timeout=20000)
            await record_step(page, 3, "myScheme returned scheme matches for the entered profile filters.")

            body_text = await page.locator("body").inner_text()
            import re
            count_match = re.search(r"We found ([\d,]+) schemes? based on your preferences", body_text)
            result_count = count_match.group(1) if count_match else "an unreported number of"
            result_cards = page.locator("[id^='scheme-name-']")
            schemes = []
            for i in range(min(await result_cards.count(), 10)):
                card = result_cards.nth(i)
                link = card.locator("a").first
                name = (await link.inner_text()).strip()
                href = await link.get_attribute("href")
                schemes.append({"name": name, "url": f"https://www.myscheme.gov.in{href}" if href and href.startswith("/") else href})

            title = await page.title()
            await browser.close()
            return {
                "status": "COMPLETED",
                "final_receipt_summary": f"Live myScheme discovery completed on {title}.\nFilters submitted: {age}yo {gender}, {state}.\nmyScheme reported {result_count} matches. Showing up to {len(schemes)} results.",
                "schemes": schemes,
                "agent_reasoning_trace": trace_logs
            }

        # MODE 1: Autonomous Execution on Legacy Gov Portal
        spring_base_url = os.environ.get("SPRING_BOOT_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
        target_url = f"{spring_base_url}/gov-portal/apply"
        await page.goto(target_url)
        await record_step(page, 1, "Navigated to Legacy Portal. Inspecting DOM tree for blocking overlays...")

        # STEP 2: Detect & Dismiss Blocking Bureaucratic Modal Obstacle
        modal = page.locator("#gazette-modal")
        if await modal.is_visible():
            await record_step(page, 2, "OBSTACLE DETECTED: Blocking Gazette Circular #402/2026 modal intercepting DOM clicks. Dispatching click to #dismiss-modal-btn.")
            await page.click("#dismiss-modal-btn")
            await record_step(page, 3, "Modal obstacle cleared. Form 8B DOM inputs are now interactive.")

        # STEP 3: Populate Form Fields with Citizen Telemetry
        await page.fill("#fullName", str(p["fullName"]))
        await page.fill("#dobFormatted", str(p["dob"]))
        await page.fill("#age", str(p["age"]))
        await page.select_option("#gender", str(p["gender"]))
        await page.select_option("#domicileState", str(p["domicileState"]))
        await page.select_option("#socialCategory", str(p["socialCategory"]))
        await page.fill("#annualIncome", str(p["annualIncome"]))
        await page.select_option("#schemeName", str(p["recommendedScheme"]))
        await page.fill("#citizenIdNumber", str(p["citizenIdNumber"]))
        await page.check("#consentCheck")

        await record_step(page, 4, f"Populated all 9 legacy form fields (DOB entered as '{p['dob']}'). Attempting initial form submission...")
        await page.click("#submitPortalBtn")

        # STEP 4: Autonomous DOM Error Detection & Gemini Self-Correction Loop
        err_banner = page.locator("#validation-error")
        if await err_banner.is_visible():
            err_text = await err_banner.inner_text()
            await record_step(page, 5, f"DOM VALIDATION ERROR DETECTED: '{err_text}'. Invoking Gemini self-correction agent...")

            correction_prompt = (
                    f"The legacy government portal rejected the form with this exact DOM error: '{err_text}'. "
                    f"The current DOB value entered was '{p['dob']}'. "
                    "Convert this date into the exact format demanded by the portal error message. "
                    "Return JSON with reasoning and corrected_dob fields."
            )
            client = get_llm_client()
            fix_response = client.models.generate_content(
                model=get_llm_model(),
                contents=correction_prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=SelfCorrectionSchema,
                    temperature=0.0,
                ),
            )
            fix_data = SelfCorrectionSchema.model_validate(extract_json_content(fix_response)).model_dump()
            corrected_dob = fix_data["corrected_dob"]

            await page.fill("#dobFormatted", corrected_dob)
            await record_step(page, 6, f"SELF-CORRECTION APPLIED: Gemini reformatted DOB from '{p['dob']}' -> '{corrected_dob}' ({fix_data['reasoning']}). Resubmitting form...")
            await page.click("#submitPortalBtn")

        # STEP 5: Continue to the explicitly local demo OTP page. A real OTP
        # challenge is never guessed or auto-filled by this worker.
        await page.wait_for_selector("#otpCode", timeout=10000)
        if not page.url.startswith(f"{spring_base_url}/gov-portal/verify"):
            raise RuntimeError("An OTP field appeared outside the local demo portal; stopping for human verification.")
        demo_otp = (await page.locator("#demoOtpHint").inner_text()).strip()
        if not demo_otp.isdigit() or len(demo_otp) != 6:
            raise RuntimeError("The local demo OTP prompt did not contain a valid six-digit test code.")
        await page.fill("#otpCode", demo_otp)
        await record_step(page, 7, "Entered the displayed local demo OTP and continued to receipt verification.")
        await page.click("#verifyOtpBtn")

        # STEP 6: Wait for Cryptographic Receipt & Scrape Verification Details
        await page.wait_for_selector("#official-receipt", timeout=10000)
        receipt_id = await page.locator("#receiptId").inner_text()
        tx_hash = await page.locator("#txHash").inner_text()
        db_status = await page.locator("#dbStatus").inner_text()

        await record_step(page, 8, f"SUCCESS: Legacy form accepted! Extracted Official Receipt {receipt_id} (SHA-256: {tx_hash}).")
        await browser.close()

        summary = (
            f"Official Receipt ID: {receipt_id}\n"
            f"Applicant: {p['fullName']} ({p['domicileState']})\n"
            f"Scheme: {p['recommendedScheme']}\n"
            f"SHA-256 Audit Signature: {tx_hash}\n"
            f"Registry Status: {db_status}"
        )
        return {
            "status": "COMPLETED",
            "final_receipt_summary": summary,
            "agent_reasoning_trace": trace_logs
        }
