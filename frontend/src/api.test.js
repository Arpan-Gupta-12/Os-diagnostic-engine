import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, request } from './api.js';

const reply = (status, body) => ({ ok: status < 300, status, text: async () => JSON.stringify(body) });

afterEach(() => vi.unstubAllGlobals());

describe('api', () => {
  it('returns the JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(200, { cores: 4 })));
    expect((await api.info()).cores).toBe(4);
  });

  it('starts a cleanup with POST and the chosen age', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(200, {})));
    await api.cleanup('thumbnails', 48);
    expect(fetch).toHaveBeenCalledWith('/api/cleanup?what=thumbnails&hours=48', { method: 'POST' });
  });

  it('sends the checkup id with a cleanup only when there is one', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(200, {})));
    await api.cleanup('temp', 24, 7);
    expect(fetch).toHaveBeenCalledWith('/api/cleanup?what=temp&hours=24&scanId=7', { method: 'POST' });
    await api.plan(24);
    expect(fetch).toHaveBeenLastCalledWith('/api/plan?hours=24', {});
  });

  it('turns a server error into a readable message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply(500, { message: 'Oops' })));
    await expect(request('/api/scan')).rejects.toThrow('Oops');
  });

  it('explains when the answer is not JSON, like a static host sends', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 404, text: async () => 'The page could not be found' }));
    await expect(request('/api/live')).rejects.toThrow('did not answer with data');
  });

  it('explains when the backend is down', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(request('/api/live')).rejects.toThrow('Cannot reach the backend');
  });
});
