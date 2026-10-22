// Every call to the Spring Boot backend, plus one small polling hook.
import { useCallback, useEffect, useRef, useState } from 'react';

// Calls the backend and returns the JSON. On failure it throws an Error with a readable message.
export async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(path, options);
  } catch {
    throw new Error('Cannot reach the backend. Is the Spring Boot app running?');
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(data?.message || `Request failed (${res.status})`);
  return data;
}

const post = (path) => request(path, { method: 'POST' });

export const api = {
  info: () => request('/api/info'),
  live: () => request('/api/live'),
  procs: (search, sort) => request(`/api/procs?search=${encodeURIComponent(search)}&sort=${sort}`),
  scan: () => post('/api/scan'),
  scans: () => request('/api/scans'),
  preview: (hours) => request(`/api/cleanup/preview?hours=${hours}`),
  cleanup: (hours) => post(`/api/cleanup?hours=${hours}`),
  cleanups: () => request('/api/cleanups'),
};

// Calls load() now and then every `ms` milliseconds. Returns { data, error }.
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

// The two settings live in the browser, so there is nothing to save on the server.
export const setting = (name, fallback) => localStorage.getItem(name) ?? fallback;
export const save = (name, value) => localStorage.setItem(name, value);
