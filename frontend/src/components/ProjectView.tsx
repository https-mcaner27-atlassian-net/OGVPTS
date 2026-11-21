/**
 * Proje ayrıntısı: ilerleme, aşamalar ve danışman yorumları.
 * Hem öğrenci (Projem) hem danışman/yönetici (Projeler → ayrıntı) bu bileşeni kullanır.
 *
 * Yetkiye göre farklı kontroller gösterilir:
 *   canEdit    → öğrenci: aşama durumu değiştirme, aşama ekleme/silme (FR-06)
 *   canComment → danışman: yorum yazma (FR-07)
 * (Sunucu da aynı kuralları ayrıca denetler.)
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { projectApi } from '../api/endpoints'
import type { Project, ProjectStatus } from '../api/types'
import { useAction } from '../hooks/useApi'
import ErrorAlert from './ErrorAlert'
import StatusBadge from './StatusBadge'
import { daysUntil, formatDate, formatDateTime, projectStatus } from '../utils/format'

type Props = {
  project: Project
  onChange: (p: Project) => void // sunucu güncel projeyi döndürünce üst bileşene haber ver
  canEdit?: boolean
  canComment?: boolean
}

const STATUSES: ProjectStatus[] = ['BASLAMADI', 'DEVAM_EDIYOR', 'TAMAMLANDI']

export default function ProjectView({ project, onChange, canEdit = false, canComment = false }: Props) {
  const action = useAction()
  const [comment, setComment] = useState('')
  const [newTitle, setNewTitle] = useState('')
  const [newDue, setNewDue] = useState('')

  async function changeStatus(milestoneId: number, status: ProjectStatus) {
    const updated = await action.run(() => projectApi.setMilestoneStatus(milestoneId, status))
    if (updated) onChange(updated)
  }

  async function removeMilestone(milestoneId: number) {
    if (!window.confirm('Bu aşamayı silmek istediğine emin misin?')) return
    const updated = await action.run(() => projectApi.deleteMilestone(milestoneId))
    if (updated) onChange(updated)
  }

  async function addMilestone(e: FormEvent) {
    e.preventDefault()
    const updated = await action.run(() => projectApi.addMilestone(project.id, { title: newTitle, dueDate: newDue }))
    if (updated) {
      onChange(updated)
      setNewTitle('')
      setNewDue('')
    }
  }

  async function sendComment(e: FormEvent) {
    e.preventDefault()
    const updated = await action.run(() => projectApi.comment(project.id, comment))
    if (updated) {
      onChange(updated)
      setComment('')
    }
  }

  return (
    <>
      <ErrorAlert message={action.error} onClose={() => action.setError(null)} />

      <div className="card mb-3">
        <div className="card-body">
          <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
            <div>
              <div className="small text-secondary">
                {project.groupName} · Danışman: {project.advisorName ?? 'atanmadı'}
              </div>
              <h2 className="h4 mb-0">{project.title}</h2>
            </div>
            <StatusBadge badge={projectStatus[project.status]} />
          </div>
          {project.description && <p className="text-secondary text-pre-wrap mb-3">{project.description}</p>}
          <div className="d-flex align-items-center gap-2">
            <div className="progress flex-grow-1" style={{ height: 10 }}>
              <div className="progress-bar bg-success" style={{ width: `${project.progressPercent}%` }} />
            </div>
            <span className="small fw-semibold">%{project.progressPercent}</span>
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">Aşamalar</div>
            <ul className="list-group list-group-flush">
              {project.milestones.map((m) => {
                const left = daysUntil(m.dueDate)
                const late = left < 0 && m.status !== 'TAMAMLANDI'
                return (
                  <li key={m.id} className="list-group-item">
                    <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
                      <div>
                        <div className="fw-semibold">{m.title}</div>
                        <div className={`small ${late ? 'text-danger' : 'text-secondary'}`}>
                          Teslim: {formatDate(m.dueDate)} {late && '· gecikti'}
                        </div>
                      </div>
                      {canEdit ? (
                        <div className="d-flex gap-2">
                          {/* Açılır liste: seçim değişince sunucuya kaydet */}
                          <select
                            className="form-select form-select-sm"
                            style={{ width: 'auto' }}
                            value={m.status}
                            disabled={action.busy}
                            onChange={(e) => changeStatus(m.id, e.target.value as ProjectStatus)}
                            aria-label="Aşama durumu"
                          >
                            {STATUSES.map((s) => (
                              <option key={s} value={s}>
                                {projectStatus[s].label}
                              </option>
                            ))}
                          </select>
                          <button
                            className="btn btn-sm btn-outline-danger"
                            title="Aşamayı sil"
                            disabled={action.busy || project.milestones.length <= 1}
                            onClick={() => removeMilestone(m.id)}
                          >
                            <i className="bi bi-trash" />
                          </button>
                        </div>
                      ) : (
                        <StatusBadge badge={projectStatus[m.status]} />
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
            {canEdit && (
              <div className="card-body border-top">
                <form onSubmit={addMilestone} className="row g-2">
                  <div className="col-sm-6">
                    <input className="form-control" placeholder="Yeni aşama başlığı" value={newTitle} onChange={(e) => setNewTitle(e.target.value)} required />
                  </div>
                  <div className="col-sm-4">
                    <input type="date" className="form-control" value={newDue} onChange={(e) => setNewDue(e.target.value)} required aria-label="Teslim tarihi" />
                  </div>
                  <div className="col-sm-2 d-grid">
                    <button className="btn btn-outline-primary" disabled={action.busy || !newTitle || !newDue}>
                      Ekle
                    </button>
                  </div>
                </form>
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-5">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">Danışman yorumları</div>
            <div className="card-body">
              {canComment && (
                <form onSubmit={sendComment} className="mb-3">
                  <textarea
                    className="form-control mb-2"
                    rows={3}
                    placeholder="Gruba yönlendirme / yorum yaz…"
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    required
                  />
                  <button className="btn btn-primary btn-sm" disabled={action.busy || !comment.trim()}>
                    <i className="bi bi-send me-1" />
                    Gönder
                  </button>
                </form>
              )}
              {project.comments.length === 0 && <p className="text-secondary small mb-0">Henüz yorum yok.</p>}
              <div className="d-flex flex-column gap-3">
                {project.comments.map((c) => (
                  <div key={c.id} className="timeline-item">
                    <div className="small text-secondary">
                      {c.authorName} · {formatDateTime(c.createdAt)}
                    </div>
                    <div className="text-pre-wrap">{c.body}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
