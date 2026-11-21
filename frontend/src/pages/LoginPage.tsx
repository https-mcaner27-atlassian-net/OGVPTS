/**
 * Giriş sayfası (FR-01).
 *
 * React kavramı — Kontrollü form (controlled input):
 *   Input'un değeri state'te tutulur: value={email} ve her tuşta onChange ile güncellenir.
 *   Böylece formun güncel değeri her an elimizdedir (doğrulama, gönderme vb. için).
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiError } from '../api/client'
import ErrorAlert from '../components/ErrorAlert'

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  // Zaten giriş yapılmışsa ana sayfaya git
  if (user) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault() // formun sayfayı yenilemesini engelle (tarayıcının varsayılan davranışı)
    setBusy(true)
    setError(null)
    try {
      await login(email, password)
      // RequireRole bizi buraya yönlendirdiyse geldiğimiz sayfaya dön
      const from = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Giriş yapılamadı.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-bg d-flex align-items-center justify-content-center p-3">
      <div className="card shadow-lg border-0" style={{ maxWidth: 420, width: '100%' }}>
        <div className="card-body p-4 p-md-5">
          <div className="text-center mb-4">
            <span className="brand-mark mb-3" style={{ width: '3rem', height: '3rem', fontSize: '1.5rem' }}>
              <i className="bi bi-mortarboard-fill" />
            </span>
            <h1 className="h4 mb-1">OGVPTS</h1>
            <p className="text-secondary small mb-0">Öğrenci–Danışman Görüşme ve Proje Takip Sistemi</p>
          </div>

          <ErrorAlert message={error} />

          <form onSubmit={handleSubmit} noValidate>
            <div className="mb-3">
              <label htmlFor="email" className="form-label">
                Okul e-postası
              </label>
              <input
                id="email"
                type="email"
                className="form-control"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="mb-4">
              <label htmlFor="password" className="form-label">
                Şifre
              </label>
              <input
                id="password"
                type="password"
                className="form-control"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary w-100" disabled={busy || !email || !password}>
              {busy ? <span className="spinner-border spinner-border-sm me-2" /> : <i className="bi bi-box-arrow-in-right me-2" />}
              Giriş yap
            </button>
          </form>

          {/* import.meta.env.DEV: yalnızca "npm run dev" ile çalışırken true olur; derlenmiş sürümde görünmez */}
          {import.meta.env.DEV && (
            <div className="alert alert-light border small mt-4 mb-0">
              <div className="fw-semibold mb-1">Demo hesaplar (şifre: Demo1234!)</div>
              <div>yonetici@ogvpts.test · ayse.yildiz@ogvpts.test · ali.demir@ogvpts.test</div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
