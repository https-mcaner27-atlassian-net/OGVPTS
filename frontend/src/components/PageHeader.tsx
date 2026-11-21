import type { ReactNode } from 'react'

/**
 * Sayfa başlığı. "actions" ile sağ tarafa buton koyulabilir.
 * React kavramı — props: bileşene dışarıdan verilen parametreler (HTML attribute'ları gibi).
 */
export default function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="d-flex flex-wrap align-items-end justify-content-between gap-2 mb-4">
      <div>
        <h1 className="h3 mb-1">{title}</h1>
        {subtitle && <p className="text-secondary mb-0">{subtitle}</p>}
      </div>
      {actions && <div className="d-flex gap-2">{actions}</div>}
    </div>
  )
}
