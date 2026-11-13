import { useState } from 'react';

// Small pieces used by several pages.

export const Err = ({ error }) =>
  error ? <p className="rounded-2xl bg-bad/10 px-4 py-3 text-sm text-bad">{error.message}</p> : null;

export function Title({ title, sub }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-extrabold">{title}</h1>
      {sub && <p className="mt-1 text-sm text-muted">{sub}</p>}
    </div>
  );
}

const COLORS = {
  Healthy: 'bg-ok/15 text-ok',
  'Needs attention': 'bg-warn/20 text-warn',
  Critical: 'bg-bad/15 text-bad',
  Unknown: 'bg-soft text-muted',
};

// The result of a checkup, e.g. "Healthy".
export const Status = ({ status }) => (
  <span className={`rounded-full px-3 py-1 text-xs font-bold ${COLORS[status] || COLORS.Unknown}`}>{status}</span>
);

// A card with one big number.
export function Stat({ name, value, note }) {
  return (
    <div className="card">
      <p className="card-title">{name}</p>
      <p className="mt-2 text-3xl font-extrabold text-deep">{value}</p>
      {note && <p className="mt-1 text-xs text-muted">{note}</p>}
    </div>
  );
}

// The list of problems found by a checkup.
export function Problems({ list }) {
  if (list.length === 0) return <p className="text-sm text-muted">No problems found. 🎉</p>;
  return (
    <ul className="space-y-2">
      {list.map((p) => (
        <li key={p} className="rounded-2xl bg-soft px-4 py-3 text-sm leading-relaxed">
          {p}
        </li>
      ))}
    </ul>
  );
}

// A button for a change that needs the user's OK. It asks first, then shows what really happened.
export function Act({ label, confirm, run, onDone }) {
  const [state, setState] = useState({});

  async function click() {
    if (!window.confirm(confirm)) return;
    setState({ busy: true });
    try {
      const result = await run();
      setState({ message: result.message });
      onDone?.();
    } catch (e) {
      setState({ error: e.message });
    }
  }

  return (
    <span className="inline-flex flex-wrap items-center gap-3">
      <button className="btn-soft" onClick={click} disabled={state.busy}>{state.busy ? 'Working...' : label}</button>
      {state.message && <span className="text-sm text-ok">{state.message}</span>}
      {state.error && <span className="text-sm text-bad">{state.error}</span>}
    </span>
  );
}
