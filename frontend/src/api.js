const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });

  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message =
      typeof body === "string"
        ? body
        : body?.message || body?.error || "Request failed";
    throw new Error(message);
  }

  return body;
}

function normalizeCompetitions(payload) {
  const raw =
    (Array.isArray(payload) && payload) ||
    payload?.items ||
    payload?.competitions ||
    payload?.data ||
    [];

  return raw.map((item) => ({
    id: item.id ?? item.competitionId ?? item.competition_id,
    name:
      item.competitionName ??
      item.name ??
      item.competition_name ??
      `Competition ${item.id ?? item.competitionId}`,
    countryName: item.countryName ?? item.country_name ?? null,
    currentSeasonYear:
      item.currentSeasonYear ?? item.currentSeason?.externalSeasonYear ?? null,
  }));
}

export async function getCompetitions() {
  const data = await request("/api/app/meta/competitions");
  return normalizeCompetitions(data);
}

export async function getDashboardHome(competitionId) {
  const params = new URLSearchParams({
    picksLimit: "5",
    formLimit: "5",
    evaluationsLimit: "5",
  });

  if (competitionId) {
    params.set("competitionId", String(competitionId));
  }

  return request(`/api/app/dashboard/home?${params.toString()}`);
}

export async function getFixturesPage(competitionId, page = 0, size = 10) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sortBy: "kickoffAt",
    sortDir: "desc",
  });

  if (competitionId) {
    params.set("competitionId", String(competitionId));
  }

  return request(`/api/app/fixtures/page?${params.toString()}`);
}

export async function captureFixturePrediction(fixtureId) {
  return request(`/api/predictions/fixture/${fixtureId}/capture`, {
    method: "POST",
  });
}

export async function captureCompetitionPreMatch(competitionId, limit = 50) {
  return request(
    `/api/competition/${competitionId}/upcoming/pre-match-capture?limit=${limit}`,
    { method: "POST" }
  );
}