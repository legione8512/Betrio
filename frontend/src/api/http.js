const API_BASE =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export function buildUrl(path, params) {
  const url = new URL(path, API_BASE);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (
        value !== undefined &&
        value !== null &&
        String(value) !== ""
      ) {
        url.searchParams.set(key, String(value));
      }
    });
  }

  return url.toString();
}

async function createHttpError(response) {
  const text = await response.text();

  let body = null;

  if (text) {
    try {
      body = JSON.parse(text);
    } catch {
      body = null;
    }
  }

  const message =
    body?.message ||
    body?.error ||
    text ||
    `${response.status} ${response.statusText}`;

  const error = new Error(message);
  error.status = response.status;
  error.body = body;

  return error;
}

export async function getJson(path, params) {
  const response = await fetch(buildUrl(path, params));

  if (!response.ok) {
    throw await createHttpError(response);
  }

  return response.json();
}

export async function postJson(path) {
  const response = await fetch(buildUrl(path), {
    method: "POST",
  });

  if (!response.ok) {
    throw await createHttpError(response);
  }

  return response.json();
}

export { API_BASE };