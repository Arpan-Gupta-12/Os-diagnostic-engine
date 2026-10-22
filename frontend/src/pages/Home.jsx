import { useEffect, useRef, useState } from 'react';
import { api, usePoll } from '../api.js';
import Mascot from '../components/Mascot.jsx';
import { Err, Problems, Stat, Status, Title } from '../components/ui.jsx';
import { bytes, pct, time } from '../format.js';

// The dashboard: the latest checkup, and a few live numbers.
export default function Home({ go }) {
  const live = usePoll(api.live, 3000);
  const [scan, setScan] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const started = useRef(false);

  async function check() {
    setBusy(true);
    setError(null);
    try {
      setScan(await api.scan());
    } catch (e) {
      setError(e);
    }
    setBusy(false);
  }

  // First visit: show the last checkup if it is recent, otherwise check now.
  useEffect(() => {
    if (started.current) return; // React dev mode runs effects twice
    started.current = true;
    api.scans().then((list) => {
      const last = list[0];
      if (last && Date.now() - new Date(last.time).getTime() < 15 * 60 * 1000) setScan(last);
      else check();
    }).catch(setError);
  }, []);

  const now = live.data?.[live.data.length - 1];

  return (
    <div>
      <Title title="Hello! 👋" sub="Here is how your PC is feeling right now." />
      <Err error={error || live.error} />

      <div className="card mb-5 flex flex-wrap items-center gap-6">
        <Mascot size={110} mode={busy ? 'scanning' : 'idle'} />
        <div className="min-w-[14rem] flex-1">
          {busy && <p className="text-muted">Checking your PC, this takes a few seconds...</p>}
          {!busy && scan && (
            <>
              <div className="flex items-center gap-3">
                <Status status={scan.status} />
                <span className="text-xs text-muted">checked at {time(scan.time)}</span>
              </div>
              <p className="mt-2 text-lg font-bold">{scan.summary}</p>
            </>
          )}
        </div>
        <div className="text-center">
          <p className="card-title">Health score</p>
          <p className="text-5xl font-extrabold text-deep">{busy ? '...' : scan?.score ?? '--'}</p>
        </div>
      </div>

      <div className="mb-5 flex gap-3">
        <button className="btn-main" onClick={() => go('optimize', true)} disabled={busy}>Optimize My PC</button>
        <button className="btn-soft" onClick={check} disabled={busy}>Check again</button>
      </div>

      <div className="mb-5 grid grid-cols-2 gap-4 md:grid-cols-4">
        <Stat name="CPU" value={pct(now?.cpu)} />
        <Stat name="Memory" value={pct(now?.mem)} note={now ? `${bytes(now.memFree)} free` : null} />
        <Stat name="Programs running" value={now?.procs ?? '--'} />
        <Stat name="Paging" value={now?.paging == null ? 'Unavailable' : `${Math.round(now.paging)}/s`} note="memory moved to disk" />
      </div>

      {scan && (
        <div className="card">
          <h2 className="card-title mb-3">What we found</h2>
          <Problems list={scan.problems} />
        </div>
      )}
    </div>
  );
}
