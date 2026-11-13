import { useEffect, useState } from 'react';
import { api, csvUrl, usePoll } from '../api.js';
import { Report } from './Optimize.jsx';
import { Err, Problems, Status, Title } from '../components/ui.jsx';
import { dateTime } from '../format.js';

export default function History() {
  const [status, setStatus] = useState('');
  const [days, setDays] = useState(0);
  const [open, setOpen] = useState(null);
  const scans = usePoll(() => api.scans(status, days), 15000);
  const cleanups = usePoll(() => api.cleanups(days), 15000);

  const { refresh: reloadScans } = scans;
  const { refresh: reloadCleanups } = cleanups;
  useEffect(() => {
    reloadScans();
    reloadCleanups();
  }, [status, days, reloadScans, reloadCleanups]);

  return (
    <div>
      <Title title="History & Reports" sub="Everything is saved in the database. Click an entry to open it." />
      <div className="mb-5 flex flex-wrap gap-3">
        <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All results</option>
          {['Healthy', 'Needs attention', 'Critical', 'Unknown'].map((s) => <option key={s}>{s}</option>)}
        </select>
        <select className="input" value={days} onChange={(e) => setDays(Number(e.target.value))}>
          <option value={0}>Any time</option>
          <option value={1}>Last 24 hours</option>
          <option value={7}>Last 7 days</option>
          <option value={30}>Last 30 days</option>
        </select>
        <a className="btn-soft" href={csvUrl(status, days)} download>Download checkups (CSV)</a>
      </div>
      <Err error={scans.error || cleanups.error} />

      <h2 className="card-title mb-3">Checkups</h2>
      <div className="mb-8 space-y-3">
        {scans.data?.length === 0 && <p className="text-sm text-muted">No checkups match.</p>}
        {scans.data?.map((s) => (
          <div key={s.id} className="card p-4">
            <button className="flex w-full flex-wrap items-center gap-3 text-left" onClick={() => setOpen(open === 's' + s.id ? null : 's' + s.id)}>
              <Status status={s.status} />
              <span className="flex-1 truncate text-sm font-semibold">{s.summary}</span>
              <span className="text-xs text-muted">Score {s.score ?? 'Unavailable'} · {dateTime(s.time)}</span>
            </button>
            {open === 's' + s.id && <div className="mt-4"><Problems list={s.problems} /></div>}
          </div>
        ))}
      </div>

      <h2 className="card-title mb-3">Cleanups</h2>
      <div className="space-y-3">
        {cleanups.data?.length === 0 && <p className="text-sm text-muted">No cleanup matches.</p>}
        {cleanups.data?.map((c) => (
          <div key={c.id} className="card p-4">
            <button className="flex w-full flex-wrap items-center gap-3 text-left" onClick={() => setOpen(open === 'c' + c.id ? null : 'c' + c.id)}>
              <span className="flex-1 truncate text-sm font-semibold">{c.summary}</span>
              <span className="text-xs text-muted">{dateTime(c.time)}</span>
            </button>
            {open === 'c' + c.id && <div className="mt-4"><Report r={c} /></div>}
          </div>
        ))}
      </div>
    </div>
  );
}
