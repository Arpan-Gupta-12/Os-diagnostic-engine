import { useEffect, useRef, useState } from 'react';
import { api, setting } from '../api.js';
import Mascot from '../components/Mascot.jsx';
import { Err, Problems, Status, Title } from '../components/ui.jsx';
import { bytes } from '../format.js';

// The steps, in order. Each one is a real call to the backend:
//   check -> POST /api/scan, plan -> GET /api/cleanup/preview,
//   clean -> POST /api/cleanup (which also measures again to verify).
const STEPS = ['Check your PC', 'Plan the cleanup', 'Clean old temp files', 'Verify the result'];

export default function Optimize({ autoRun }) {
  const [step, setStep] = useState(-1); // index of the step that is working now, -1 = not started
  const [scan, setScan] = useState(null);
  const [plan, setPlan] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const auto = useRef(false);
  const hours = Number(setting('hours', 24));

  async function start() {
    setScan(null);
    setPlan(null);
    setResult(null);
    setError(null);
    try {
      setStep(0);
      setScan(await api.scan());
      setStep(1);
      const p = await api.preview(hours);
      setPlan(p);
      setStep(p.files > 0 ? 2 : 4); // waits at step 2 for the user's OK
    } catch (e) {
      setError(e);
      setStep(-1);
    }
  }

  // The user's click on "Clean now" is the approval.
  async function clean() {
    try {
      setStep(3);
      setResult(await api.cleanup(hours));
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
      <Title title="Optimize My PC" sub="Check, plan, clean and verify. Only old temp files are ever removed." />
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
                <li>We look for old temp files (older than {hours} hours) and show you what would go.</li>
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

          {step === 2 && (
            <div className="card">
              <h3 className="font-bold">Ready to clean</h3>
              <p className="mt-1 text-sm">
                {plan.files} old files ({bytes(plan.bytes)}) in <code className="text-deep">{plan.folder}</code> can be removed.
                Only your own files older than {plan.hours} hours, never folders or links.
              </p>
              <button className="btn-main mt-4" onClick={clean}>Clean now</button>
            </div>
          )}

          {step === 4 && !result && (
            <div className="card text-sm">No old temp files were found, so there is nothing to clean. ✨</div>
          )}

          {result && <Report r={result} />}
        </div>
      </div>
    </div>
  );
}

// Before / after, measured the same way both times.
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
          <tr className="border-t border-line"><td className="py-2 text-muted">Old temp files</td><td>{bytes(r.bytesBefore)}</td><td>{bytes(r.bytesAfter)}</td></tr>
          <tr className="border-t border-line"><td className="py-2 text-muted">Free disk space</td><td>{bytes(r.freeBefore)}</td><td>{bytes(r.freeAfter)}</td></tr>
        </tbody>
      </table>
      <p className="mt-3 text-xs text-muted">
        Removing temp files frees disk space. It does not usually change CPU or memory use, so we do not promise a faster PC.
        {r.failed > 0 && ` ${r.failed} files were skipped because they are in use.`}
      </p>
    </div>
  );
}
