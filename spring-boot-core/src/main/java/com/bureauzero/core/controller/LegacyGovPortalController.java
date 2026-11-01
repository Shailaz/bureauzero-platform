package com.bureauzero.core.controller;

import com.bureauzero.core.entity.CitizenApplication;
import com.bureauzero.core.repository.ApplicationRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/gov-portal")
public class LegacyGovPortalController {

    private final ApplicationRepository repository;

    public LegacyGovPortalController(ApplicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/apply", produces = MediaType.TEXT_HTML_VALUE)
    public String renderLegacyPortal() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>e-District National Welfare Portal v2.4</title>
            <style>
                body { font-family: Tahoma, sans-serif; background: #e2e8f0; margin: 0; padding: 25px; }
                .container { max-width: 680px; margin: auto; background: #fff; padding: 28px; border-top: 8px solid #1e3a8a; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                .field { margin-bottom: 14px; }
                label { font-weight: bold; font-size: 13px; color: #1e293b; display: block; margin-bottom: 5px; }
                input, select { width: 100%; padding: 9px; border: 1px solid #94a3b8; border-radius: 4px; box-sizing: border-box; }
                .error-banner { display: none; background: #fee2e2; color: #991b1b; border: 1px solid #f87171; padding: 12px; margin-bottom: 16px; font-weight: bold; }
                #gazette-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.65); display: flex; align-items: center; justify-content: center; z-index: 999; }
                .modal-box { background: white; padding: 24px; max-width: 420px; border-top: 5px solid #d97706; text-align: center; }
                .btn { background: #1e3a8a; color: white; padding: 12px 24px; border: none; font-weight: bold; cursor: pointer; width: 100%; }
            </style>
        </head>
        <body>
            <div id="gazette-modal">
                <div class="modal-box">
                    <h3>Important Circular #402/2026</h3>
                    <p>All applicants must verify Aadhaar seeding and strictly enter Date of Birth in DD/MM/YYYY format before proceeding.</p>
                    <button id="dismiss-modal-btn" onclick="document.getElementById('gazette-modal').style.display='none'" style="background:#d97706; color:white; padding:10px 18px; border:none; cursor:pointer; font-weight:bold;">
                        I Acknowledge & Close Notice
                    </button>
                </div>
            </div>

            <div class="container">
                <h2>Directorate of Public Welfare — Form 8B (Legacy Portal)</h2>
                <div id="validation-error" class="error-banner"></div>

                <form id="govForm" action="/gov-portal/verify" method="POST" onsubmit="return validateLegacyRules()">
                    <div class="field">
                        <label>Full Name of Applicant (Strictly as per Govt ID):</label>
                        <input type="text" id="fullName" name="fullName" required>
                    </div>
                    <div class="field">
                        <label>Date of Birth (Strict Format: DD/MM/YYYY):</label>
                        <input type="text" id="dobFormatted" name="dobFormatted" placeholder="DD/MM/YYYY" required>
                    </div>
                    <div class="field">
                        <label>Completed Age (Years):</label>
                        <input type="number" id="age" name="age" required>
                    </div>
                    <div class="field">
                        <label>Gender:</label>
                        <select id="gender" name="gender" required>
                            <option value="">-- Select Gender --</option>
                            <option value="Male">Male</option>
                            <option value="Female">Female</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>Domicile State:</label>
                        <select id="domicileState" name="domicileState" required>
                            <option value="">-- Select State --</option>
                            <option value="Assam">Assam</option>
                            <option value="Delhi">Delhi</option>
                            <option value="Karnataka">Karnataka</option>
                            <option value="Maharashtra">Maharashtra</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>Social Category:</label>
                        <select id="socialCategory" name="socialCategory" required>
                            <option value="">-- Select Category --</option>
                            <option value="General">General</option>
                            <option value="OBC">OBC</option>
                            <option value="SC">SC</option>
                            <option value="ST">ST</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>Annual Family Income (INR - Numeric Only):</label>
                        <input type="number" id="annualIncome" name="annualIncome" required>
                    </div>
                    <div class="field">
                        <label>Scheme Applied For:</label>
                        <select id="schemeName" name="schemeName" required>
                            <option value="National Post-Matric Merit Scholarship">National Post-Matric Merit Scholarship</option>
                            <option value="Digital India Student Hardware Grant">Digital India Student Hardware Grant</option>
                            <option value="Mukhyamantri Yuva Swarojgar Subsidy">Mukhyamantri Yuva Swarojgar Subsidy</option>
                        </select>
                    </div>
                    <div class="field">
                        <label>Verified Government ID / Certificate Number:</label>
                        <input type="text" id="citizenIdNumber" name="citizenIdNumber" required>
                    </div>
                    <div class="field">
                        <label><input type="checkbox" id="consentCheck" required style="width:auto;"> I certify under penalty of perjury that the above details match official records.</label>
                    </div>
                    <button type="submit" id="submitPortalBtn" class="btn">Submit Official Welfare Claim</button>
                </form>
            </div>

            <script>
                function validateLegacyRules() {
                    const dob = document.getElementById('dobFormatted').value.trim();
                    const errBox = document.getElementById('validation-error');
                    const regex = /^\\d{2}\\/\\d{2}\\/\\d{4}$/;
                    if (!regex.test(dob)) {
                        errBox.style.display = 'block';
                        errBox.innerText = 'PORTAL VALIDATION ERROR [CODE 422]: Invalid Date of Birth format "' + dob + '". Legacy system strictly requires DD/MM/YYYY with forward slashes!';
                        return false;
                    }
                    return true;
                }
            </script>
        </body>
        </html>
        """;
    }

    @PostMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public String renderOtpStep(
            @RequestParam String fullName,
            @RequestParam String dobFormatted,
            @RequestParam Integer age,
            @RequestParam String gender,
            @RequestParam String domicileState,
            @RequestParam String socialCategory,
            @RequestParam Long annualIncome,
            @RequestParam String schemeName,
            @RequestParam String citizenIdNumber,
            HttpSession session
    ) {
        session.setAttribute("fullName", fullName);
        session.setAttribute("dobFormatted", dobFormatted);
        session.setAttribute("age", age);
        session.setAttribute("gender", gender);
        session.setAttribute("domicileState", domicileState);
        session.setAttribute("socialCategory", socialCategory);
        session.setAttribute("annualIncome", annualIncome);
        session.setAttribute("schemeName", schemeName);
        session.setAttribute("citizenIdNumber", citizenIdNumber);
        return renderOtpChallenge("");
    }

    @PostMapping(value = "/submit", produces = MediaType.TEXT_HTML_VALUE)
    public String handleFormSubmission(@RequestParam String otpCode, HttpSession session) throws Exception {
        if (!"123456".equals(otpCode)) {
            return renderOtpChallenge("The demo code did not match. Enter the code shown below.");
        }

        String fullName = (String) session.getAttribute("Name");
        String dobFormatted = (String) session.getAttribute("dobFormatted");
        Integer age = (Integer) session.getAttribute("age");
        String gender = (String) session.getAttribute("gender");
        String domicileState = (String) session.getAttribute("domicileState");
        String socialCategory = (String) session.getAttribute("socialCategory");
        Long annualIncome = (Long) session.getAttribute("annualIncome");
        String schemeName = (String) session.getAttribute("schemeName");
        String citizenIdNumber = (String) session.getAttribute("citizenIdNumber");
        if (fullName == null || dobFormatted == null || age == null || gender == null || domicileState == null
                || socialCategory == null || annualIncome == null || schemeName == null || citizenIdNumber == null) {
            return renderOtpChallenge("This verification session expired. Restart the application form.");
        }

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String receiptId = "NIC-GOV-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest((receiptId + citizenIdNumber + now).getBytes(StandardCharsets.UTF_8));
        String txHash = HexFormat.of().formatHex(hashBytes);

        CitizenApplication application = new CitizenApplication();
        application.setReceiptId(receiptId);
        application.setFullName(fullName);
        application.setDobFormatted(dobFormatted);
        application.setAge(age);
        application.setGender(gender);
        application.setDomicileState(domicileState);
        application.setSocialCategory(socialCategory);
        application.setAnnualIncome(annualIncome);
        application.setSchemeName(schemeName);
        application.setCitizenIdNumber(citizenIdNumber);
        application.setStatus("SANCTION_PENDING_VERIFIED");
        application.setCryptographicHash(txHash);
        application.setSubmittedAt(now);
        CitizenApplication saved = repository.save(application);
        session.invalidate();

        return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: Tahoma, sans-serif; background: #f0fdf4; padding: 40px;">
            <div id="official-receipt" style="max-width: 600px; margin: auto; background: white; border: 3px solid #15803d; padding: 28px;">
                <h2 style="color: #15803d; margin-top: 0;">Government e-District Acknowledgment Slip</h2>
                <p><strong>Official Receipt ID:</strong> <span id="receiptId">%s</span></p>
                <p><strong>Applicant Name:</strong> %s</p>
                <p><strong>DOB Verified:</strong> %s</p>
                <p><strong>Domicile State:</strong> %s</p>
                <p><strong>Scheme Sanctioned:</strong> %s</p>
                <p><strong>SHA-256 Audit Signature:</strong> <code id="txHash">%s</code></p>
                <p><strong>Database Status:</strong> <span id="dbStatus">%s</span></p>
            </div>
        </body>
        </html>
        """.formatted(
                saved.getReceiptId(),
                saved.getFullName(),
                saved.getDobFormatted(),
                saved.getDomicileState(),
                saved.getSchemeName(),
                saved.getCryptographicHash(),
                saved.getStatus()
        );
    }

    private String renderOtpChallenge(String errorMessage) {
        String errorBlock = errorMessage.isBlank() ? "" : "<div class=\"error-banner\" style=\"display:block\">" + errorMessage + "</div>";
        return """
        <!DOCTYPE html>
        <html>
        <head><title>Verify demo OTP</title></head>
        <body style="font-family:Tahoma,sans-serif;background:#e2e8f0;padding:25px">
            <main style="max-width:560px;margin:auto;background:white;padding:28px;border-top:8px solid #1e3a8a">
                <h2>Step 2 of 2 — Verify the local demo code</h2>
                <p>This simulated OTP challenge is only for the local test portal.</p>
                <p>Demo OTP: <code id="demoOtpHint">123456</code></p>
                %s
                <form action="/gov-portal/submit" method="POST">
                    <label for="otpCode">Six-digit code</label>
                    <input id="otpCode" name="otpCode" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" required>
                    <button id="verifyOtpBtn" type="submit">Verify code and continue</button>
                </form>
            </main>
        </body>
        </html>
        """.formatted(errorBlock);
    }
}
