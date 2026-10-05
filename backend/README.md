# Portfolio-chat

Java 21/Javalin → Dify Knowledge API → OpenAI Responses API. Hugo remains on GitHub Pages.
The chatbot uses one Dify High Quality knowledge base, GPT-6 Luna with reasoning `none`,
plain-text answers and validated source links. It does not upload, re-chunk or modify your documents.

## First deployment (Linux / DigitalOcean)

1. Install Docker Engine with Compose and Caddy on the host. Point an API hostname at the server.
2. Copy this `backend` directory to the server. Copy `.env.example` to `.env`, restrict it with
   `chmod 600 .env`, and fill in the three credentials/identifiers. Never send keys in chat or commit them.
3. In OpenAI, create a **dedicated project and key only for this bot**, add credit, disable automatic
   recharge if unwanted, and verify that the project has access to `gpt-6-luna`.
4. In Dify Cloud → Knowledge → Service API, create a key scoped to your knowledge base.
   Copy the dataset UUID from its URL. API access must be enabled and indexing complete.
   Keep the existing embedding model. Requests select semantic search, top 4, score threshold 0.3,
   and disable reranking. A restrictive Cloud quota may still limit retrieval.
5. Optionally add string metadata `source_url` to each document, pointing to its **full HTTPS
   portfolio page URL** (including `/Portfolio/`). Otherwise sources display document names.
6. Run:

   ```sh
   docker compose build
   docker compose run --rm chat --init-budget
   docker compose up -d
   curl http://127.0.0.1:8080/health
   ```

   Initialization is a deliberate **one-time** operation. It refuses to overwrite an existing file.
   Normal startup refuses a missing/corrupt database; it never creates fresh credit automatically.
7. Run Caddy on the host using `Caddyfile.example` with your hostname. Open ports 80/443, keeping
   the backend bound to loopback. If you already have a proxy, use it instead, preserving the header rule.
8. Set `TRUSTED_PROXY_IPS` to the exact proxy peer address seen by the container. For native Linux
   Docker bridge networking with a host proxy, inspect the bridge gateway:

   ```sh
   docker network inspect portfolio-chat_default --format '{{(index .IPAM.Config 0).Gateway}}'
   docker compose up -d
   ```

   Verify the peer for your deployment, especially on Docker Desktop. Trust only your proxy and
   always overwrite `X-Real-IP` there. Without a trusted proxy configured, rate limiting remains
   active but all visitors through it share one limit. Browser-supplied `X-Forwarded-For` is ignored.
9. Set `params.chatbot.apiBase` in `hugo.yaml` to `https://YOUR-API-HOST` (without `/api/chat`).
   Keep `enabled: true`, build/publish Hugo normally, then try the smoke tests below.
   A blank URL displays a polite unavailable message and sends no network requests.

## Budget and limits

- Internal lifetime ceiling: **$4.90** against the $5 allocation; no monthly reset.
- Only this backend's OpenAI answer generation is counted. **Dify retrieval/embeddings,
  document indexing, hosting, taxes and other project activity are outside this ledger.**
- Every answer reserves $0.00115 atomically before generation: 6,000 input tokens at the maximum
  configured input/cache-write rate plus 800 output tokens. OpenAI's input-token counting endpoint
  checks the exact request, including the structured-output schema. Oversized context is shortened.
- Pricing constants are pinned to GPT-6 Luna Standard short-context pricing checked 2026-10-04:
  input $0.10, cache read $0.01, cache write $0.125, output $0.50 per million tokens.
  `service_tier=default`; no regional premium endpoint. Recheck official pricing before first use
  and after provider changes. The local ceiling relies on these rates and exclusive use of the key;
  it is not a guarantee about the entire OpenAI account invoice.
- Settlement uses integer nano-USD. Missing cache-write detail is conservatively charged at the
  cache-write rate. Missing/invalid usage, transport failures and timeouts retain the full reservation.
  There are **no automatic generation retries**, including after a model error. A manual retry is a new call.
- Retained reservations survive crashes. They may exhaust the allowance early. Do not release them
  without reconciling the corresponding provider usage. No public reset/budget admin endpoint exists.
- Rolling limits: 5 requests/minute, 50/24 hours per salted IP hash, persisted in SQLite. Entries expire
  after 24 hours. Origin checks are not authentication; a public bot's shared budget can still be used up.
- At most two requests are processed concurrently. Deploy **one replica**; a filesystem lock prevents
  two production processes sharing the same volume. Do not run independent volumes with the same API key.
- Health checks test the ledger, without paid upstream calls. Logs contain status, timing and token/cost
  totals; no questions, answers, keys or raw IP addresses. Dify/OpenAI retain data according to their settings.
- Chat history stays in the browser tab's sessionStorage (up to 15 turns). Only the last three pairs
  are submitted to the backend. `Ny samtale` clears it. `store:false` disables saved OpenAI Responses;
  this is not a promise of zero provider retention.

## Backup and move to another server

Never run `docker compose down -v` for a deployed bot. Keep the same database when redeploying.
Stop the service for a consistent backup of the whole SQLite directory, including WAL files:

```sh
docker compose stop chat
docker run --rm -v portfolio-chat-budget:/data:ro -v "$PWD":/backup alpine:3.21 tar czf /backup/portfolio-chat-budget.tgz -C /data .
docker compose start chat
```

Protect the archive; do not put it in Git or Hugo's static files. To migrate: stop the **old** instance,
take a final backup, copy backend code, `.env` and archive, then on the new server:

```sh
docker compose build
docker volume create portfolio-chat-budget
docker run --rm -v portfolio-chat-budget:/data -v "$PWD":/backup:ro alpine:3.21 tar xzf /backup/portfolio-chat-budget.tgz -C /data
docker compose up -d
```

Do **not** run `--init-budget` during migration. Preserve ownership UID/GID 10001. Update proxy/DNS
and `apiBase` if necessary. Never restart the old instance after the new one goes live. Restoring an older
backup can undercount later spending; reconcile that gap with OpenAI before allowing further calls.

## Development and verification

```sh
mvn verify
docker compose build
```

From the repository root, run `npm ci --prefix tests/frontend` and `npm test --prefix tests/frontend`
for the widget's DOM tests (history, safe rendering, error states and reset during a pending request).
These run under jsdom; they do not replace a real-browser visual/mobile check.
The `Verify chatbot` GitHub Actions workflow runs both test suites without provider credentials.

Automated tests use local mock Dify/OpenAI servers and temporary SQLite databases; no real keys or
paid calls are required. For local manual development use Java 21, initialize `data/budget.db`,
set the environment variables, and run `java -jar target/portfolio-chat.jar`. Add
`http://localhost:1313` to `ALLOWED_ORIGINS`, and use a Hugo development override for the API URL.
Local HTTP API URLs are accepted by the widget only when the website itself is on localhost.

After production configuration, perform a few **paid** smoke tests (included in the allowance):

- Ask about an indexed AIDA topic and a D&D assignment; open each source link and check the answer.
- Ask a follow-up and repeat in English. Ask about something absent from the documents; no invented answer.
- Check the welcome panel, loading/errors, keyboard/Escape, `Ny samtale`, navigation, both themes and mobile.
- Confirm browser traffic contains no API keys and the API receives only the last three history pairs.
- Verify `GET /health` and inspect `docker compose logs --tail 30 chat` for usage/status.

API: `POST /api/chat` with `{ "message": "...", "history": [{ "role": "user", "content": "..." },
{ "role": "assistant", "content": "..." }] }`, exact allowed `Origin`, and JSON content type.
The response is `{ "answer": "...", "sources": [{ "id": "S1", "documentId": "...", "chunkId": "...",
"title": "...", "url": null }] }`. History is optional, ordered in complete user/assistant pairs,
maximum six messages. Question limit: 250 Unicode code points; assistant history limit: 4,000.
Errors return `{ "error": "code", "message": "..." }` with 400/403/415/429/503; oversized bodies get 413.
Empty retrieval skips OpenAI. Failed/incomplete model responses return a safe error, with spend accounted for.

References: [OpenAI model/pricing](https://developers.openai.com/api/docs/models/gpt-6-luna),
[input-token counting](https://developers.openai.com/api/docs/guides/token-counting),
[Dify retrieval](https://docs.dify.ai/en/api-reference/knowledge-bases/retrieve-chunks-from-a-knowledge-base-test-retrieval).
