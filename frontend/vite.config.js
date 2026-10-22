import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the React app runs on port 5173 and forwards /api calls
// to the Spring Boot backend on port 8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    // Recharts is fairly large; one bundle is fine for a desktop app served locally.
    chunkSizeWarningLimit: 800,
  },
  test: {
    environment: 'node',
  },
});
