import type { ReactNode } from 'react'

/** Liste boşken gösterilen kutu. */
export default function EmptyState({ icon = 'inbox', title, children }: { icon?: string; title: string; children?: ReactNode }) {
  return (
    <div className="text-center text-secondary border rounded-3 py-5 px-3 bg-body-tertiary">
      <i className={`bi bi-${icon} fs-1 d-block mb-2`} />
      <p className="fw-semibold mb-1">{title}</p>
      {children && <div className="small">{children}</div>}
    </div>
  )
}
