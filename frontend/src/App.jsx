import { useState } from 'react';
import Intro from './components/Intro.jsx';
import Mascot from './components/Mascot.jsx';
import { setting } from './api.js';
import Home from './pages/Home.jsx';
import Live from './pages/Live.jsx';
import Processes from './pages/Processes.jsx';
import Optimize from './pages/Optimize.jsx';
import History from './pages/History.jsx';
import Settings from './pages/Settings.jsx';

const PAGES = [
  ['home', 'Overview'],
  ['live', 'Live Diagnostics'],
  ['procs', 'Processes'],
  ['optimize', 'Optimize PC'],
  ['history', 'History & Reports'],
  ['settings', 'Settings'],
];

// The page is just a state value, so no router library is needed.
export default function App() {
  const [intro, setIntro] = useState(setting('intro', 'on') === 'on');
  const [page, setPage] = useState('home');
  const [autoRun, setAutoRun] = useState(false);

  if (intro) return <Intro onDone={() => setIntro(false)} />;

  const go = (id, run = false) => {
    setAutoRun(run);
    setPage(id);
  };

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      <aside className="border-b border-line bg-white/80 md:h-screen md:w-60 md:shrink-0 md:border-b-0 md:border-r">
        <div className="flex items-center gap-3 px-5 py-4">
          <Mascot size={44} />
          <p className="text-sm font-extrabold leading-tight">OS Diagnostic<br />Engine</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col">
          {PAGES.map(([id, name]) => (
            <button key={id} onClick={() => go(id)}
              className={`shrink-0 rounded-full px-4 py-2 text-left text-sm font-bold transition ${
                id === page ? 'bg-accent text-white shadow-md shadow-accent/30' : 'text-muted hover:bg-soft hover:text-deep'
              }`}>
              {name}
            </button>
          ))}
        </nav>
      </aside>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 md:h-screen md:overflow-y-auto md:px-8">
        {page === 'home' && <Home go={go} />}
        {page === 'live' && <Live />}
        {page === 'procs' && <Processes />}
        {page === 'optimize' && <Optimize autoRun={autoRun} />}
        {page === 'history' && <History />}
        {page === 'settings' && <Settings />}
      </main>
    </div>
  );
}
