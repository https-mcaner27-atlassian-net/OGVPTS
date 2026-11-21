/**
 * Sayfa koruyucu (route guard).
 *
 * Giriş yapılmamışsa /giris sayfasına, rol uymuyorsa ana sayfaya yönlendirir.
 * Bu yalnızca KULLANICI DENEYİMİ içindir: asıl güvenlik backend'dedir (SecurityConfig + AccessGuard).
 * Kötü niyetli biri bu kontrolü atlasa bile sunucu 403 döner.
 */
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { UserRole } from '../api/types'
import { useAuth } from './AuthContext'
import Loading from '../components/Loading'

export default function RequireRole({ roles }: { roles?: UserRole[] }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <Loading />
  }
  if (!user) {
    // state: giriş yaptıktan sonra kullanıcıyı geldiği sayfaya geri götürmek için
    return <Navigate to="/giris" replace state={{ from: location.pathname }} />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  // <Outlet />: bu korumanın altındaki asıl sayfa burada çizilir
  return <Outlet />
}
