import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, usePoll } from '../api.js';
import { Err, Title } from '../components/ui.jsx';
import { time } from '../format.js';

export default function Live() {
  const live = usePoll(api.live, 3000);
  const data = live.data ?? [];

  return (
    <div>
      <Title title="Live Diagnostics" sub="Updated every 3 seconds." />
      <Err error={live.error} />
      <div className="grid gap-5 md:grid-cols-2">
        <Chart title="CPU usage" data={data} field="cpu" color="#ff7a1a" max={100} unit="%" />
        <Chart title="Memory usage" data={data} field="mem" color="#e6a100" max={100} unit="%" />
        <Chart title="Paging (memory moved to disk)" data={data} field="paging" color="#d9822b" unit=" pages/s" />
        <Chart title="Disk busy" data={data} field="disk" color="#c2410c" max={100} unit="%" />
        <Chart title="Waiting for the disk (I/O wait)" data={data} field="ioWait" color="#f59e6b" max={100} unit="%" />
        <Chart title="Processor temperature" data={data} field="temp" color="#e5533d" unit=" °C" />
      </div>
    </div>
  );
}

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
              <CartesianGrid stroke="rgb(var(--c-line))" vertical={false} />
              <XAxis dataKey="time" tickFormatter={time} stroke="rgb(var(--c-muted))" fontSize={11} minTickGap={40} />
              <YAxis stroke="rgb(var(--c-muted))" fontSize={11} width={40} domain={max ? [0, max] : undefined} />
              <Tooltip contentStyle={{ background: 'rgb(var(--c-panel))', border: '1px solid rgb(var(--c-line))', borderRadius: 12, color: 'rgb(var(--c-ink))' }} labelFormatter={time} formatter={(v) => [`${Math.round(v)}${unit}`, title]} />
              <Line dataKey={field} stroke={color} strokeWidth={3} dot={false} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
