/**
 * Görevler listesi (FR-13).
 * Öğrenci: grubunun görevleri. Danışman: atadığı görevler + yeni görev atama formu.
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { groupApi, taskApi } from '../../api/endpoints'
import type { TaskStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import StatusBadge from '../../components/StatusBadge'
import { daysUntil, formatDate, taskStatus, todayIso } from '../../utils/format'

export default function TasksPage() {
  const { user } = useAuth()
  const isAdvisor = user?.role === 'DANISMAN'
  const tasks = useApi(() => (isAdvisor || user?.groupId ? taskApi.list() : Promise.resolve([])), [isAdvisor, user?.groupId])
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('')

  if (!isAdvisor && !user?.groupId) {
    return (
      <EmptyState icon="people" title="Görevleri görmek için önce bir proje grubunda olmalısın">
        <Link to="/grubum">Grubunu oluştur</Link>
      </EmptyState>
    )
  }

  // .filter ile ekranda gösterilecek listeyi süzüyoruz; asıl veri (tasks.data) değişmiyor.
  const visible = (tasks.data ?? []).filter((t) => !statusFilter || t.status === statusFilter)

  return (
    <>
      <PageHeader title="Görevler" subtitle={isAdvisor ? 'Gruplarına atadığın görevler ve teslim durumları' : 'Danışmanının grubuna atadığı görevler'} />

      {isAdvisor && <CreateTaskForm onCreated={() => void tasks.reload()} />}

      <div className="d-flex justify-content-end mb-2">
        <select className="form-select" style={{ maxWidth: 240 }} value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as TaskStatus | '')} aria-label="Durum filtresi">
          <option value="">Tüm durumlar</option>
          {(Object.keys(taskStatus) as TaskStatus[]).map((s) => (
            <option key={s} value={s}>
              {taskStatus[s].label}
            </option>
          ))}
        </select>
      </div>

      {tasks.loading && <Loading />}
      <ErrorAlert message={tasks.error} />
      {!tasks.loading && visible.length === 0 && <EmptyState icon="list-check" title="Görev yok" />}

      {visible.length > 0 && (
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Görev</th>
                  {isAdvisor && <th>Grup</th>}
                  <th>Son tarih</th>
                  <th>Teslim</th>
                  <th>Durum</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((t) => {
                  const left = daysUntil(t.dueDate)
                  return (
                    <tr key={t.id}>
                      <td>
                        <Link to={`/gorevler/${t.id}`} className="fw-semibold text-decoration-none">
                          {t.title}
                        </Link>
                      </td>
                      {isAdvisor && <td>{t.groupName}</td>}
                      <td className={left < 0 && t.status !== 'ONAYLANDI' ? 'text-danger' : ''}>{formatDate(t.dueDate)}</td>
                      <td>{t.submissionCount}</td>
                      <td>
                        <StatusBadge badge={taskStatus[t.status]} />
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}

/** Danışman: kendi gruplarından birine görev ata. */
function CreateTaskForm({ onCreated }: { onCreated: () => void }) {
  const groups = useApi(() => groupApi.advisorGroups(), [])
  const action = useAction()
  const [open, setOpen] = useState(false)
  const [groupId, setGroupId] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [dueDate, setDueDate] = useState(todayIso(7))

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (await action.run(() => taskApi.create(Number(groupId), title, description, dueDate))) {
      setTitle('')
      setDescription('')
      setOpen(false)
      onCreated()
    }
  }

  if (!open) {
    return (
      <button className="btn btn-primary mb-3" onClick={() => setOpen(true)}>
        <i className="bi bi-plus-lg me-1" />
        Yeni görev ata
      </button>
    )
  }

  return (
    <div className="card mb-4">
      <div className="card-header bg-white fw-semibold">Yeni görev</div>
      <div className="card-body">
        <ErrorAlert message={action.error ?? groups.error} onClose={() => action.setError(null)} />
        {groups.data?.length === 0 && <p className="text-secondary">Sorumlu olduğun grup yok; görev atayabilmek için yöneticinin sana grup ataması gerekir.</p>}
        <form onSubmit={submit} className="row g-3">
          <div className="col-md-4">
            <label className="form-label" htmlFor="tgroup">Grup *</label>
            <select id="tgroup" className="form-select" value={groupId} onChange={(e) => setGroupId(e.target.value)} required>
              <option value="">Seç…</option>
              {groups.data?.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.name}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-5">
            <label className="form-label" htmlFor="ttitle">Başlık *</label>
            <input id="ttitle" className="form-control" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required />
          </div>
          <div className="col-md-3">
            <label className="form-label" htmlFor="tdue">Son teslim tarihi *</label>
            <input id="tdue" type="date" className="form-control" min={todayIso()} value={dueDate} onChange={(e) => setDueDate(e.target.value)} required />
          </div>
          <div className="col-12">
            <label className="form-label" htmlFor="tdesc">Açıklama</label>
            <textarea id="tdesc" className="form-control" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="col-12 d-flex gap-2">
            <button className="btn btn-primary" disabled={action.busy || !groupId || !title.trim()}>
              Görevi ata
            </button>
            <button type="button" className="btn btn-light" onClick={() => setOpen(false)}>
              Vazgeç
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
