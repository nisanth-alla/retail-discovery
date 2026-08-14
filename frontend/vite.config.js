import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/home/',
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/datastore': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/replicate': {
        target: 'https://api.replicate.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/replicate/, ''),
        secure: true,
      },
    },
  },
  build: {
    outDir: '../src/main/resources/static/home',
    emptyOutDir: true,
  },
})
