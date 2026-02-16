import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const target = 'http://localhost:8080'
// const target = 'http://192.168.1.10:8080'
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: target,
        changeOrigin: true,
        secure: false,
      },
      '/oauth2': {
        target: target,
        changeOrigin: true,
        secure: false,
      },
      '/login': {
        target: target,
        changeOrigin: true,
        secure: false,
      },
      '/logout': {
        target: target,
        changeOrigin: true,
        secure: false,
      }
    }
  }
})