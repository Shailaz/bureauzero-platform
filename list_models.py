import os

from openai import OpenAI


def main() -> int:
    api_key = os.environ.get("OPENROUTER_API_KEY")
    if not api_key:
        print("OPENROUTER_API_KEY is not set in this terminal.")
        return 1

    endpoint = os.environ.get(
        "OPENAI_URL", "https://openrouter.ai/api/v1/chat/completions"
    ).rstrip("/")
    if endpoint.endswith("/chat/completions"):
        endpoint = endpoint[: -len("/chat/completions")]
    client = OpenAI(api_key=api_key, base_url=endpoint)
    for model in client.models.list().data:
        print(model.id)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
