import { useEffect, useRef, useState } from 'react';
import { api, setting } from '../api.js';
import Mascot from '../components/Mascot.jsx';
import { Act, Err, Problems, Status, Title } from '../components/ui.jsx';
import { bytes } from '../format.js';

const STEPS = ['Check your PC', 'Plan the cleanup', 'Clean old files', 'Verify the result'];

export default function Optimize({ autoRun }) {
  const [step, setStep] = useState(-1);
  const [scan, setScan] = useState(null);
  const [plan, setPlan] = useState([]);
  const [picked, setPicked] = useState({});
  const [results, setResults] = useState([]);
  const [error, setError] = useState(null);
  const auto = useRef(false);
  const hours = Number(setting('hours', 24));

  async function start() {
    setScan(null);
    setPlan([]);
    setResults([]);
    setError(null);
    try {
      setStep(0);
      const checked = await api.scan();
      setScan(checked);
      setStep(1);
      const actions = await api.plan(hours, checked.id);
      setPlan(actions);
      setPicked(Object.fromEntries(actions.filter((a) => a.kind === 'clean').map((a) => [a.key, a.files > 0])));
      setStep(actions.some((a) => a.kind === 'clean' && a.files > 0) ? 2 : 4);
    } catch (e) {
      setError(e);
      setStep(-1);
    }
  }

  // Nothing is deleted before the user clicks "Clean now".
  async function clean() {
    try {
      setStep(3);
      const done = [];
      for (const [what, yes] of Object.entries(picked)) {
        if (yes) done.push(await api.cleanup(what, hours, scan?.id));
      }
      setResults(done);
      setStep(4);
    } catch (e) {
      setError(e);
      setStep(-1);
    }
  }

  useEffect(() => {
    if (autoRun && !auto.current) {
      auto.current = true;
      start();
    }
  }, [autoRun]);

  const working = step === 0 || step === 1 || step === 3;

  return (
    <div>
      <Title title="Optimize My PC" sub="Check, plan, clean and verify. Only old temp files, caches and crash reports are ever removed." />
      <Err error={error} />
      <div className="grid gap-5 md:grid-cols-3">
        <div className="card h-fit text-center">
          <Mascot size={110} mode={working ? 'scanning' : 'idle'} />
          <ol className="mt-4 space-y-2 text-left text-sm">
            {STEPS.map((name, i) => (
              <li key={name} className={i < step ? 'font-bold text-ok' : i === step ? 'font-bold text-deep' : 'text-muted'}>
                {i < step ? '✓' : i === step ? '●' : '○'} {name}
              </li>
            ))}
          </ol>
          <button className="btn-main mt-5 w-full" onClick={start} disabled={working}>
            {step === -1 ? 'Optimize My PC' : 'Run again'}
          </button>
        </div>

        <div className="space-y-5 md:col-span-2">
          {step === -1 && !error && (
            <div className="card text-sm leading-relaxed">
              <p className="font-bold">What happens when you click the button</p>
              <ol className="mt-2 list-decimal space-y-1 pl-5">
                <li>Your PC is checked and any problem is explained.</li>
                <li>We look for old temp files, thumbnails, browser caches and crash reports (older than {hours} hours) and show you what would go, with advice about startup, power and heavy programs.</li>
                <li>You press <b>Clean now</b>. Nothing is deleted before that.</li>
                <li>We measure again, so we can tell you honestly if it worked.</li>
              </ol>
              <p className="mt-3 text-muted">Programs are never closed and your documents are never touched.</p>
            </div>
          )}

          {scan && (
            <div className="card">
              <Status status={scan.status} />
              <p className="mb-3 mt-2 font-bold">{scan.summary}</p>
              <Problems list={scan.problems} />
            </div>
          )}

          {step >= 2 && (
            <div className="card">
              <h3 className="font-bold">{step === 2 ? 'Ready to clean' : 'The plan'}</h3>
              <p className="mt-1 text-sm text-muted">Only your own files older than {hours} hours. Never folders, links or documents.</p>
              <ul className="mt-3 space-y-2">
                {plan.filter((a) => a.kind === 'clean').map((a) => (
                  <li key={a.key}>
                    <label className="flex items-start gap-3 rounded-2xl bg-soft px-4 py-3 text-sm">
                      <input type="checkbox" className="mt-1 h-4 w-4 accent-accent" disabled={a.files === 0 || step !== 2}
                        checked={!!picked[a.key]} onChange={(e) => setPicked({ ...picked, [a.key]: e.target.checked })} />
                      <span>
                        <b>{a.title}</b>: {a.files === 0 ? 'nothing to clean' : `${a.files} files (${bytes(a.bytes)})`}
                        <br /><span className="text-xs text-muted">{a.text}</span>
                        <br /><code className="text-xs text-muted">{a.details[0]}</code>
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
              {step === 2 && (
                <button className="btn-main mt-4" onClick={clean} disabled={!Object.values(picked).some(Boolean)}>Clean now</button>
              )}
            </div>
          )}

          {step >= 2 && plan.filter((a) => a.kind !== 'clean').map((a) => (
            <div key={a.key} className="card text-sm">
              <p className="font-bold">
                {a.title} <span className="ml-2 rounded-full bg-soft px-2 py-0.5 text-xs font-bold text-muted">
                  {a.kind === 'advice' ? 'Advice' : 'Not done by this app'}
                </span>
              </p>
              <p className="mt-1 leading-relaxed">{a.text}</p>
              {a.details.length > 0 && (
                <ul className="mt-2 list-disc space-y-1 pl-5 text-muted">{a.details.map((d) => <li key={d}>{d}</li>)}</ul>
              )}
              {a.closable?.length > 0 && (
                <div className="mt-3 flex flex-col gap-2">
                  {a.closable.map((name) => (
                    <Act key={name} label={`Close ${name}`}
                      confirm={`Close every "${name}" window and process? Unsaved work in it will be lost.`}
                      run={() => api.closeProgram(name)} />
                  ))}
                </div>
              )}
              {a.key === 'power-saver' && (
                <div className="mt-3">
                  <Act label="Switch to Balanced" confirm="Switch the power plan to Balanced? You can change it back in the system power settings."
                    run={() => api.setPower('balanced')} />
                </div>
              )}
            </div>
          ))}

          {step === 4 && results.length === 0 && (
            <div className="card text-sm">No old files were found, so there is nothing to clean. ✨</div>
          )}

          {results.map((r) => <Report key={r.what} r={r} />)}
        </div>
      </div>
    </div>
  );
}

export function Report({ r }) {
  return (
    <div className="card">
      <p className={`text-lg font-bold ${r.verified ? 'text-ok' : 'text-warn'}`}>{r.summary}</p>
      <table className="mt-4 w-full text-sm">
        <thead>
          <tr className="text-left text-xs text-muted">
            <th className="pb-2">Measurement</th><th className="pb-2">Before</th><th className="pb-2">After</th>
          </tr>
        </thead>
        <tbody>
          <tr className="border-t border-line"><td className="py-2 text-muted">Old files left</td><td>{bytes(r.bytesBefore)}</td><td>{bytes(r.bytesAfter)}</td></tr>
          <tr className="border-t border-line"><td className="py-2 text-muted">Free disk space</td><td>{bytes(r.freeBefore)}</td><td>{bytes(r.freeAfter)}</td></tr>
        </tbody>
      </table>
      <p className="mt-3 text-xs text-muted">
        Removing old files frees disk space. It does not usually change CPU or memory use, so we do not promise a faster PC.
        {r.what === 'thumbnails' && ' Picture previews are rebuilt automatically when needed.'}
        {r.failed > 0 && ` ${r.failed} files were skipped because they are in use.`}
      </p>
    </div>
  );
}
