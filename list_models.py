import os

from google import genai


def main() -> int:
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("GEMINI_API_KEY is not set in this terminal.")
        return 1

    client = genai.Client(api_key=api_key)
    for model in client.models.list():
        if "flash" in model.name.lower():
            print(model.name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
