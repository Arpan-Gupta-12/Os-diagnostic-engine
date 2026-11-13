import { useEffect } from 'react';
import Mascot from './Mascot.jsx';

// A PC screen wakes up, smiles, and zooms into the home screen. Skippable, and it can be switched off in Settings.
export default function Intro({ onDone }) {
  useEffect(() => {
    const calm = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    const timer = setTimeout(onDone, calm ? 500 : 3400);
    return () => clearTimeout(timer);
  }, [onDone]);

  return (
    <div className="intro fixed inset-0 z-50 flex flex-col items-center justify-center overflow-hidden">
      <Mascot size={220} mode="intro" />
      <p className="intro-text mt-4 text-xl font-bold text-muted">Hello. Let's look after your PC.</p>
      <button onClick={onDone} className="btn-soft absolute bottom-8 right-8">Skip</button>
    </div>
  );
}
