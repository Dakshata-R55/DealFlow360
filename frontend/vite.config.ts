import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const apiProxyTarget =
  process.env.DOCKER === '1' ? 'http://backend:18080' : 'http://127.0.0.1:18080'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 5178,
    strictPort: true,
  },
  preview: {
    host: '0.0.0.0',
    port: 5178,
    strictPort: true,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
})
