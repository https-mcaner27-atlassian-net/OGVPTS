/**
 * Giriş yapıldıktan sonraki tüm sayfaların ortak çerçevesi: üst menü + içerik alanı.
 *
 * Menü, kullanıcının rolüne göre değişir (öğrenci / danışman / yönetici).
 * Mobilde menü "hamburger" düğmesiyle açılır (NFR-04 mobil uyumluluk).
 */
import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { notificationApi } from '../api/endpoints'
import type { UserRole } from '../api/types'
import { roleLabels } from '../utils/format'

type MenuItem = { to: string; label: string; icon: string }

/** Rol → menü öğeleri. Yeni bir sayfa eklerken buraya bir satır eklemek yeterli. */
const MENUS: Record<UserRole, MenuItem[]> = {
  OGRENCI: [
    { to: '/', label: 'Panel', icon: 'speedometer2' },
    { to: '/grubum', label: 'Grubum', icon: 'people' },
    { to: '/projem', label: 'Projem', icon: 'kanban' },
    { to: '/randevular', label: 'Randevular', icon: 'calendar-event' },
    { to: '/gorevler', label: 'Görevler', icon: 'list-check' },
  ],
  DANISMAN: [
    { to: '/', label: 'Panel', icon: 'speedometer2' },
    { to: '/musaitlik', label: 'Müsaitlik', icon: 'calendar-plus' },
    { to: '/randevular', label: 'Randevular', icon: 'calendar-event' },
    { to: '/projeler', label: 'Projeler', icon: 'kanban' },
    { to: '/gorevler', label: 'Görevler', icon: 'list-check' },
  ],
  YONETICI: [
    { to: '/', label: 'Panel', icon: 'speedometer2' },
    { to: '/yonetim/eslestirme', label: 'Eşleştirme', icon: 'diagram-3' },
    { to: '/yonetim/kullanicilar', label: 'Kullanıcılar', icon: 'person-gear' },
    { to: '/yonetim/raporlar', label: 'Raporlar', icon: 'bar-chart' },
  ],
}

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const [unread, setUnread] = useState(0)

  // Sayfa değişince okunmamış bildirim sayısını tazele.
  // (Sunucudan gelen cevapla state güncellemek, effect'lerin doğru kullanımıdır.)
  useEffect(() => {
    notificationApi
      .unreadCount()
      .then((r) => setUnread(r.count))
      .catch(() => undefined)
  }, [location.pathname])

  // Her 60 saniyede bir okunmamış bildirim sayısını kontrol et.
  // Effect'ten döndürülen fonksiyon "temizlik"tir: bileşen kapanınca zamanlayıcıyı durdurur.
  useEffect(() => {
    const id = window.setInterval(() => {
      notificationApi
        .unreadCount()
        .then((r) => setUnread(r.count))
        .catch(() => undefined)
    }, 60_000)
    return () => window.clearInterval(id)
  }, [])

  if (!user) return null
  const menu = MENUS[user.role]

  function handleLogout() {
    logout()
    navigate('/giris')
  }

  return (
    <>
      <nav className="navbar navbar-expand-lg bg-white border-bottom sticky-top app-navbar">
        <div className="container">
          <Link className="navbar-brand d-flex align-items-center gap-2 fw-bold" to="/">
            <span className="brand-mark">
              <i className="bi bi-mortarboard-fill" />
            </span>
            OGVPTS
          </Link>

          <button
            className="navbar-toggler"
            type="button"
            aria-label="Menüyü aç/kapat"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <span className="navbar-toggler-icon" />
          </button>

          <div className={`collapse navbar-collapse ${menuOpen ? 'show' : ''}`}>
            <ul className="navbar-nav me-auto mb-2 mb-lg-0">
              {/* Listeyi .map ile JSX'e çeviriyoruz. Her öğeye benzersiz "key" vermek zorunlu:
                  React hangi öğenin değiştiğini bu anahtarla takip eder. */}
              {menu.map((item) => (
                <li className="nav-item" key={item.to}>
                  <NavLink className="nav-link" to={item.to} end={item.to === '/'} onClick={() => setMenuOpen(false)}>
                    <i className={`bi bi-${item.icon} me-1`} />
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>

            <div className="d-flex align-items-center gap-2">
              <NavLink to="/bildirimler" className="btn btn-light position-relative" title="Bildirimler">
                <i className="bi bi-bell" />
                {unread > 0 && (
                  <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger">
                    {unread > 99 ? '99+' : unread}
                  </span>
                )}
              </NavLink>
              <div>
                <NavLink to="/hesabim" className="btn btn-light d-flex align-items-center gap-2">
                  <i className="bi bi-person-circle" />
                  <span className="d-none d-md-inline text-start lh-sm">
                    <span className="d-block small fw-semibold">{user.fullName}</span>
                    <span className="d-block text-secondary" style={{ fontSize: '.75rem' }}>
                      {roleLabels[user.role]}
                    </span>
                  </span>
                </NavLink>
              </div>
              <button className="btn btn-outline-secondary" onClick={handleLogout} title="Çıkış">
                <i className="bi bi-box-arrow-right" />
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="container py-4">
        {/* Hangi sayfa açıksa (App.tsx'teki rotalar) burada çizilir */}
        <Outlet />
      </main>

      <footer className="container pb-4 small text-secondary d-flex flex-wrap gap-3 justify-content-between">
        <span>OGVPTS · Maltepe Üniversitesi</span>
        <Link to="/kvkk" className="link-secondary">
          KVKK aydınlatma metni
        </Link>
      </footer>
    </>
  )
}
