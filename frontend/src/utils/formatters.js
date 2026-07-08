const dateTimeFormatter = new Intl.DateTimeFormat("ro-RO", {
  dateStyle: "medium",
  timeStyle: "short",
  timeZone: "Europe/Bucharest",
});

const percentageFormatter = new Intl.NumberFormat("ro-RO", {
  style: "percent",
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const decimalFormatter = new Intl.NumberFormat("ro-RO", {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export function formatDateTime(value, fallback = "—") {
  if (!value) {
    return fallback;
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return fallback;
  }

  return dateTimeFormatter.format(date);
}

export function formatProbability(value, fallback = "—") {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  const number = Number(value);

  if (!Number.isFinite(number)) {
    return fallback;
  }

  return percentageFormatter.format(number);
}

export function formatDecimal(value, fallback = "—") {
  if (value === null || value === undefined || value === "") {
    return fallback;
  }

  const number = Number(value);

  if (!Number.isFinite(number)) {
    return fallback;
  }

  return decimalFormatter.format(number);
}