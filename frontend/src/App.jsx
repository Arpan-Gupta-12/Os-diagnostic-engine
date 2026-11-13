import { useState } from 'react';
import Intro from './components/Intro.jsx';
import Logo from './components/Logo.jsx';
import { getTheme, setTheme } from './theme.js';
import { api, setting, usePoll } from './api.js';
import Home from './pages/Home.jsx';
import Live from './pages/Live.jsx';
import Processes from './pages/Processes.jsx';
import Optimize from './pages/Optimize.jsx';
import Startup from './pages/Startup.jsx';
import Security from './pages/Security.jsx';
import History from './pages/History.jsx';
import Settings from './pages/Settings.jsx';

const PAGES = [
  ['home', 'Overview'],
  ['live', 'Live Diagnostics'],
  ['procs', 'Processes'],
  ['optimize', 'Optimize PC'],
  ['startup', 'Startup & Power'],
  ['security', 'Security'],
  ['history', 'History & Reports'],
  ['settings', 'Settings'],
];

export default function App() {
  const [intro, setIntro] = useState(setting('intro', 'on') === 'on');
  const [page, setPage] = useState('home');
  const [autoRun, setAutoRun] = useState(false);
  const [theme, setThemeState] = useState(getTheme());
  const info = usePoll(api.info, 5000);

  function toggleTheme() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    setThemeState(next);
  }

  const go = (id, run = false) => {
    setAutoRun(run);
    setPage(id);
  };

  return (
    <div className="flex min-h-screen flex-col md:flex-row">
      {intro && <Intro onDone={() => setIntro(false)} />}
      <aside className="relative border-b border-line bg-panel/80 md:h-screen md:w-60 md:shrink-0 md:border-b-0 md:border-r">
        <div className="flex items-center gap-3 px-5 py-4">
          <Logo size={44} />
          <p className="text-sm font-extrabold leading-tight">OS Diagnostic<br />Engine</p>
        </div>
        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col">
          {PAGES.map(([id, name]) => (
            <button key={id} onClick={() => go(id)}
              className={`shrink-0 rounded-full px-4 py-2 text-left text-sm font-bold transition ${
                id === page ? 'bg-accent text-onaccent shadow-md shadow-accent/30' : 'text-muted hover:bg-soft hover:text-deep'
              }`}>
              {name}
            </button>
          ))}
        </nav>
        <div className="px-3 pb-4 md:absolute md:bottom-0 md:w-60">
          <button onClick={toggleTheme} className="btn-soft w-full">{theme === 'dark' ? '☀ Light mode' : '☾ Dark mode'}</button>
        </div>
      </aside>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 md:h-screen md:overflow-y-auto md:px-8">
        {info.data?.demo !== 'off' && info.data && (
          <p className="mb-5 rounded-2xl bg-warn/20 px-4 py-3 text-sm font-semibold text-ink">
            Demo mode is on ({info.data.demo === 'slow' ? 'a slow PC' : 'a healthy PC'}). These are made-up numbers and nothing is removed.
            Turn it off in Settings.
          </p>
        )}
        {page === 'home' && <Home go={go} />}
        {page === 'live' && <Live />}
        {page === 'procs' && <Processes />}
        {page === 'optimize' && <Optimize autoRun={autoRun} />}
        {page === 'startup' && <Startup />}
        {page === 'security' && <Security />}
        {page === 'history' && <History />}
        {page === 'settings' && <Settings />}
      </main>
    </div>
  );
}
