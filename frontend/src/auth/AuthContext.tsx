/**
 * Oturum (giriş yapmış kullanıcı) bilgisini TÜM uygulamaya dağıtan yapı.
 *
 * React kavramı — Context:
 *   Normalde bir bileşen veriyi alt bileşenlere "props" ile tek tek geçirir. Kullanıcı bilgisi
 *   ise neredeyse her sayfada lazım; her katmandan props geçirmek zahmetli olur ("prop drilling").
 *   Context, veriyi bir "yayın" gibi en üstte bir kez koyar; altındaki her bileşen useAuth()
 *   ile doğrudan okuyabilir.
 *
 * React kavramı — State (useState):
 *   Bileşenin "hafızası". Değer değişince React ekranı otomatik yeniden çizer.
 *   const [user, setUser] = useState(...) → user: mevcut değer, setUser: değiştirme fonksiyonu.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/endpoints'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import type { Me } from '../api/types'

type AuthState = {
  user: Me | null // null → giriş yapılmamış
  loading: boolean // sayfa ilk açıldığında token geçerli mi diye bakarken true
  login: (email: string, password: string) => Promise<Me>
  logout: () => void
  refresh: () => Promise<void> // ör. öğrenci grup kurunca grup bilgisini tazelemek için
}

// Context'in kendisi. Başlangıç değeri null; gerçek değeri AuthProvider verir.
const AuthContext = createContext<AuthState | null>(null)

/** Uygulamayı saran "sağlayıcı" bileşen (bkz. main.tsx). children: içine konan her şey. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null)
  // Başlangıç değeri bir fonksiyonla hesaplanabilir: token varsa doğrulayana kadar "yükleniyor".
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  // useCallback: fonksiyonu her çizimde yeniden üretmemek için hafızada tutar.
  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  const refresh = useCallback(async () => {
    setUser(await authApi.me())
  }, [])

  /**
   * React kavramı — Effect (useEffect):
   *   "Ekran çizildikten SONRA şunu yap" demektir. Sunucudan veri çekmek gibi yan işler buraya yazılır.
   *   Sondaki [] (bağımlılık listesi) boş olduğu için yalnızca bileşen ilk açıldığında bir kez çalışır.
   */
  useEffect(() => {
    // Token süresi dolarsa API istemcisi bu fonksiyonu çağırır → kullanıcı çıkış yapmış olur.
    setUnauthorizedHandler(logout)

    // Sayfa yenilendiğinde: elimizde token varsa hâlâ geçerli mi diye sunucuya sor.
    if (!tokenStore.get()) {
      return
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password)
    tokenStore.set(res.token)
    setUser(res.user)
    return res.user
  }, [])

  // useMemo: aynı değerler için aynı nesneyi döndür; gereksiz yeniden çizimleri önler.
  const value = useMemo(() => ({ user, loading, login, logout, refresh }), [user, loading, login, logout, refresh])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/**
 * Özel hook: herhangi bir bileşende `const { user, logout } = useAuth()` diye kullanılır.
 * (Hook = "use" ile başlayan, React özelliklerini kullanan fonksiyon.)
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth, AuthProvider içinde kullanılmalı')
  }
  return ctx
}
