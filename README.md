# Voice AI Chatbot Platform

A production-ready, multi-tenant voice AI chatbot platform built with a microservice architecture. The system supports real-time voice interaction — streaming audio from hardware clients (ESP/robot devices), converting speech to text, generating AI responses via LLM, synthesizing speech with a custom TTS model, and delivering audio back to clients over WebSocket.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        Clients                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  React Web   │  │  Robot/ESP   │  │  Admin Dashboard │  │
│  │  (BHXH Chat) │  │  (Python mic)│  │  (REST API)      │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼─────────────────┼───────────────────┼────────────┘
          │ HTTP/WebSocket   │ WebSocket Binary   │ HTTP/JWT
          ▼                  ▼                   ▼
┌──────────────────────────────────────────────────────────────┐
│              Backend  (Spring Boot 3 / Java 17)              │
│  JWT Auth · WebSocket Handler · REST API · Redis Queue       │
│  Admin: Users / Groups / Prompts / RAG Docs                  │
└──────────────────┬───────────────────────────────────────────┘
                   │ Redis Pub/Sub + Queue
          ┌────────┴──────────────────────┐
          ▼                               ▼
┌──────────────────────┐     ┌────────────────────────────┐
│   AI Chat Workers    │     │   TTS Workers              │
│   (Python / Vertex)  │     │   (Python / OmniVoice)     │
│   LangChain + Memory │     │   Streaming MP3/PCM        │
└──────────────────────┘     └────────────────────────────┘
          │                               │
          ▼                               ▼
┌──────────────────────┐     ┌────────────────────────────┐
│   Embedding Workers  │     │   voice_ready:{userId}     │
│   SentenceTransformer│     │   → Redis Pub/Sub          │
│   Qdrant RAG         │     │   → WS push to client      │
└──────────────────────┘     └────────────────────────────┘

Infrastructure: PostgreSQL · Redis · Qdrant
```

---

## Key Features

- **Real-time voice pipeline** — WebSocket binary streaming, STT (Soniox), LLM response, TTS synthesis, audio delivery in one round-trip
- **Multi-tenant** — Groups with independent system prompts; each group gets its own AI persona and RAG knowledge base
- **Scalable AI workers** — Python workers consume from Redis queues; scale horizontally with `--scale`
- **Custom TTS** — OmniVoice model with multiple Vietnamese voice profiles, streaming MP3 output
- **RAG (Retrieval-Augmented Generation)** — Upload PDF/Word documents, chunked and embedded into Qdrant for semantic search
- **Sliding-window memory + summarization** — Per-user conversation history with automatic background summarization via Vertex AI
- **Role-based access control** — Admin and User roles enforced via Spring Security + JWT
- **Hardware client support** — Python client for ESP/robot devices that streams raw PCM from microphone over WebSocket

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend API | Java 17, Spring Boot 3.2, Spring Security, Spring WebSocket |
| ORM / DB migration | Spring Data JPA, Flyway |
| AI / LLM | Google Vertex AI (Gemini), LangChain Python |
| TTS | OmniVoice (custom voice cloning model), lameenc MP3 |
| STT | Soniox Streaming API |
| Embeddings | SentenceTransformer (`all-MiniLM-L6-v2`) |
| Vector DB | Qdrant |
| Message queue | Redis 7 (Pub/Sub + List queue) |
| Relational DB | PostgreSQL 15 |
| Frontend | React 18, Vite, Tailwind CSS |
| Hardware client | Python (PyAudio mic streaming) |
| Containerization | Docker, Docker Compose |

---

## Project Structure

```
.
├── backend-java/               # Spring Boot API server
│   └── src/main/java/com/voiceai/
│       ├── auth/               # JWT auth, Spring Security filter
│       ├── admin/              # Admin controllers (users, groups, prompts, RAG)
│       ├── client/             # Client-facing chat + RAG endpoints
│       ├── websocket/          # WebSocket handler + JWT handshake interceptor
│       ├── redis/              # Redis queue service (push tasks, Pub/Sub)
│       ├── model/              # JPA entities: User, Group, Prompt, Role
│       └── config/             # CORS, Redis, Security, Jackson configs
│
├── AI_Service/
│   ├── chat_service_robot/     # AI chat worker (Vertex AI + LangChain memory)
│   ├── embetdding_service/     # Embedding worker (SentenceTransformer + Qdrant)
│   └── tts_service/            # TTS worker (OmniVoice streaming synthesis)
│       └── omnivoice/          # Model code: training, inference, evaluation
│
├── client/                     # Python robot client (mic streaming over WebSocket)
├── ui-ux/                      # React frontend (BHXH chatbot UI)
└── docker-compose.yml          # PostgreSQL + Redis + Qdrant + Backend
```

---

## Data Model

```
role ──< users >── groups ──< prompts ──< summaryprompts
                                │
                             Qdrant collection (embeddings per groupId)
```

- **Groups** — organizational unit (e.g. a company deploying a chatbot)
- **Prompts** — system prompt defining the AI persona for each group
- **Users** — belong to a group; `clientType` is either `human` (web) or `robot` (hardware device)

---

## How a Voice Request Flows

1. Robot client streams raw PCM audio frames over WebSocket binary
2. Backend receives frames and forwards to Soniox STT (streaming HTTP)
3. Transcribed text is pushed as a task onto a Redis list queue
4. An AI chat worker picks up the task, runs RAG retrieval from Qdrant, queries Vertex AI, returns LLM text
5. LLM text is pushed to a TTS worker queue
6. TTS worker synthesizes speech chunk-by-chunk using OmniVoice, encodes to MP3, stores the file
7. TTS worker publishes `voice_ready:{userId}` on Redis Pub/Sub with text + audio URL
8. Backend WebSocket handler receives the Pub/Sub event and pushes `AI_VOICE_REPLY` to the client session

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Python 3.11+ with `uv` (for AI services)
- Java 17 + Maven (for backend, or use the provided Dockerfile)

### 1. Start infrastructure

```bash
docker compose up -d
```

Starts: PostgreSQL (port 5431), Redis (port 6378), Qdrant (port 6333), and the Spring Boot backend (port 3000).

### 2. Start AI workers

```bash
# Chat workers (scale as needed)
cd AI_Service/chat_service_robot
docker compose up --scale ai_worker=3

# Embedding worker
cd AI_Service/embetdding_service
uv run python worker.py

# TTS worker (GPU recommended)
cd AI_Service/tts_service
uv run python wordker.py
```

### 3. Start the web frontend

```bash
cd ui-ux
npm install
npm run dev
```

Frontend available at `http://localhost:5173`.

### 4. Start the robot client

```bash
cd client
uv run python main.py
```

---

## Environment Variables (Backend)

| Variable | Description | Default |
|---|---|---|
| `HOST_DB` / `PORT_DB` | PostgreSQL host and port | `localhost` / `5431` |
| `NAME_DB` / `USER_DB` / `PASS_DB` | Database credentials | see `docker-compose.yml` |
| `HOST_REDIS` / `PORT_REDIS` / `PASS_REDIS` | Redis connection | see `docker-compose.yml` |
| `AUTH_TOKEN` | JWT signing secret | — |
| `STT` | Soniox API key | — |
| `UPLOAD_DIR` | Directory for uploaded RAG documents | `uploads` |

---

## API Highlights

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Login, returns JWT |
| `GET/POST` | `/api/chat` | Send a text message, get AI reply |
| `WS` | `/ws/chat` | WebSocket connection for voice clients |
| `POST` | `/api/rag/upload` | Upload document for RAG |
| `POST` | `/admin/users` | Create user (admin only) |
| `POST` | `/admin/groups` | Create group with prompt (admin only) |
| `GET` | `/health` | Health check |

---

## Notable Design Decisions

- **Redis as the async backbone** — All cross-service communication goes through Redis queues and Pub/Sub, keeping services fully decoupled. The Java backend never calls Python services directly.
- **Per-group prompt isolation** — Each tenant group has its own system prompt cached in Redis at WebSocket connect time (`warmupPromptCache`), avoiding DB reads on every message.
- **Sliding-window + async summarization** — The last 6 messages are kept in memory; older messages are summarized in a background thread to stay under LLM context limits without blocking the main response.
- **OmniVoice streaming** — TTS output is generated sentence-by-sentence and streamed as MP3 chunks, reducing time-to-first-audio.

---

## Author

**Duy Do** — [dev.dinhduy@gmail.com](mailto:dev.dinhduy@gmail.com)
