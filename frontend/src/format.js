// A missing value is shown as "Unavailable", never as an invented number.
export const NA = 'Unavailable';
const missing = (v) => v === null || v === undefined || Number.isNaN(v);

export const pct = (v) => (missing(v) ? NA : `${Math.round(v)}%`);
export const degrees = (v) => (missing(v) ? NA : `${Math.round(v)} °C`);

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

export const dateTime = (iso) => (iso ? new Date(iso).toLocaleString() : NA);
export const time = (iso) => (iso ? new Date(iso).toLocaleTimeString() : NA);
