// Pixel, the cute little PC. Drawn with SVG, animated with plain CSS (see index.css).
//   mode="hello"    bounces in and waves (intro)
//   mode="idle"     blinks and its heart beats
//   mode="scanning" looks left and right while a scan runs
export default function Mascot({ size = 140, mode = 'idle' }) {
  const css = { hello: 'm-bounce', idle: '', scanning: 'm-look' }[mode];
  return (
    <svg className={`mascot ${css}`} width={size} height={size} viewBox="0 0 120 120" role="img" aria-label="Pixel the PC mascot">
      {/* antenna with a heart */}
      <path d="M60 22 Q60 12 60 10" stroke="#9fd0f7" strokeWidth="3" strokeLinecap="round" fill="none" />
      <path className="m-heart" d="M60 12 C52 4 46 12 60 20 C74 12 68 4 60 12Z" fill="#ff9db8" />
      {/* arms and feet */}
      <rect x="4" y="52" width="14" height="9" rx="4.5" fill="#bfe0fb" />
      <g className={mode === 'hello' ? 'm-arm-wave' : ''}>
        <rect x="102" y="52" width="14" height="9" rx="4.5" fill="#bfe0fb" />
      </g>
      <rect x="34" y="96" width="18" height="9" rx="4.5" fill="#bfe0fb" />
      <rect x="68" y="96" width="18" height="9" rx="4.5" fill="#bfe0fb" />
      {/* body and screen */}
      <rect x="14" y="22" width="92" height="78" rx="26" fill="#ffffff" stroke="#9fd0f7" strokeWidth="3" />
      <rect x="23" y="31" width="74" height="58" rx="18" fill="#e6f3ff" />
      {/* eyes */}
      <g className="m-eye"><ellipse cx="45" cy="58" rx="7" ry="9" fill="#34495e" /></g>
      <g className="m-eye"><ellipse cx="75" cy="58" rx="7" ry="9" fill="#34495e" /></g>
      <g className="m-pupils">
        <circle cx="47" cy="54.5" r="2.6" fill="#fff" />
        <circle cx="77" cy="54.5" r="2.6" fill="#fff" />
      </g>
      {/* rosy cheeks and smile */}
      <ellipse cx="34" cy="72" rx="6" ry="4" fill="#ffc2d1" opacity="0.8" />
      <ellipse cx="86" cy="72" rx="6" ry="4" fill="#ffc2d1" opacity="0.8" />
      <path d="M52 71 Q60 79 68 71" stroke="#34495e" strokeWidth="3" strokeLinecap="round" fill="none" />
    </svg>
  );
}
