# Betrio Frontend

This frontend was generated after inspecting the uploaded backend archive and matching the existing controllers and DTOs.

## Backend endpoints used

- `GET /api/app/dashboard/home`
- `GET /api/app/dashboard/summary`
- `GET /api/app/fixtures/page`
- `GET /api/app/fixtures/upcoming`
- `GET /api/app/fixtures/recent`
- `GET /api/app/fixture/{fixtureId}/overview`
- `GET /api/app/fixture/{fixtureId}/h2h`
- `GET /api/app/fixture/{fixtureId}/market-comparison`
- `GET /api/app/fixture/{fixtureId}/match-center`
- `GET /api/app/fixture/{fixtureId}/recommendation`
- `GET /api/app/recommendations/upcoming`
- `GET /api/app/standings/current`
- `GET /api/app/standings/form/current`
- `GET /api/app/team/{teamId}/overview`
- `GET /api/app/team/{teamId}/recent`
- `GET /api/app/team/{teamId}/upcoming`
- `GET /api/app/team/{teamId}/form`
- `GET /api/app/meta/statuses`
- `GET /api/app/meta/teams`
- `GET /api/app/meta/rounds`
- `GET /api/app/evaluations/recent`
- `GET /api/reports/dashboard`
- `GET /api/reports/recent`
- `GET /api/reports/fixture/{fixtureId}`
- `GET /api/reports/team/{teamId}`
- `POST /api/actions/base-refresh`
- `POST /api/actions/fixture/{fixtureId}/smart-update`
- `GET /api/actions/history`
- `GET /api/actions/fixture/{fixtureId}/history`

## Run

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

If the backend runs on another host or port, update `VITE_API_BASE_URL` in `.env`.

## Notes

- The frontend uses only endpoints confirmed in the backend archive.
- There is no mocked data.
- If the browser blocks requests, enable CORS in the Spring Boot backend for the frontend origin.
