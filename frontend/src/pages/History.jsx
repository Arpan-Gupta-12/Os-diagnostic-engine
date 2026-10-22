import { useState } from 'react';
import { api, usePoll } from '../api.js';
import { Report } from './Optimize.jsx';
import { Err, Problems, Status, Title } from '../components/ui.jsx';
import { dateTime } from '../format.js';

// Everything is saved in the database, so it is still here after a restart.
export default function History() {
  const scans = usePoll(api.scans, 15000);
  const cleanups = usePoll(api.cleanups, 15000);
  const [open, setOpen] = useState(null); // id of the scan or cleanup shown in full

  return (
    <div>
      <Title title="History & Reports" sub="Click an entry to open it." />
      <Err error={scans.error || cleanups.error} />

      <h2 className="card-title mb-3">Checkups</h2>
      <div className="mb-8 space-y-3">
        {scans.data?.length === 0 && <p className="text-sm text-muted">No checkups yet.</p>}
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
        {cleanups.data?.length === 0 && <p className="text-sm text-muted">No cleanup has been run yet.</p>}
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
