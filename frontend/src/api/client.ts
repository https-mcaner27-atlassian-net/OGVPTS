/**
 * Backend ile konuşan tek yer.
 *
 * Neden ayrı bir dosya? Her sayfada fetch(...) yazıp token eklemek, hataları ayıklamak
 * kod tekrarına yol açar. Burada bir kez yazıyoruz, her yerde `api.get(...)` diye kullanıyoruz.
 *
 * fetch: tarayıcının yerleşik HTTP istek fonksiyonu.
 * async/await: istek bitene kadar "bekle", sonra devam et (Java'daki future.get() gibi düşünebilirsin,
 * ama tarayıcıyı kilitlemez).
 */

const TOKEN_KEY = 'ogvpts.token'

/** Token tarayıcının localStorage'ında saklanır; sayfa yenilense de oturum kalır. */
export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/**
 * Backend'den gelen hata. Spring'in ProblemDetail yanıtındaki "detail" alanı mesaj olur.
 * Örn. { status: 409, code: "SLOT_CONFLICT", message: "Bu saatte onaylı başka bir görüşme var (BR-02)." }
 */
export class ApiError extends Error {
  status: number
  code: string | null
  fields: Record<string, string> | null

  constructor(status: number, message: string, code: string | null, fields: Record<string, string> | null) {
    super(message)
    this.status = status
    this.code = code
    this.fields = fields
  }
}

/** Oturum düştüğünde (401) AuthContext'e haber vermek için dinleyici. */
let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

/** Tüm isteklerin geçtiği ortak fonksiyon. <T>: dönecek verinin tipi (generic — Java'daki gibi). */
async function request<T>(method: Method, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = tokenStore.get()
  if (token) {
    headers['Authorization'] = `Bearer ${token}` // JWT her istekte bu başlıkla gider
  }

  let payload: BodyInit | undefined
  if (body instanceof FormData) {
    payload = body // dosya yükleme: tarayıcı Content-Type'ı kendisi ayarlar
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body) // JS nesnesi → JSON metni
  }

  let response: Response
  try {
    response = await fetch(url, { method, headers, body: payload })
  } catch {
    throw new ApiError(0, 'Sunucuya ulaşılamadı. Backend çalışıyor mu?', 'NETWORK', null)
  }

  if (response.status === 401 && !url.endsWith('/auth/login')) {
    onUnauthorized?.() // "?.": fonksiyon tanımlıysa çağır
    throw new ApiError(401, 'Oturumunuzun süresi doldu, lütfen tekrar giriş yapın.', 'UNAUTHORIZED', null)
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  // 204 No Content: gövde yok (ör. "grubunuz yok" ya da silme işlemi)
  if (response.status === 204 || response.status === 202) {
    return null as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const data = await response.json()
    return new ApiError(response.status, data.detail ?? 'Bir hata oluştu.', data.code ?? null, data.fields ?? null)
  } catch {
    // Gövde JSON değilse hata backend'den değil aradaki katmandan gelmiştir.
    // 502/503/504: geliştirmede Vite'ın yönlendirmesi (proxy) 8080'e ulaşamadı → backend kapalı.
    if (response.status === 502 || response.status === 503 || response.status === 504) {
      return new ApiError(response.status, 'Sunucuya ulaşılamadı. Backend çalışıyor mu?', 'NETWORK', null)
    }
    const fallback = response.status === 403 ? 'Bu işlem için yetkiniz yok.' : `İstek başarısız (${response.status}).`
    return new ApiError(response.status, fallback, null, null)
  }
}

/** Dosya indirme: token gerektiği için <a href> yetmez; dosyayı fetch ile alıp tarayıcıya kaydettiriyoruz. */
async function download(url: string, fallbackName: string): Promise<void> {
  const token = tokenStore.get()
  const response = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!response.ok) {
    throw await toApiError(response)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition') ?? ''
  // "filename*=UTF-8''..." (Türkçe karakterli adlar) veya "filename=..." biçimini çöz
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition) ?? /filename="?([^";]+)"?/i.exec(disposition)
  const name = match ? decodeURIComponent(match[1]) : fallbackName
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = name
  link.click()
  URL.revokeObjectURL(link.href)
}

/** Kullanım: api.get<Task[]>('/api/tasks'), api.post<Meeting>('/api/meetings', { slotId, topic }) */
export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body),
  put: <T>(url: string, body?: unknown) => request<T>('PUT', url, body),
  patch: <T>(url: string, body?: unknown) => request<T>('PATCH', url, body),
  delete: <T>(url: string) => request<T>('DELETE', url),
  download,
}
