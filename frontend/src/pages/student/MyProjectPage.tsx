/**
 * Öğrencinin projesi (FR-05, FR-06).
 * Proje yoksa oluşturma formu; varsa ProjectView ile ayrıntı ve düzenleme.
 *
 * React kavramı — Dizi state'i:
 *   Aşamalar bir dizi (milestones). State'i DOĞRUDAN değiştirmeyiz (push yok);
 *   her seferinde yeni bir dizi üretip set ederiz: setMilestones([...eski, yeni]).
 *   React değişikliği ancak böyle "fark eder".
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { projectApi } from '../../api/endpoints'
import type { NewMilestone } from '../../api/endpoints'
import { useApi, useAction } from '../../hooks/useApi'
import { useAuth } from '../../auth/AuthContext'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import ProjectView from '../../components/ProjectView'

export default function MyProjectPage() {
  const { user } = useAuth()
  const { data: project, setData, loading, error } = useApi(() => (user?.groupId ? projectApi.mine() : Promise.resolve(null)), [user?.groupId])

  if (!user?.groupId) {
    return (
      <EmptyState icon="people" title="Önce bir proje grubu gerekli">
        <Link to="/grubum">Grubunu oluştur</Link>
      </EmptyState>
    )
  }
  if (loading) return <Loading />
  if (error) return <ErrorAlert message={error} />

  if (!project) {
    return (
      <>
        <PageHeader title="Projeni oluştur" subtitle="Proje adı, açıklaması ve en az bir aşama gerekli." />
        <CreateProjectForm onCreated={setData} />
      </>
    )
  }

  return (
    <>
      <PageHeader title="Projem" />
      <ProjectView project={project} onChange={setData} canEdit />
    </>
  )
}

function CreateProjectForm({ onCreated }: { onCreated: (p: Awaited<ReturnType<typeof projectApi.create>>) => void }) {
  const action = useAction()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [milestones, setMilestones] = useState<NewMilestone[]>([{ title: '', dueDate: '' }])

  /** i. aşamanın bir alanını güncelle (diğerlerine dokunmadan yeni dizi üret). */
  function updateMilestone(i: number, field: keyof NewMilestone, value: string) {
    setMilestones((list) => list.map((m, idx) => (idx === i ? { ...m, [field]: value } : m)))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    const filled = milestones.filter((m) => m.title.trim() && m.dueDate)
    if (filled.length === 0) {
      action.setError('En az bir aşama (başlık ve teslim tarihi) girmelisin.')
      return
    }
    const created = await action.run(() => projectApi.create(title, description, filled))
    if (created) onCreated(created)
  }

  return (
    <div className="card" style={{ maxWidth: 760 }}>
      <div className="card-body">
        <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
        <form onSubmit={submit}>
          <div className="mb-3">
            <label className="form-label" htmlFor="ptitle">
              Proje adı *
            </label>
            <input id="ptitle" className="form-control" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required />
          </div>
          <div className="mb-4">
            <label className="form-label" htmlFor="pdesc">
              Açıklama
            </label>
            <textarea id="pdesc" className="form-control" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <label className="form-label">Aşamalar *</label>
          {milestones.map((m, i) => (
            <div className="row g-2 mb-2" key={i}>
              <div className="col-sm-7">
                <input className="form-control" placeholder={`Aşama ${i + 1} (ör. Gereksinim analizi)`} value={m.title} onChange={(e) => updateMilestone(i, 'title', e.target.value)} />
              </div>
              <div className="col-8 col-sm-4">
                <input type="date" className="form-control" value={m.dueDate} onChange={(e) => updateMilestone(i, 'dueDate', e.target.value)} aria-label="Teslim tarihi" />
              </div>
              <div className="col-4 col-sm-1 d-grid">
                <button
                  type="button"
                  className="btn btn-outline-danger"
                  title="Kaldır"
                  disabled={milestones.length === 1}
                  onClick={() => setMilestones((list) => list.filter((_, idx) => idx !== i))}
                >
                  <i className="bi bi-x-lg" />
                </button>
              </div>
            </div>
          ))}
          <button type="button" className="btn btn-link px-0 mb-3" onClick={() => setMilestones((list) => [...list, { title: '', dueDate: '' }])}>
            <i className="bi bi-plus-circle me-1" />
            Aşama ekle
          </button>

          <div>
            <button className="btn btn-primary" disabled={action.busy || !title.trim()}>
              Projeyi oluştur
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
