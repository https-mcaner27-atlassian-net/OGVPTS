import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/**
 * Vite ayarları.
 *
 * - server.proxy: Geliştirme sırasında React http://localhost:5173 adresinde, Spring Boot
 *   http://localhost:8080 adresinde çalışır. "/api" ile başlayan istekleri Vite, Spring Boot'a
 *   yönlendirir. Böylece kodda tam adres yazmayız ve CORS sorunu yaşamayız.
 *
 * - build.outDir: "npm run build" çıktısı doğrudan Spring Boot'un static klasörüne yazılır.
 *   Sonra Spring Boot tek başına hem API'yi hem arayüzü sunar (http://localhost:8080).
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
