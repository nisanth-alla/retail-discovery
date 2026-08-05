
# Visual Retail Discovery

An AI-powered fashion e-commerce discovery platform combining visual search, smart outfit recommendations, and conversational AI styling assistance.


### Business Objective
Enable end-users to discover fashion products by uploading an image or describing an item in text, and receive visually similar product matches along with complementary outfit recommendations


## Overview

The objective is to build a web-based fashion product discovery application that transforms the retail journey from "Keyword Search" to "Visual Discovery." The application empowers users to use their camera as a bridge between real-world inspiration and a product catalog — not just to find a product, but to curate a lifestyle.
Users can find visually similar fashion items using image recognition and/or natural language descriptions, operating on publicly available datasets rather than live e-commerce catalogs.

Visual Retail Discovery is built around three core pillars:

1. **Visual Search** — Upload an image or type a query to find similar fashion products using semantic embeddings and YOLO-based object detection.
2. **Style Synthesis & Smart Swaps** — Get outfit completion suggestions and value-optimized alternatives based on detected items.
3. **Fashion AI Chat** — Conversational styling assistant powered by Anthropic Claude, with multi-turn context and image-aware recommendations.

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Spring Boot 3.x (Java 17) | REST API framework |
| DJL (Deep Java Library) 0.36.0 | ML model inference (PyTorch, ONNX) |
| YOLOv11n | Fashion object detection |
| HuggingFace `all-MiniLM-L6-v2` | Semantic text & image embeddings |
| ONNX Runtime 1.17.0 | Custom ONNX model inference |
| OpenCV 4.9.0 | Image processing |
| Anthropic Claude API (`claude-sonnet-4-20250514`) | AI fashion chat |
| SpringDoc OpenAPI 2.8.x | API documentation |

### Frontend
| Technology | Purpose |
|---|---|
| React 19 + TypeScript | UI framework |
| Vite | Build tool & dev server |
| Tailwind CSS | Styling |
| Material-UI (MUI) | UI component library |
| Three.js + React Three Fiber | 3D avatar in chat interface |
| React Router v6 | Client-side routing |

---

## Project Structure

```
binary_brains_cs04_2026_aih/
├── src/main/java/com/innova/visual_retail_discovery/
│   ├── controller/           # REST API endpoints
│   ├── service/
│   │   ├── anthropic/        # Anthropic Claude integration
│   │   ├── detector/         # YOLO object detection
│   │   ├── embeddings/       # Text & image embeddings
│   │   ├── engine/           # Style rule engine
│   │   ├── style/            # Style synthesis & smart swaps
│   │   ├── vector/           # JSON vector store
│   │   └── translator/       # YOLO model translators
│   ├── model/                # Request/response data models
│   └── data/                 # Data initialization
├── src/main/resources/static/
│   ├── datastore/            # 146 fashion product images (dataset)
│   └── home/                 # Built frontend (served by Spring Boot)
├── frontend/                 # React + Vite source
│   └── src/
│       ├── pages/            # Login, Register, Search, Vendor, Chat, Home
│       ├── components/       # UI components
│       ├── context/          # Auth, search, chat state
│       └── services/         # API client
├── embeddings/               # Persisted vector index (JSON)
├── models/                   # ML model files
├── binary_brains_m1.onnx     # Custom ONNX model
└── yolo11n.pt                # YOLOv11 Nano model
```

---

## API Endpoints

### Image Search (`/api/image`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/image/search` | Visual search by uploaded image |
| POST | `/api/image/register` | Register vendor product images |
| GET  | `/api/image/fetch` | Fetch all catalog images |
| POST | `/api/image/searchByLabel` | Search by image + label + price filter |
| POST | `/api/image/searchtext` | Text-based semantic search |
| POST | `/api/image/styleIt` | Style suggestions from text or image |


### Fashion Chat (`/api/fashion`)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/fashion/chat` | Multi-turn AI fashion stylist chat |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ and npm (for frontend development)
- Anthropic API key

### Configuration

Set your Anthropic API key in `src/main/resources/application.properties`:

```properties
anthropic.api.key=YOUR_API_KEY_HERE
anthropic.model=claude-sonnet-4-20250514
anthropic.api.url=https://api.anthropic.com/v1/messages

app.upload.dir=src/main/resources/static/datastore
spring.servlet.multipart.max-file-size=50MB
```

### Build & Run

#### Backend
```bash
./mvnw spring-boot:run
```
The backend starts on `http://localhost:8080`.

#### Frontend (Development)
```bash
cd frontend
npm install
npm run dev
```
The dev server starts on `http://localhost:5173` with a proxy to the backend.

#### Build Frontend for Production
```bash
cd frontend
npm run build
```
Built assets are output to `src/main/resources/static/home/` and served by Spring Boot.

---

## Features

### Visual Search
- Drag-and-drop or file-picker image upload
- YOLO detects fashion items in the uploaded image
- Semantic similarity search against the indexed product catalog
- Text search with natural language queries
- Filter results by label and price

### Style Synthesis
- **Complete the Look:** Detects items in an uploaded image and recommends complementary products (tops, bottoms, outerwear, accessories)
- **Smart Swaps:** Suggests alternatives with better value scores based on price savings, ratings, and promotions

### Fashion AI Chat
- Conversational interface with a 3D avatar
- Context-aware responses powered by Anthropic Claude
- Multi-turn conversations with message history
- Can incorporate visual search results as chat context

### Vendor Portal
- Protected route for authorized vendors
- Multi-image product upload and registration
- Products are indexed into the vector store on upload

---
### Sentence-transformers
The application uses sentence-transformers from HuggingFace to power semantic understanding across several features:

#### 1. Text-to-Product Semantic Search
Users describe a fashion item in natural language (e.g., "floral summer dress with puff sleeves"). The description is encoded into a dense vector using a sentence transformer model (e.g., `all-MiniLM-L6-v2`). This embedding is compared against pre-indexed product description embeddings in the vector store to surface the most semantically relevant matches — going beyond keyword overlap.

#### 2. Cross-Modal Query Expansion
When a user uploads an image and Custom YOLO detects objects (e.g., "blue denim jacket"), the detected labels are passed through a sentence transformer to enrich the query with semantically related terms. This improves recall by bridging the gap between visual detection output and product catalog vocabulary.

#### 3. Outfit Completion (Semantic Pairing)
For "Complete the Look" suggestions, sentence transformer embeddings of detected garment descriptions are used to find complementary items. Cosine similarity between outfit component embeddings helps rank candidates that pair well stylistically (e.g., pairing "slim-fit chinos" with "Oxford shirt").

### Custom-trained model for Object detection

We have developed Custom-trained model tailored specifically for a fashion retail application.
The model is currently trained on 15 carefully selected product categories aligned with our business requirements.

By limiting the scope to relevant categories, we achieve:
- Higher accuracy
- Faster inference
- Better user experience

##### Scalability & Future Vision
As the application evolves, we can incrementally expand the model by training it on new categories.
This modular approach allows:
 - Controlled growth of the system
- Continuous improvement without retraining from scratch
- Faster onboarding of new product lines.
##### Key Differentiator
Unlike generic market-available models:
Our custom model is domain-specific, trained only on our business data.

This gives us:
- Full control over predictions
- Reduced noise from irrelevant categories
- Better alignment with business goals

##### Impact
- Improved product detection accuracy
- Enhanced customer experience in search/discovery
- Flexible and scalable architecture for future expansion

## FashionEmbeddingSemanticService

`FashionEmbeddingSemanticService` is the core text-embedding pipeline for semantic product discovery. It uses HuggingFace `sentence-transformers/all-MiniLM-L6-v2` via DJL to power two features:

### Feature — Text-to-Product Semantic Search
Users describe a fashion item in natural language (e.g., *"floral summer dress with puff sleeves"*). The query is encoded into a dense vector at request-time and compared against **pre-indexed product embeddings** via cosine similarity. This surfaces semantically relevant matches beyond keyword overlap.

---

## Dataset

The product catalog consists of **146+ fashion images** stored in `src/main/resources/static/datastore/`. Images follow the naming convention:

```
{Description}_{Brand}_{Category}_{Price}.jpg
```

Example: `Black Blazer_ManQ_Business Formals_2400.jpg`

Product metadata (name, brand, category, price) is parsed directly from the filename.

---

### Routes

| Route | Access | Description |
|---|---|---|
| `/` | Public | Landing page |
| `/login` | Public | Demo login |
| `/register` | Public | Demo registration |
| `/search` | Public | Visual & text search |
| `/chat` | Public | Fashion AI chat |
| `/vendor` | Protected | Vendor product upload |

---

## Architecture Notes

- **Vector Store:** Custom JSON-based persistence in the `embeddings/` directory — no external vector database required.
- **Embeddings:** Products are indexed using HuggingFace `all-MiniLM-L6-v2` text embeddings via DJL with cosine similarity for retrieval.
- **Style Rules:** A rule-based engine maps gender + item type + color to recommended complementary product categories.
- **Smart Swap Scoring:** Value scores combine visual similarity, price savings, product ratings, and discount percentages.

---

## Team

**Binary Brains** — CS04 2026 AIH Hackathon
