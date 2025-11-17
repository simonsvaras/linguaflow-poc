# LinguageFlow Camunda Worker

Spring Boot 3.3 application (Java 21) that connects to Camunda 8 SaaS, exposes the `pricing.compute` worker and a REST webhook for correlating the `PaymentReceived` message.

## Requirements

- Java 21 (set `JAVA_HOME` or let Gradle toolchain download it)
- Gradle Wrapper (included)
- Camunda 8 SaaS cluster credentials (Zeebe API client)

## Configuration

All secrets are provided via environment variables (or a local `.env` loaded by your shell tooling). No credentials live in the repo.

| Variable | Description |
| --- | --- |
| `SERVER_PORT` | Optional HTTP port (`8080` default). |
| `CAMUNDA_REGION` | SaaS region, e.g. `fra-1`. |
| `CAMUNDA_CLUSTER_ID` | Cluster ID from Camunda Console. |
| `CAMUNDA_CLIENT_ID` | OAuth client id for Zeebe API. |
| `CAMUNDA_CLIENT_SECRET` | OAuth client secret. |
| `CAMUNDA_AUTH_URL` | OAuth token endpoint (`https://login.cloud.camunda.io/oauth/token` default). |

`zeebe.gateway` is derived automatically from `clusterId` + `region` and can be overridden if necessary.

Example `.env` fragment:

```env
CAMUNDA_REGION=fra-1
CAMUNDA_CLUSTER_ID=abcdef123456
CAMUNDA_CLIENT_ID=xxx
CAMUNDA_CLIENT_SECRET=xxx
SERVER_PORT=8080
```

## Running locally

```bash
./gradlew bootRun
```

On startup the app authenticates with Camunda SaaS, registers the `pricing.compute` worker, and exposes `POST /webhooks/payment`.

### Pricing worker

- Job type: `pricing.compute`
- Computes quotes using base rate `0.8 CZK/word`, coefficients for formality/urgency, and falls back to defaults (`wordCount=1000`, `formality=neutral`, `urgency=normal`).
- Responds with:

```json
{
  "quote": {
    "totalPrice": 1234,
    "currency": "CZK",
    "expirationAt": "2025-01-01T12:00:00Z"
  }
}
```

- Throws BPMN error `PRICING_FAILED` on failure.

### Payment webhook

`POST /webhooks/payment`

```json
{
  "orderId": "LF-2025-00123",
  "variables": {
    "paidAt": "2025-11-12T09:00:00Z"
  }
}
```

The controller validates `orderId`, correlates the `PaymentReceived` message (variables default to `{}` if missing), and returns `202 Accepted`.

## Testing

```bash
./gradlew test
```

Included tests:

- `PricingCalculatorTest` – unit coverage for the pure quote math.
- `MessageControllerTest` – MVC-style smoke test for the webhook/Zeebe publish flow (Zeebe client mocked).
