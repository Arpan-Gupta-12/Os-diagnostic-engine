import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, usePoll } from '../api.js';
import { Err, Title } from '../components/ui.jsx';
import { time } from '../format.js';

// Live charts. They update every 3 seconds.
export default function Live() {
  const live = usePoll(api.live, 3000);
  const data = live.data ?? [];

  return (
    <div>
      <Title title="Live Diagnostics" sub="Updated every 3 seconds." />
      <Err error={live.error} />
      <div className="grid gap-5 md:grid-cols-2">
        <Chart title="CPU usage" data={data} field="cpu" color="#5bb0f2" max={100} unit="%" />
        <Chart title="Memory usage" data={data} field="mem" color="#a78bfa" max={100} unit="%" />
        <Chart title="Paging (memory moved to disk)" data={data} field="paging" color="#f0a94a" unit=" pages/s" />
      </div>
    </div>
  );
}

// One line chart. If no sample has a value, it says Unavailable instead of drawing zeros.
function Chart({ title, data, field, color, max, unit }) {
  const hasData = data.some((p) => p[field] !== null);
  return (
    <div className="card">
      <h3 className="card-title mb-3">{title}</h3>
      {!hasData ? (
        <p className="flex h-48 items-center justify-center text-sm text-muted">Unavailable on this PC</p>
      ) : (
        <div className="h-48">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
              <CartesianGrid stroke="#e3effb" vertical={false} />
              <XAxis dataKey="time" tickFormatter={time} stroke="#7f95aa" fontSize={11} minTickGap={40} />
              <YAxis stroke="#7f95aa" fontSize={11} width={40} domain={max ? [0, max] : undefined} />
              <Tooltip labelFormatter={time} formatter={(v) => [`${Math.round(v)}${unit}`, title]} />
              <Line dataKey={field} stroke={color} strokeWidth={3} dot={false} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
