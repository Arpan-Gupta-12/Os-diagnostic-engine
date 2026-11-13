// Pixel, a small PC screen with a face. Drawn with SVG, animated with CSS (see index.css).
//   mode="idle"     blinks slowly, breathes
//   mode="scanning" looks left and right while a checkup runs
//   mode="intro"    wakes up, smiles, then the screen grows to fill the window
export default function Mascot({ size = 140, mode = 'idle' }) {
  return (
    <svg className={`mascot m-${mode}`} width={size} height={size} viewBox="0 0 120 120" role="img" aria-label="Pixel the PC">
      <rect x="12" y="14" width="96" height="72" rx="18" className="m-frame" />
      <rect x="20" y="22" width="80" height="56" rx="11" className="m-screen" />
      <g className="m-face">
        <ellipse cx="48" cy="46" rx="5" ry="7" className="m-eye m-ink" />
        <ellipse cx="72" cy="46" rx="5" ry="7" className="m-eye m-ink" />
        <ellipse cx="38" cy="58" rx="6" ry="3.5" className="m-blush" />
        <ellipse cx="82" cy="58" rx="6" ry="3.5" className="m-blush" />
        <path d="M52 58 Q60 66 68 58" className="m-smile m-stroke" pathLength="1" />
      </g>
      <rect x="54" y="86" width="12" height="9" className="m-stand" />
      <rect x="38" y="95" width="44" height="8" rx="4" className="m-stand" />
    </svg>
  );
}
