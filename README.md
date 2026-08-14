# Visual Retail Discovery

Visual Retail Discovery is a fashion product discovery application that combines image search, semantic text search, outfit recommendations, and conversational styling assistance over a local product catalogue.

This project started at an event and is being continued independently as a personal engineering project.

## What It Does

- Finds visually similar products from an uploaded image.
- Supports natural-language product search using semantic embeddings.
- Recommends complementary products and lower-cost alternatives.
- Provides a conversational fashion stylist backed by an open-model provider.
- Includes a demo vendor flow for registering additional catalogue images.

The catalogue is a local demo dataset rather than a live retail inventory.

## Architecture

The application is deployed as one Spring Boot service. Spring Boot serves the REST API and the production Vite bundle from the same process.

```text
Browser
  |
  v
Spring Boot application
  |-- REST controllers for search, recommendations, chat, and vendor uploads
  |-- DJL, ONNX Runtime, YOLO, and OpenCV for image inference
  |-- Hugging Face embeddings and a JSON vector store for retrieval
  |-- Static product catalogue and frontend assets
  `-- Groq-hosted open model for conversational styling
```

The frontend can also run independently during development. Vite proxies API and catalogue requests to the local Spring Boot server.

## Tech Stack

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.x, Maven |
| Image inference | DJL, YOLOv11, ONNX Runtime, OpenCV |
| Search | Hugging Face `all-MiniLM-L6-v2`, cosine similarity, JSON vector store |
| AI styling | Groq API with `llama-3.3-70b-versatile`; optional Anthropic fallback |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, Material UI |
| 3D interface | Three.js, React Three Fiber |

## Repository Layout

```text
src/main/java/                         Spring Boot API and services
src/main/resources/static/datastore/   Demo product catalogue
src/main/resources/static/home/        Production frontend bundle
frontend/                              React + Vite source
python_module/                         Dataset and model-support scripts
*.onnx                                 Custom ONNX models
yolo11n.pt                             YOLO model
```

## API Surface

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/image/search` | Search by uploaded image |
| `POST` | `/api/image/searchtext` | Search by text description |
| `POST` | `/api/image/searchByLabel` | Search with labels and price filters |
| `POST` | `/api/image/styleIt` | Generate outfit recommendations |
| `POST` | `/api/image/register` | Register vendor product images |
| `GET` | `/api/image/fetch` | List catalogue images |
| `POST` | `/api/fashion/chat` | Ask the conversational stylist |

## Run Locally

### Prerequisites

- Java 17 or newer
- Maven 3.8 or newer, or the included Maven wrapper
- Node.js 24 and npm
- Groq API key for the chat route
- Replicate API token only for the virtual try-on route

### Configure API keys

Set environment variables before starting the backend:

```bash
export GROQ_API_KEY=your_groq_key
export REPLICATE_API_TOKEN=your_replicate_token
```

The chat and try-on integrations are optional for image and text search. The chat provider defaults to Groq. To use the Anthropic fallback instead:

```bash
export CHAT_PROVIDER=anthropic
export ANTHROPIC_API_KEY=your_anthropic_key
```

### Start the backend

```bash
./mvnw spring-boot:run
```

The API and any previously built frontend assets are available at `http://localhost:8080`.

### Start the frontend in development mode

```bash
cd frontend
npm ci
npm run dev
```

The development UI runs at `http://localhost:5173` and proxies backend requests to port 8080.

### Build the frontend for Spring Boot

```bash
cd frontend
npm run build
```

The build output is written to `src/main/resources/static/home/` and is served by Spring Boot.

### Build and run the container

```bash
docker build -t visual-retail-discovery .
docker run --rm -p 8080:8080 \
  -e GROQ_API_KEY=your_groq_key \
  -e REPLICATE_API_TOKEN=your_replicate_token \
  visual-retail-discovery
```

## Demo Routes

| Route | Access |
| --- | --- |
| `/` | Public catalogue |
| `/search` | Public visual and text search |
| `/chat` | Public stylist chat |
| `/login` | Demo login |
| `/register` | Demo registration flow |
| `/vendor` | Demo vendor upload flow |

The authentication flow is client-side demo functionality, not production account security.

## Deployment

The repository includes a Dockerfile and Render Blueprint for a single-service deployment.

For Render:

1. Create a Web Service from this repository and select Docker, or use the included `render.yaml` Blueprint.
2. Add `GROQ_API_KEY` as a secret environment variable in the Render dashboard. Do not commit the key or put it in `render.yaml`.
3. Keep `CHAT_PROVIDER=groq` unless using the optional Anthropic fallback.
4. Deploy and verify `/`, `/api/image/fetch`, and `/api/fashion/chat` from the generated `onrender.com` URL.

Render's free web service tier sleeps after inactivity and is not suitable for an always-on guarantee. The ML models also make startup and memory limits important deployment constraints.

GitHub Pages and Vercel are appropriate for a static frontend, but neither can host this Java inference backend. A genuinely always-on free deployment is not generally available as a reliable managed service; it requires a suitable always-free VM and more operational setup.
