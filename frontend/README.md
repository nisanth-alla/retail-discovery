# Visual Retail Discovery Frontend

React and Vite frontend for the Visual Retail Discovery application. It provides the catalogue, visual search, semantic search, styling assistant, and demo vendor flow.

## Development

```bash
npm ci
npm run dev
```

The development server runs at `http://localhost:5173`. Vite proxies `/api`, `/datastore`, and `/replicate` requests to the local backend.

## Production Build

```bash
npm run build
```

The output is written to `../src/main/resources/static/home/` so Spring Boot can serve the built application.

## Demo Authentication

The authentication flow is intentionally client-side demo functionality:

| Role | Email | Password |
| --- | --- | --- |
| User | `demo@retail-discovery.local` | `password123` |
| Vendor | `vendor@retail-discovery.local` | `vendorpassword` |

It does not provide production account security or persistent user registration.

## Backend Contract

- `POST /api/image/search` accepts an image in the `file` multipart field.
- `POST /api/image/register` accepts product images in the `images` multipart field.
- `GET /api/image/fetch` returns the demo catalogue.
- `POST /api/image/searchtext` accepts a natural-language search request.
