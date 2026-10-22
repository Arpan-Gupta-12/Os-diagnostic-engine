import { useState } from 'react';
import { api, save, setting, usePoll } from '../api.js';
import { bytes } from '../format.js';
import { Title } from '../components/ui.jsx';

// Two settings, kept in this browser. Below them: what the app found out about this PC.
export default function Settings() {
  const [intro, setIntro] = useState(setting('intro', 'on') === 'on');
  const [hours, setHours] = useState(setting('hours', 24));
  const info = usePoll(api.info, 60000).data;

  function changeIntro(on) {
    setIntro(on);
    save('intro', on ? 'on' : 'off');
  }

  function changeHours(value) {
    const h = Math.min(720, Math.max(1, Number(value) || 24));
    setHours(h);
    save('hours', h);
  }

  return (
    <div>
      <Title title="Settings" sub="Saved in this browser right away." />
      <div className="card mb-5 space-y-5">
        <label className="flex items-center gap-3">
          <input type="checkbox" className="h-5 w-5 accent-[#5bb0f2]" checked={intro} onChange={(e) => changeIntro(e.target.checked)} />
          <span className="text-sm font-semibold">Play the welcome animation when the app opens</span>
        </label>
        <div>
          <label className="text-sm font-semibold">Only clean temp files older than (hours)</label>
          <input type="number" min="1" max="720" className="input mt-2 block w-32" value={hours} onChange={(e) => changeHours(e.target.value)} />
          <p className="mt-1 text-xs text-muted">Newer files may still be in use, so they are never removed. Allowed: 1 - 720.</p>
        </div>
      </div>

      <div className="card">
        <h2 className="card-title mb-3">About this PC</h2>
        {info && (
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div><dt className="text-xs text-muted">System</dt><dd>{info.os}</dd></div>
            <div><dt className="text-xs text-muted">Name</dt><dd>{info.host}</dd></div>
            <div><dt className="text-xs text-muted">Processor</dt><dd>{info.cpu} ({info.cores} cores)</dd></div>
            <div><dt className="text-xs text-muted">Memory</dt><dd>{bytes(info.ram)}</dd></div>
            {info.disks.map((d) => (
              <div key={d.name}><dt className="text-xs text-muted">Drive {d.name}</dt><dd>{bytes(d.free)} free of {bytes(d.total)}</dd></div>
            ))}
          </dl>
        )}
      </div>
    </div>
  );
}
