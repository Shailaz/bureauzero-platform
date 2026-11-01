# 🏛️ BureauZero Platform

[![Python Version](https://img.shields.io/badge/python-3.10%2B-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.100%2B-teal.svg)](https://fastapi.tiangolo.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Playwright](https://img.shields.io/badge/Playwright-Async-orange.svg)](https://playwright.dev/python/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-3.8%20Flash-purple.svg)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**BureauZero** is a full-stack, autonomous AI-driven platform engineered to eliminate bureaucratic friction and automate welfare claim submissions on complex legacy government portals.

By integrating a secure Spring Boot control dashboard with a high-performance Python FastAPI state engine, headless browser automation (Playwright), and **Google Gemini 3.8 Flash**, BureauZero autonomously handles DOM obstacles, validates citizen data, recovers from portal validation errors in real-time, and generates cryptographic audit receipts.

---

## 📐 System Architecture & Workflow

BureauZero uses a decoupled micro-architecture designed for resilience and auditability.

```text
+-----------------------------------------------------------------+
|              Spring Boot Control Dashboard (Port 8080)          |
|                                                                 |
|  - User Session Management & Profile Uploads                    |
|  - Cryptographic Audit Receipt & SHA-256 Verification Viewer    |
+-----------------------------------------------------------------+
                              |
                              | HTTP JSON Telemetry
                              v
+-----------------------------------------------------------------+
|           Python FastAPI AI & Playwright State Engine           |
|                                                                 |
|  1. Document Data Extraction (Gemini 3.8 Flash)                 |
|  2. Headless Browser Navigation & DOM Inspection (Playwright)   |
|  3. Obstacle Bypass (Dismissing Modal Intercepts)               |
|  4. Self-Correction Loop (Catching 422/DOM Errors & Fixing DOB) |
|  5. SHA-256 Cryptographic Receipt Scraping                      |
+-----------------------------------------------------------------+
```

## 🗂️ Project Directory Structure

```text
bureauzero-platform/
│
├── python-ai-agent/
│   ├── venv/
│   ├── ai_worker.py
│   ├── start.py
│   └── requirements.txt
│
├── spring-boot-core/
│   ├── src/main/
│   │   ├── java/com/bureauzero/
│   │   └── resources/
│   │       ├── static/
│   │       └── templates/
│   └── pom.xml
│
└── README.md
```

## 🚀 Key Features & Capabilities

- **Intelligent Document Parsing:** Upload citizen ID cards, certificates, or identity proofs to extract structured demographic parameters using Gemini 3.8 Flash with Pydantic JSON schema validation.
- **Autonomous DOM Navigation:** Headless or headed Chromium automation via Playwright to navigate portal application flows reliably.
- **Dynamic Obstacle Bypassing:** Automatically detects and dismisses intrusive layout obstacles, such as blocking Gazette Circular modal popups, that disrupt standard web scrapers.
- **AI-Powered Self-Correction Loop:** When legacy portal validation rejects input fields (e.g., non-standard Date of Birth formats), the agent captures the exact DOM error text, queries Gemini 3.8 Flash for a contextual format correction, patches the input field, and resubmits seamlessly.
- **Cryptographic Audit Signatures:** Scrapes official transaction hashes, registry statuses, and unique receipt IDs upon successful filing.

## 📊 Core API Endpoints

### 1. Extract Profile Telemetry

- **Endpoint:** `POST /ai/extract-profile`
- **Payload:** Multipart form-data containing an official citizen document/image file.
- **Response:** Returns verified structured JSON matching `CitizenSchema`.

Example response:

```json
{
  "fullName": "string",
  "dob": "YYYY-MM-DD",
  "age": 22,
  "gender": "Male",
  "domicileState": "Assam",
  "socialCategory": "General",
  "annualIncome": 145000,
  "citizenIdNumber": "string",
  "recommendedScheme": "National Post-Matric Merit Scholarship"
}
```

### 2. Execute Autonomous Workflow

- **Endpoint:** `POST /ai/execute-workflow`
- **Payload:** JSON containing citizen profile data and `target_mode` (`spring_legacy_portal` or `myscheme_live_discovery`).
- **Response:** Returns execution status, agent reasoning trace logs with base64 screenshots per step, and final cryptographic receipt details.

## 🛠️ Installation & Quickstart Guide

### Prerequisites

- Java Development Kit (JDK 21+) and Maven
- Python 3.10+ installed and added to system PATH
- Google Gemini API key (`GEMINI_API_KEY`)

### Step 1: Clone the Repository

```bash
git clone https://github.com/Shailaz/bureauzero-platform.git
cd bureauzero-platform
```

### Step 2: Set Up the Python AI Worker

Navigate to the Python worker directory, configure the virtual environment, and install dependencies.

```powershell
cd python-ai-agent
python -m venv venv

.\venv\Scripts\Activate.ps1

pip install -r requirements.txt

playwright install chromium
```

### Step 3: Configure the Gemini API

Copy `.env.example` to `.env` and set your Gemini API key and model.

```powershell
$env:GEMINI_API_KEY = "your_actual_api_key_here"
$env:GEMINI_MODEL = "gemini-3.8-flash"
```

### Step 4: Run the AI State Engine

Launch the FastAPI server via Uvicorn.

```powershell
python start.py
```

The server will initialize on:

`http://127.0.0.1:8001`

with Windows Selector Event Loop optimizations.

## 💡 Tech Stack

- **AI & Machine Learning:** Google GenAI SDK (`gemini-3.8-flash`), Pydantic structural validation
- **Browser Automation:** Playwright Async API for Python, Chromium
- **Backend State Engine:** Python, FastAPI, Uvicorn
- **Control Center Dashboard:** Java, Spring Boot, Thymeleaf
- **OS Environment Compatibility:** Windows PowerShell and Command Prompt optimized with Selector Event Loop policies

## 📜 License

This project is licensed under the MIT License. See the `LICENSE` file for details.
