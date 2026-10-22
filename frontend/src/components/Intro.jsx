import { useEffect } from 'react';
import Mascot from './Mascot.jsx';

// About 3 seconds of cozy welcome. Can be skipped, or switched off in Settings.
export default function Intro({ onDone }) {
  useEffect(() => {
    const calm = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const timer = setTimeout(onDone, calm ? 600 : 3000);
    return () => clearTimeout(timer);
  }, [onDone]);

  return (
    <div className="intro fixed inset-0 z-50 flex flex-col items-center justify-center overflow-hidden">
      <div className="cloud" style={{ width: 150, height: 44, top: '14%', left: 0 }} />
      <div className="cloud" style={{ width: 110, height: 34, top: '30%', left: 0, animationDelay: '-4s', animationDuration: '12s' }} />
      <div className="cloud" style={{ width: 180, height: 50, top: '72%', left: 0, animationDelay: '-7s', animationDuration: '14s' }} />
      <span className="sparkle text-2xl" style={{ top: '26%', left: '36%' }}>✦</span>
      <span className="sparkle text-xl" style={{ top: '34%', left: '62%', animationDelay: '0.4s' }}>✦</span>
      <span className="sparkle text-lg" style={{ top: '58%', left: '30%', animationDelay: '0.8s' }}>✦</span>
      <span className="sparkle text-2xl" style={{ top: '60%', left: '68%', animationDelay: '0.2s' }}>✦</span>

      <Mascot size={200} mode="hello" />
      <p className="intro-text mt-6 text-2xl font-extrabold text-ink">Hi there! I'm Pixel 💙</p>
      <p className="intro-sub mt-2 text-sm text-muted">Let's keep your PC happy and healthy.</p>
      <button onClick={onDone} className="btn-soft absolute bottom-8 right-8">
        Skip
      </button>
    </div>
  );
}
