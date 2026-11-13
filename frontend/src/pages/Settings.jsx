import { useEffect, useState } from 'react';
import { api, save, setting, usePoll } from '../api.js';
import { bytes } from '../format.js';
import { Err, Title } from '../components/ui.jsx';

const LIMITS = [
  ['cpu', 'CPU busy above (%)', 20, 100],
  ['mem', 'Memory used above (%)', 20, 100],
  ['paging', 'Paging above (pages per second)', 1, 100000],
  ['program', 'One program uses more than (% of memory)', 5, 90],
  ['disk', 'Drive free space below (%)', 1, 50],
  ['diskBusy', 'Disk busy above (%)', 20, 100],
  ['temp', 'Processor hotter than (°C)', 40, 110],
];

const DEMOS = [
  ['off', 'Off (real measurements)'],
  ['healthy', 'Demo: a healthy PC'],
  ['slow', 'Demo: a slow PC'],
];

export default function Settings() {
  const [intro, setIntro] = useState(setting('intro', 'on') === 'on');
  const [hours, setHours] = useState(setting('hours', 24));
  const [limits, setLimits] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState(null);
  const info = usePoll(api.info, 60000);

  useEffect(() => {
    api.limits().then(setLimits).catch(setError);
  }, []);

  function changeIntro(on) {
    setIntro(on);
    save('intro', on ? 'on' : 'off');
  }

  function changeHours(value) {
    const h = Math.min(720, Math.max(1, Number(value) || 24));
    setHours(h);
    save('hours', h);
  }

  async function run(action, done) {
    setError(null);
    setMessage('');
    try {
      await action();
      setMessage(done);
    } catch (e) {
      setError(e);
    }
  }

  const saveLimits = () => run(async () => setLimits(await api.saveLimits(limits)), 'Limits saved. The next checkup uses them.');
  const resetLimits = () => run(async () => setLimits(await api.resetLimits()), 'Limits are back to the defaults.');
  const changeDemo = (mode) => run(async () => { await api.demo(mode); info.refresh(); }, 'Demo mode changed.');

  const d = info.data;

  return (
    <div>
      <Title title="Settings" sub="The first two are saved in this browser. The rest is saved in the database." />
      <Err error={error} />
      {message && <p className="mb-4 rounded-2xl bg-ok/10 px-4 py-3 text-sm text-ok">{message}</p>}

      <div className="card mb-5 space-y-5">
        <label className="flex items-center gap-3">
          <input type="checkbox" className="h-5 w-5 accent-accent" checked={intro} onChange={(e) => changeIntro(e.target.checked)} />
          <span className="text-sm font-semibold">Play the welcome animation when the app opens</span>
        </label>
        <div>
          <label className="text-sm font-semibold">Only clean files older than (hours)</label>
          <input type="number" min="1" max="720" className="input mt-2 block w-32" value={hours} onChange={(e) => changeHours(e.target.value)} />
          <p className="mt-1 text-xs text-muted">Newer files may still be in use, so they are never removed. Allowed: 1 - 720.</p>
        </div>
      </div>

      <div className="card mb-5">
        <h2 className="card-title mb-1">When is something a problem?</h2>
        <p className="mb-4 text-xs text-muted">The checkup rules use these limits. Change them if the defaults do not fit your PC.</p>
        {limits && (
          <div className="grid gap-4 sm:grid-cols-2">
            {LIMITS.map(([key, label, min, max]) => (
              <label key={key} className="text-sm">
                <span className="font-semibold">{label}</span>
                <input type="number" min={min} max={max} className="input mt-1 block w-full" value={limits[key]}
                  onChange={(e) => setLimits({ ...limits, [key]: Number(e.target.value) })} />
              </label>
            ))}
          </div>
        )}
        <div className="mt-4 flex gap-3">
          <button className="btn-main" onClick={saveLimits}>Save limits</button>
          <button className="btn-soft" onClick={resetLimits}>Back to defaults</button>
        </div>
      </div>

      <div className="card mb-5">
        <h2 className="card-title mb-1">Demo mode</h2>
        <p className="mb-3 text-xs text-muted">Shows made-up numbers so you can try the app on a PC without problems. Nothing is deleted in demo mode.</p>
        <div className="flex flex-wrap gap-4">
          {DEMOS.map(([mode, label]) => (
            <label key={mode} className="flex items-center gap-2 text-sm font-semibold">
              <input type="radio" name="demo" className="accent-accent" checked={d?.demo === mode} onChange={() => changeDemo(mode)} />
              {label}
            </label>
          ))}
        </div>
      </div>

      <div className="card">
        <h2 className="card-title mb-3">About this PC</h2>
        {d && (
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            <div><dt className="text-xs text-muted">System</dt><dd>{d.os}</dd></div>
            <div><dt className="text-xs text-muted">Name</dt><dd>{d.host}</dd></div>
            <div><dt className="text-xs text-muted">Processor</dt><dd>{d.cpu} ({d.cores} cores)</dd></div>
            <div><dt className="text-xs text-muted">Memory</dt><dd>{bytes(d.ram)}</dd></div>
            {d.disks.map((disk) => (
              <div key={disk.name}><dt className="text-xs text-muted">Drive {disk.name}</dt><dd>{bytes(disk.free)} free of {bytes(disk.total)}</dd></div>
            ))}
          </dl>
        )}
      </div>
    </div>
  );
}
