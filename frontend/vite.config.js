import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  build: {
    // Spring Boot serves the built app from its classpath (see the frontend plugin in pom.xml).
    outDir: '../target/classes/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // During development (npm run dev) API calls go to the Spring Boot backend on port 8080.
    proxy: {
      '/api': backend,
      '/logout': backend,
      // GET /login is the React sign-in page; POST /login is Spring Security's sign-in endpoint.
      '/login': { target: backend, bypass: (req) => (req.method === 'GET' ? '/index.html' : undefined) },
    },
  },
});
