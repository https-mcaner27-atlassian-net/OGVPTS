/**
 * Hata kutusu. message boşsa hiçbir şey çizmez.
 *
 * React kavramı — Koşullu çizim:
 *   `if (!message) return null` → bileşen ekrana bir şey basmaz.
 *   JSX içinde de `{koşul && <Etiket />}` kalıbı sık kullanılır.
 */
export default function ErrorAlert({ message, onClose }: { message: string | null; onClose?: () => void }) {
  if (!message) return null
  return (
    <div className="alert alert-danger d-flex align-items-start gap-2" role="alert">
      <i className="bi bi-exclamation-triangle-fill mt-1" />
      <div className="flex-grow-1">{message}</div>
      {onClose && <button type="button" className="btn-close" aria-label="Kapat" onClick={onClose} />}
    </div>
  )
}
