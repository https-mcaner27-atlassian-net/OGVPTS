/**
 * React kavramı — Bileşen (component):
 *   Ekranın bir parçasını çizen fonksiyon. Büyük harfle başlar ve JSX döndürür.
 *   JSX: JavaScript içinde HTML'e benzeyen yazım. "class" yerine "className" kullanılır.
 */
export default function Loading({ text = 'Yükleniyor…' }: { text?: string }) {
  return (
    <div className="d-flex align-items-center gap-2 text-secondary py-4" role="status">
      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
      <span>{text}</span>
    </div>
  )
}
