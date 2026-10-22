// Showing numbers. A missing value becomes "Unavailable", never an invented number.
export const NA = 'Unavailable';
const missing = (v) => v === null || v === undefined || Number.isNaN(v);

export const pct = (v) => (missing(v) ? NA : `${Math.round(v)}%`);

export function bytes(v) {
  if (missing(v)) return NA;
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i++;
  }
  return `${v.toFixed(i >= 3 ? 1 : 0)} ${units[i]}`;
}

// The server sends UTC times; these show them in the user's own time.
export const dateTime = (iso) => (iso ? new Date(iso).toLocaleString() : NA);
export const time = (iso) => (iso ? new Date(iso).toLocaleTimeString() : NA);
