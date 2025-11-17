# Linguaflow Camunda Worker

Spring Boot 3.3 application (Java 21) that connects to Camunda 8 SaaS, exposes the `pricing.compute` Zeebe worker and a REST webhook for correlating the `PaymentReceived` message.

## Requirements

- Java 21 (set `JAVA_HOME` accordingly)
- Maven 3.9+
- Camunda 8 SaaS cluster credentials with the `pricing.compute` service task configured in a process

## Configuration

All secrets/config values are injected via environment variables. You can export them in your shell or load them from a local `.env` file (remember `.env` is ignored by git).

| Variable | Description |
| --- | --- |
| `SERVER_PORT` | Optional HTTP port (defaults to `8080`). |
| `CAMUNDA_REGION` | SaaS region, e.g. `fra-1`. |
| `CAMUNDA_CLUSTER_ID` | Cluster ID from Camunda 8 console. |
| `CAMUNDA_CLIENT_ID` | OAuth client id for the Zeebe API client. |
| `CAMUNDA_CLIENT_SECRET` | OAuth client secret. |
| `CAMUNDA_AUTH_URL` | OAuth token endpoint (defaults to `https://login.cloud.camunda.io/oauth/token`). |

The Zeebe gateway endpoint is derived automatically as `<CLUSTER_ID>.<REGION>.zeebe.camunda.io:443`.

Example `.env` snippet:

```env
CAMUNDA_REGION=fra-1
CAMUNDA_CLUSTER_ID=abcdef123456
CAMUNDA_CLIENT_ID=xxx
CAMUNDA_CLIENT_SECRET=xxx
SERVER_PORT=8080
```

## Running locally

```bash
mvn spring-boot:run
```

The application connects to the Zeebe gateway on startup, registers the `pricing.compute` worker and exposes the REST endpoint at `POST /webhooks/payment`.

### Pricing worker

- Job type: `pricing.compute`
- Calculates a quote using base price `0.8 CZK/word`, formality + urgency coefficients and a default `wordCount` of 1000 if not provided.
- Returns variables in the shape:

```json
{
  "quote": {
    "totalPrice": 1234,
    "currency": "CZK",
    "expirationAt": "2025-01-01T12:00:00Z"
  }
}
```

- On error it logs the failure and throws BPMN error `PRICING_FAILED`.

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

The controller validates `orderId`, correlates the `PaymentReceived` message using the Zeebe client and responds with `202 Accepted`.

## Testing

```bash
mvn test
```

Included tests:

- `PricingCalculatorTest` – unit test for the pure price calculation logic.
- `MessageControllerTest` – MVC test that verifies the webhook endpoint orchestrates the Zeebe publish command.
