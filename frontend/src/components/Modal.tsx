import type { ReactNode } from 'react'

/**
 * Basit modal (açılır pencere). Bootstrap'in JavaScript'ini kullanmadan, React state'iyle açıp kapatıyoruz:
 * bileşen ekranda çiziliyorsa pencere açık, çizilmiyorsa kapalı.
 *
 * Kullanım:  {secili && <Modal title="..." onClose={() => setSecili(null)}> içerik </Modal>}
 */
export default function Modal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <>
      <div className="modal d-block" tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby="modal-title" onClick={onClose}>
        {/* stopPropagation: pencerenin içine tıklayınca "dışarı tıklandı" sayılıp kapanmasın */}
        <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable" onClick={(e) => e.stopPropagation()}>
          <div className="modal-content">
            <div className="modal-header">
              <h2 className="modal-title h5" id="modal-title">
                {title}
              </h2>
              <button type="button" className="btn-close" aria-label="Kapat" onClick={onClose} />
            </div>
            <div className="modal-body">{children}</div>
          </div>
        </div>
      </div>
      <div className="modal-backdrop show" />
    </>
  )
}
