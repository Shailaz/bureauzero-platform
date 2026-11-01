import { defineRailway, github, project, service, volume } from "railway/iac";

export default defineRailway(() => {
  const receiptData = volume("bureauzero-receipts", {
    region: "us-west2",
    sizeMB: 1024,
  });

  const app = service("app", {
    source: github("AryanGupta833/bureauzero-platform", {
      branch: "main",
      rootDirectory: "/",
    }),
    build: {
      builder: "DOCKERFILE",
      dockerfilePath: "spring-boot-core/Dockerfile",
    },
    healthcheck: "/",
    healthcheckTimeout: 300,
    volumeMounts: {
      "/data": receiptData,
    },
    env: {
      BUREAUZERO_DATA_DIR: "/data",
      H2_CONSOLE_ENABLED: "false",
      PYTHON_AI_URL: "http://${{ai.RAILWAY_PRIVATE_DOMAIN}}:${{ai.PORT}}",
    },
    deploy: {
      restartPolicyType: "ON_FAILURE",
      restartPolicyMaxRetries: 10,
    },
  });

  const ai = service("ai", {
    source: github("AryanGupta833/bureauzero-platform", {
      branch: "main",
      rootDirectory: "/",
    }),
    build: {
      builder: "DOCKERFILE",
      dockerfilePath: "python-ai-agent/Dockerfile",
    },
    env: {
      AI_WORKER_HOST: "0.0.0.0",
      AI_WORKER_PORT: "8001",
      SPRING_BOOT_BASE_URL: "http://${{app.RAILWAY_PRIVATE_DOMAIN}}:${{app.PORT}}",
      PLAYWRIGHT_HEADLESS: "true",
      GEMINI_API_KEY: "${{shared.GEMINI_API_KEY}}",
      GEMINI_MODEL: "gemini-3.8-flash",
    },
    deploy: {
      restartPolicyType: "ON_FAILURE",
      restartPolicyMaxRetries: 10,
    },
  });

  return project("BureauZero", {
    resources: [app, ai, receiptData],
  });
});
