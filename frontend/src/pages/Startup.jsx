import { api, usePoll } from '../api.js';
import { Act, Err, Title } from '../components/ui.jsx';
import { NA } from '../format.js';

export default function Startup() {
  const { data, error, refresh } = usePoll(api.startup, 60000);

  return (
    <div>
      <Title title="Startup & Power" sub="Programs that start with your PC, and the power plan. The startup list is only shown, never changed." />
      <Err error={error} />
      {data && (
        <div className="space-y-5">
          <div className="card">
            <p className="card-title">Power plan</p>
            <p className="mt-2 text-3xl font-extrabold text-deep">{data.power || NA}</p>
            <div className="mt-4 flex flex-wrap gap-3">
              <Act label="Switch to Balanced" confirm="Switch the power plan to Balanced? You can change it back in the system power settings."
                run={() => api.setPower('balanced')} onDone={refresh} />
              <Act label="Switch to High performance" confirm="Switch the power plan to High performance? The PC will be faster but use more power and get hotter."
                run={() => api.setPower('performance')} onDone={refresh} />
            </div>
          </div>

          {data.tips.map((tip) => (
            <p key={tip} className="rounded-2xl bg-soft px-4 py-3 text-sm leading-relaxed">{tip}</p>
          ))}

          <div className="card overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-muted">
                  {['Program', 'Started from', 'Command'].map((h) => <th key={h} className="px-5 py-3 font-bold">{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {data.items.map((item) => (
                  <tr key={item.where + item.name} className="border-t border-line hover:bg-soft">
                    <td className="px-5 py-2 font-semibold">{item.name}</td>
                    <td className="px-5 py-2 text-muted">{item.where}</td>
                    <td className="max-w-xs truncate px-5 py-2 text-xs text-muted" title={item.command}>{item.command}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {data.items.length === 0 && (
              <p className="p-5 text-sm text-muted">No startup programs were found, or this PC does not let us read them.</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
