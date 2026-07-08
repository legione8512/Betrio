const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export function buildUrl(path, params) {
  const url = new URL(path, API_BASE);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && String(value) !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }
  return url.toString();
}

async function parseError(response) {
  const text = await response.text();
  return text || `${response.status} ${response.statusText}`;
}

export async function getJson(path, params) {
  const response = await fetch(buildUrl(path, params));
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json();
}

export async function postJson(path) {
  const response = await fetch(buildUrl(path), { method: "POST" });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json();
}

export { API_BASE };
