# Candidate Profile Transformer — Frontend

Part of the [Multi-Source Candidate Data Transformer](../README.md). React + Vite + TypeScript SPA for the Candidate Profile Transformation System.

## Stack

- React 19 + Vite + TypeScript
- Tailwind CSS v4
- TanStack Query (API state)
- React Router (routing)

## Pages

| Route | Page |
|-------|------|
| `/upload` | Upload sources, runtime config, process candidate |
| `/candidates` | Searchable list with filters and pagination |
| `/candidates/:id` | Full profile with tabbed detail view |

## Development

```bash
npm install
npm run dev
```

Runs at [http://localhost:5173](http://localhost:5173). API requests to `/api/*` are proxied to `http://localhost:8080`.

## Environment

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | API base URL (empty = same origin / proxy) |

## Build

```bash
npm run build
npm run preview
```
