import { useCallback, useEffect, useRef, useState } from 'react';

export async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(path, options);
  } catch {
    throw new Error('Cannot reach the backend. Is the Spring Boot app running?');
  }
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    throw new Error('The backend did not answer with data. Open the app from the Spring Boot address, for example http://localhost:8080.');
  }
  if (!res.ok) throw new Error(data?.message || `Request failed (${res.status})`);
  return data;
}

const post = (path) => request(path, { method: 'POST' });
const query = (params) => new URLSearchParams(params).toString();

export const api = {
  info: () => request('/api/info'),
  live: () => request('/api/live'),
  procs: (search, sort) => request(`/api/procs?${query({ search, sort })}`),
  scan: () => post('/api/scan'),
  scans: (status = '', days = 0) => request(`/api/scans?${query({ status, days })}`),
  plan: (hours, scanId) => request(`/api/plan?${query(scanId ? { hours, scanId } : { hours })}`),
  cleanup: (what, hours, scanId) => post(`/api/cleanup?${query(scanId ? { what, hours, scanId } : { what, hours })}`),
  cleanups: (days = 0) => request(`/api/cleanups?${query({ days })}`),
  startup: () => request('/api/startup'),
  security: () => request('/api/security'),
  securityScan: () => post('/api/security/scan'),
  closeProgram: (name) => post(`/api/programs/close?${query({ name })}`),
  setPower: (plan) => post(`/api/power?${query({ plan })}`),
  limits: () => request('/api/limits'),
  saveLimits: (limits) =>
    request('/api/limits', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(limits) }),
  resetLimits: () => request('/api/limits', { method: 'DELETE' }),
  demo: (mode) => post(`/api/demo?${query({ mode })}`),
};

export const csvUrl = (status, days) => `/api/scans.csv?${query({ status, days })}`;

// Calls load() now and then every `ms` milliseconds.
export function usePoll(load, ms) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);
  const loadRef = useRef(load);
  loadRef.current = load;

  const refresh = useCallback(() => {
    loadRef.current().then(setData).then(() => setError(null)).catch(setError);
  }, []);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, ms);
    return () => clearInterval(timer);
  }, [refresh, ms]);

  return { data, error, refresh };
}

// Two settings live in the browser: the welcome animation and the minimum age of files to clean.
export const setting = (name, fallback) => localStorage.getItem(name) ?? fallback;
export const save = (name, value) => localStorage.setItem(name, value);
