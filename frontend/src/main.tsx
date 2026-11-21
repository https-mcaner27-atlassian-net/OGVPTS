/**
 * Uygulamanın giriş noktası. index.html'deki <div id="root"> içine React'i bağlar.
 *
 * Sarmalama sırası önemli (dıştan içe):
 *   StrictMode     → geliştirme sırasında olası hataları erken yakalar
 *   BrowserRouter  → adres çubuğundaki URL'ye göre sayfa seçimi (React Router)
 *   AuthProvider   → giriş yapmış kullanıcı bilgisini tüm sayfalara dağıtır
 *   App            → rotalar ve sayfalar
 */
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.css'
import './styles.css'
import App from './App'
import { AuthProvider } from './auth/AuthContext'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
