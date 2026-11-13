// Dark (black and orange) or light (warm white and orange). The choice is remembered in this browser.
export const getTheme = () => document.documentElement.dataset.theme || 'dark';

export function setTheme(theme) {
  document.documentElement.dataset.theme = theme;
  try {
    localStorage.setItem('theme', theme);
  } catch {
    // the choice is just not remembered
  }
}
