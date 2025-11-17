# LinguaFlow Pricing Service & Camunda Worker

A Spring Boot 3.3 / Java 21 microservice that centralizes pricing logic for LinguaFlow translations. It exposes the quote
calculation over REST, provides a Camunda 8 Zeebe worker (`pricing.compute`), and accepts payment webhooks that correlate the
`PaymentReceived` message back into running processes.

## What the service does

| Capability | Description |
| --- | --- |
| Pricing REST API | `POST /api/pricing/quote` receives a validated `PricingRequest`, computes the total price from configurable rates/coefficients, and returns a rich `PricingResponse` (total, currency, expiration, per-rule details). |
| Pricing worker for Camunda | Registers the `pricing.compute` job type on startup (can be disabled via `lf.workers.pricing.enabled=false`), converts Zeebe variables to a `PricingRequest`, completes the job with the full quote, or throws BPMN error `PRICING_FAILED` on failure. |
| Payment webhook | `POST /webhooks/payment` validates the incoming payload and correlates the Camunda message `PaymentReceived` using the provided `orderId` and optional variables. |
| Extensible pricing rules | Pricing logic is composed from `PricingRule` beans (`BaseRateRule`, `FormalityRule`, `UrgencyRule`). Adding discounts, taxes, or other adjustments only requires another `@Component` implementing the same interface. |

### Pricing algorithm

The pricing pipeline lives in `pricing.service.PricingServiceImpl` and applies the following calculation:

```
raw = baseRate × wordCount × formalityCoef × urgencyCoef
(total is rounded up to whole CZK)
```

* **Configuration first** – `PricingProperties` binds `pricing.base-rate`, `pricing.currency`, `pricing.expiration-duration`, and the maps of coefficients from `application.yml`, so no magic numbers live in code.
* **Validation** – `PricingRequest` uses Jakarta validation annotations: `@NotNull`, `@Min(1)` for `wordCount`; enums (`Formality`, `Urgency`) are enforced by Jackson. Invalid input results in `IllegalArgumentException` mapped to `400 Bad Request` by `PricingExceptionHandler`.
* **Expiration** – each quote gets `expirationAt = Clock.systemUTC().instant() + expirationDuration` (48h by default, configurable via ISO-8601 duration).
* **Details for debugging** – the response includes the applied base rate and coefficients so that operators can understand how the total was derived.

## Architecture at a glance

```
controller
├── pricing.api.PricingController (REST)
├── pricing.api.PricingExceptionHandler (400 mapping)
└── api.MessageController (payment webhook)
service
├── pricing.service.PricingService / PricingServiceImpl
├── pricing.service.PricingContext (mutable aggregate used by rules)
└── pricing.rules.* (BaseRateRule, FormalityRule, UrgencyRule)
domain
└── pricing.domain.* (request/response DTOs + enums)
integration
└── workers.PricingWorker (Zeebe job handler)
config
└── pricing.config.PricingProperties (@ConfigurationProperties)
```

## Configuration

### Mandatory environment variables

| Variable | Description |
| --- | --- |
| `SERVER_PORT` | Optional HTTP port (`8080` default). |
| `CAMUNDA_REGION` | Camunda SaaS region, e.g. `fra-1`. |
| `CAMUNDA_CLUSTER_ID` | Cluster ID from Camunda Console. |
| `CAMUNDA_CLIENT_ID` | OAuth client id for the Zeebe API. |
| `CAMUNDA_CLIENT_SECRET` | OAuth client secret. |
| `CAMUNDA_AUTH_URL` | OAuth token endpoint (`https://login.cloud.camunda.io/oauth/token` default). |

`zeebe.gateway` is derived from region + cluster but can be overridden via property if necessary.

### Pricing knobs (`application.yml`)

```yaml
pricing:
  base-rate: 0.8           # CZK per word
  currency: CZK
  expiration-duration: PT48H
  formality-coefs:
    NEUTRAL: 1.0
    FORMAL: 1.2
    CREATIVE: 1.3
  urgency-coefs:
    NORMAL: 1.0
    RUSH: 1.4
    OVERNIGHT: 1.6
```

Change the values (or use `application-*.yml`) to reflect real price books, currencies, or SLAs.

## Running the service

```bash
./gradlew bootRun
```

On startup the app:
1. Loads pricing configuration and registers all available `PricingRule` beans.
2. Connects to Camunda 8 SaaS using the Zeebe client configuration in `ZeebeClientConfig`.
3. Registers the `pricing.compute` worker unless disabled.
4. Exposes the REST controllers on the configured HTTP port.

### ...or run everything via Docker Compose

> Requires Docker/Docker Compose v2.24+.

1. Copy the example env file and fill in your Camunda credentials:
   ```bash
   cp ../../.env.example ../../.env   # from this README's directory
   # edit ../../.env to set CAMUNDA_* and (optionally) SERVER_PORT
   ```
2. From the repository root, build and start the container:
   ```bash
   docker compose up --build
   ```
3. The service is available on `http://localhost:${SERVER_PORT:-8080}` and will automatically register the `pricing.compute` worker using the credentials from `.env`.

To stop the container, press `Ctrl+C` or run `docker compose down` in another terminal. The image is rebuilt whenever the source changes, so the Compose workflow stays in sync with your codebase.

## Calling the REST API

```bash
curl -X POST http://localhost:8080/api/pricing/quote \
  -H 'Content-Type: application/json' \
  -d '{
        "orderId": "LF-2025-0001",
        "wordCount": 2500,
        "sourceLang": "cs",
        "targetLang": "en",
        "formality": "FORMAL",
        "urgency": "RUSH"
      }'
```

Successful response (example):

```json
{
  "orderId": "LF-2025-0001",
  "totalPrice": 3360,
  "currency": "CZK",
  "expirationAt": "2025-01-15T10:00:00Z",
  "details": {
    "baseRate": 0.8,
    "formalityCoef": 1.2,
    "urgencyCoef": 1.4
  }
}
```

*Validation errors* (e.g., missing word count) return `400` with a descriptive message.
*Unexpected exceptions* return `500` and are logged with stack traces.

## Using the Camunda worker

1. **Model the task** – In your BPMN model, create a Service Task with job type `pricing.compute`.
2. **Provide variables** – When the task runs, ensure `wordCount`, `formality`, `urgency`, and optional `orderId` are present. The worker is forgiving and falls back to `wordCount=1000`, `formality=neutral`, `urgency=normal`, and `orderId=bpmnProcessId`, but providing accurate values yields accurate quotes.
3. **Result payload** – The worker completes the job with a single variable `quote` that contains the same structure as `PricingResponse`. Downstream tasks can reuse `quote.totalPrice`, `quote.expirationAt`, etc.
4. **Error handling** – Any exception triggers `PRICING_FAILED` BPMN error, so configure boundary events if you want to react to pricing failures.

To disable the worker locally (e.g., for tests), start the app with `-Dlf.workers.pricing.enabled=false` or set the property in `application.yml`.

## Payment webhook endpoint

`POST /webhooks/payment` expects a JSON body:

```json
{
  "orderId": "LF-2025-0001",
  "variables": {
    "paidAt": "2025-03-01T10:00:00Z",
    "paymentMethod": "CARD"
  }
}
```

The controller validates that `orderId` is present (`@NotBlank`) and publishes the `PaymentReceived` message to Zeebe with the provided variables. It responds with `202 Accepted` once the message is correlated.

## Testing

```bash
./gradlew --console=plain test
```

Included coverage:

- `pricing.service.PricingServiceImplTest` – unit tests for the pricing pipeline (math, expiration, validation paths).
- `pricing.api.PricingControllerTest` – full-stack MVC test for the REST endpoint (success + validation failure), with the Zeebe worker disabled.
- `api.MessageControllerTest` – tests correlating the payment webhook with a mocked Zeebe client.
- `LinguageFlowPocApplicationTests` – smoke test to ensure the Spring context loads.

## Next steps & extensions

Because pricing logic is rule-based, adding features such as discounts, VAT, loyalty modifiers, or tiered currencies only requires
another `PricingRule` bean that reads from configuration and mutates the `PricingContext`. Likewise, more controllers or workers can reuse `PricingService` without duplicating business logic.
