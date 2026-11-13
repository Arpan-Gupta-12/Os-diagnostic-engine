// The app logo: a small PC screen that smiles.
export default function Logo({ size = 40 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 64 64" role="img" aria-label="OS Diagnostic Engine">
      <rect width="64" height="64" rx="16" fill="#0d0d0d" />
      <rect x="11" y="13" width="42" height="31" rx="8" fill="#1b1612" stroke="#ff7a1a" strokeWidth="3.5" />
      <circle cx="25" cy="26" r="2.8" fill="#ffb066" />
      <circle cx="39" cy="26" r="2.8" fill="#ffb066" />
      <path d="M26 33 q6 5 12 0" stroke="#ffb066" strokeWidth="3" fill="none" strokeLinecap="round" />
      <rect x="29" y="44" width="6" height="5" fill="#ff7a1a" />
      <rect x="21" y="49" width="22" height="4" rx="2" fill="#ff7a1a" />
    </svg>
  );
}
