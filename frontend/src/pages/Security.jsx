import { api, usePoll } from '../api.js';
import { Err, Title } from '../components/ui.jsx';

const COLORS = { Warning: 'bg-bad/15 text-bad', Check: 'bg-warn/20 text-ink' };

export default function Security() {
  const { data, error, refresh } = usePoll(api.security, 60000);

  return (
    <div>
      <Title title="Security" sub="Looks for warning signs of a hacked PC. These are reasons to look closer, not proof, and this is not a virus scan." />
      <Err error={error} />
      <button className="btn-soft mb-5" onClick={refresh}>Check again</button>
      {data && (
        <div className="space-y-5">
          <div className="card">
            <p className={`text-lg font-bold ${data.indicators.length ? 'text-warn' : 'text-ok'}`}>{data.summary}</p>
          </div>

          {data.indicators.map((i) => (
            <div key={i.title} className="card text-sm">
              <p className="font-bold">
                <span className={`mr-2 rounded-full px-3 py-1 text-xs font-bold ${COLORS[i.level]}`}>{i.level}</span>
                {i.title}
              </p>
              <p className="mt-2 leading-relaxed text-muted">{i.text}</p>
            </div>
          ))}

          <div className="card text-sm">
            <p className="card-title mb-2">What was checked</p>
            <ul className="list-disc space-y-1 pl-5 text-muted">{data.checked.map((c) => <li key={c}>{c}</li>)}</ul>
            <p className="mt-3 text-xs text-muted">Nothing is changed or closed. If something looks wrong, run a full scan with your antivirus.</p>
          </div>
        </div>
      )}
    </div>
  );
}
