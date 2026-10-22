/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      // Soft white and light blue palette
      colors: {
        canvas: '#f3f9ff', // page background
        panel: '#ffffff', // cards
        soft: '#e8f3ff', // hovered rows, inputs
        line: '#d3e7fb', // borders
        accent: '#5bb0f2', // light blue
        deep: '#2f86d0', // darker blue for text on white
        ink: '#34495e', // main text
        muted: '#7f95aa', // secondary text
        ok: '#4db38a',
        warn: '#f0a94a',
        bad: '#ea7a7a',
      },
      fontFamily: {
        sans: ['Nunito', 'Quicksand', 'ui-rounded', '"Segoe UI"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
