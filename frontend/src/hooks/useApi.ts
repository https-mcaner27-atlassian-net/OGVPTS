/**
 * Sunucudan veri yüklemek için tekrar kullanılabilir hook.
 *
 * Her sayfada aynı üçlü lazım: veri, "yükleniyor mu?", "hata var mı?". Bunu tek yerde yazdık:
 *
 *   const { data, loading, error, reload } = useApi(() => taskApi.list(), [])
 *
 * - İlk parametre: veriyi getiren fonksiyon (Promise döner).
 * - İkinci parametre: bu değerlerden biri değişince veriyi yeniden yükle (useEffect bağımlılıkları).
 * - reload(): bir işlemden sonra (ör. onay) listeyi tazelemek için.
 * - setData: sunucu güncel nesneyi döndürdüğünde listeyi yeniden çekmeden ekranı güncellemek için.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import type { DependencyList } from 'react'
import { ApiError } from '../api/client'

export function useApi<T>(fetcher: () => Promise<T>, deps: DependencyList) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // useRef: çizimler arasında değeri koruyan ama değişince yeniden çizim YAPTIRMAYAN kutu.
  // En güncel fetcher'ı burada tutuyoruz.
  const fetcherRef = useRef(fetcher)
  fetcherRef.current = fetcher

  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await fetcherRef.current())
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Veri yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void reload() // "void": Promise'in sonucunu beklemediğimizi açıkça belirtir
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { data, setData, loading, error, reload }
}

/**
 * Buton tıklamaları gibi "işlem" çağrıları için yardımcı:
 * çalışırken busy=true (buton devre dışı kalır), hata olursa mesajı error'a yazar.
 */
export function useAction() {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async <R,>(action: () => Promise<R>): Promise<R | undefined> => {
    setBusy(true)
    setError(null)
    try {
      return await action()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'İşlem başarısız oldu.')
      return undefined
    } finally {
      setBusy(false)
    }
  }, [])

  return { busy, error, setError, run }
}
