import sys
import asyncio

# Force Windows Selector Event Loop Policy for Playwright compatibility
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

import uvicorn
import os

if __name__ == "__main__":
    uvicorn.run(
        "ai_worker:app",
        host="0.0.0.0",
        port=int(os.environ.get("PORT", os.environ.get("AI_WORKER_PORT", "8001"))),
        reload=False,
    )
