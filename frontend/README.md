# Visual Retail Discovery (Frontend)

A demo frontend for the **Visual Retail Discovery** hackathon project.

This React + Vite application implements a demo authentication flow and a visual search experience. Users can log in as a vendor, upload product images (mock upload), and search for similar products using an image.

## 🚀 Features

- Drag & drop + file picker upload
- Image preview + remove action
- Calls backend API: `POST /api/image/search`
- Uploads vendor images to: `POST /api/image/register`
- Shows results in a responsive grid
- Clean Tailwind UI with modern styling

## 🧩 Backend API Contract

### 1) Image Search API

**POST** `/api/image/search`

- Content-Type: `multipart/form-data`
- Body field: `file` (file)

Response JSON:

```json
[
  {
    "productId": "p123",
    "name": "Nike Air Max",
    "brand": "Nike",
    "price": 120,
    "score": 0.92,
    "imagePath": "https://...",
    "detectedLabels": ["sneaker", "running"]
  }
]
```

### 2) Vendor Upload API

**POST** `/api/image/register`

- Content-Type: `multipart/form-data`
- Body field: `images` (multiple files)

### 3) Static Image Fetch (Optional)

The backend also exposes images stored in the demo datastore under:

**GET** `/api/datastore/images/{filename}`

Response:
- `Content-Type: image/*`
- Raw image bytes

Example usage in UI (if used):

```tsx
<img src={`/api/datastore/images/${filename}`} alt={name} />
```

## 🛠️ Getting Started

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

> If the backend is unreachable, the frontend falls back to a built-in mock response for demo purposes.

## 🔐 Demo Authentication


**Login credentials (hardcoded):**

- **Normal User**
  - **Email:** `demo@innovasolutions.com`
  - **Password:** `password123`
- **Superuser**
  - **Email:** `super@innovasolutions.com`
  - **Password:** `superpassword`

### Routes

- `/login` — Demo login page
- `/register` — Demo registration page (no real account creation)
- `/vendor` — Vendor upload page (requires login)
- `/search` — Public search page

## 🧩 How it works

- Logging in stores a flag in `localStorage` so refreshes keep you logged in.
- `/vendor` is protected and redirects to `/login` if not authenticated.
- `/search` is always accessible.

## 📦 Project Structure

- `src/components/` — UI components (uploader, cards, grid)
- `src/pages/` — page layout (`Home`)
- `src/hooks/` — state + logic (`useImageSearch`)
- `src/services/` — API client + mocks
- `src/types/` — TypeScript types

## ✅ Example Mock Response

When backend calls fail, the app uses this mock response:

```json
{
  "results": [
    { "productId": "p123", "name": "Nike Air Max 270", "brand": "Nike", "price": 129.99, "similarity": 0.92 },
    { "productId": "p456", "name": "Adidas Ultraboost 5", "brand": "Adidas", "price": 179.99, "similarity": 0.88 },
    { "productId": "p789", "name": "New Balance 990v6", "brand": "New Balance", "price": 159.0, "similarity": 0.84 }
  ]
}
```
