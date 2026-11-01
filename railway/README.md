# Deploy BureauZero on Railway

The `.railway/railway.ts` file defines two services and a persistent volume:

- `app`: public Spring Boot dashboard and API.
- `ai`: private FastAPI + Playwright worker.
- `bureauzero-receipts`: a 1 GB volume mounted at `/data` for the H2 receipt ledger.

## One-time setup

1. Push this repository to GitHub. The config points to `AryanGupta833/bureauzero-platform` on the `main` branch; edit those values in `.railway/railway.ts` if your fork or branch differs.
2. Install the current Railway CLI, then sign in and create/link the new Railway project from the repository root:

   ```sh
   railway login
   railway init
   ```
3. In the Railway environment, create a shared variable named `GEMINI_API_KEY` and set it to your Google AI API key. Document extraction and DOB correction use the `gemini-3.8-flash` model; you can override it with `GEMINI_MODEL`. Leave the API key unset if those features are not part of the demo.
4. Choose `us-west2` for the app and worker service region to match the persistent volume region in the config. If you choose another region, change `region` in `.railway/railway.ts` to the same region before applying.
5. Preview and apply the Railway configuration from the repository root:

   ```sh
   railway config plan
   railway config apply
   ```

   Review the plan to confirm it creates both services and the attached volume before confirming the apply.
6. Generate a public domain for the `app` service in Railway’s service Settings → Networking. Do not generate a public domain for the `ai` service.

The app binds to Railway’s injected `PORT`; the worker listens on its private port 8001. Both services use Railway private networking. The volume is required to keep receipt history across redeploys.

## Deploy updates

After the services are connected to the repository branch, push changes to trigger automatic deploys. You can also use Railway’s dashboard to redeploy a service.
