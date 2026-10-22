import { useEffect, useState } from 'react';
import { api, usePoll } from '../api.js';
import { Err, Title } from '../components/ui.jsx';
import { NA, bytes, pct } from '../format.js';

// Running programs. Read-only on purpose: this app never closes a program for you.
export default function Processes() {
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState('mem');
  const list = usePoll(() => api.procs(search, sort), 5000);

  const { refresh } = list;
  useEffect(refresh, [search, sort, refresh]); // reload straight away when the search or sort changes

  return (
    <div>
      <Title title="Processes" sub="Programs running right now. This app never closes them for you." />
      <div className="mb-4 flex gap-3">
        <input className="input flex-1" placeholder="Search by name" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select className="input" value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="mem">Most memory</option>
          <option value="cpu">Most CPU</option>
        </select>
      </div>
      <Err error={list.error} />
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs text-muted">
              {['Name', 'PID', 'CPU', 'Memory', 'User'].map((h) => (
                <th key={h} className="px-5 py-3 font-bold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {list.data?.map((p) => (
              <tr key={p.pid} className="border-t border-line hover:bg-soft">
                <td className="px-5 py-2 font-semibold">{p.name}</td>
                <td className="px-5 py-2 text-muted">{p.pid}</td>
                <td className="px-5 py-2">{p.cpu == null ? NA : pct(p.cpu)}</td>
                <td className="px-5 py-2">{bytes(p.mem)}</td>
                <td className="px-5 py-2 text-muted">{p.user || NA}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {list.data?.length === 0 && <p className="p-5 text-sm text-muted">No program matches.</p>}
      </div>
    </div>
  );
}
