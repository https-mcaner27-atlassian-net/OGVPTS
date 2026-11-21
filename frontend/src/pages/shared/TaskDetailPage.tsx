/**
 * Görev ayrıntısı: açıklama, teslim geçmişi ve geri bildirimler.
 *
 * Öğrenci (FR-14): dosya yükler. Her yükleme yeni bir sürüm olur; öncekiler silinmez.
 * Danışman (FR-15): en son teslimi onaylar ya da düzeltme ister (açıklama zorunlu).
 *
 * React Router kavramı — useParams: adresteki ":id" değerini okur (/gorevler/5 → "5").
 */
import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { taskApi } from '../../api/endpoints'
import type { FeedbackDecision, Submission } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import StatusBadge from '../../components/StatusBadge'
import { daysUntil, decisionLabels, formatDate, formatDateTime, formatSize, taskStatus } from '../../utils/format'

const MAX_BYTES = 20 * 1024 * 1024
const ALLOWED = ['pdf', 'doc', 'docx', 'zip', 'pptx']

export default function TaskDetailPage() {
  const { id } = useParams()
  const taskId = Number(id)
  const { user } = useAuth()
  const { data, setData, loading, error } = useApi(() => taskApi.detail(taskId), [taskId])
  const action = useAction()

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const { task, submissions } = data
  const latest = submissions[0] // sunucu en yeni sürümü en başta gönderir
  const left = daysUntil(task.dueDate)
  const isStudent = user?.role === 'OGRENCI'
  const isAdvisor = user?.role === 'DANISMAN'

  return (
    <>
      <nav aria-label="breadcrumb" className="small mb-2">
        <Link to="/gorevler">← Görevler</Link>
      </nav>
      <PageHeader title={task.title} subtitle={`${task.groupName} · son teslim ${formatDate(task.dueDate)}${left >= 0 ? ` (${left} gün kaldı)` : ' (süre doldu)'}`} actions={<StatusBadge badge={taskStatus[task.status]} />} />
      <ErrorAlert message={action.error} onClose={() => action.setError(null)} />

      {task.description && (
        <div className="card mb-3">
          <div className="card-body text-pre-wrap">{task.description}</div>
        </div>
      )}

      <div className="row g-3">
        <div className="col-lg-7">
          <div className="card">
            <div className="card-header bg-white fw-semibold">Teslim geçmişi</div>
            <ul className="list-group list-group-flush">
              {submissions.length === 0 && <li className="list-group-item text-secondary">Henüz teslim yok.</li>}
              {submissions.map((s) => (
                <SubmissionRow key={s.id} s={s} isLatest={s.id === latest?.id} onDownload={() => action.run(() => taskApi.download(s.id, s.fileName))} />
              ))}
            </ul>
          </div>
        </div>

        <div className="col-lg-5">
          {isStudent && (
            <UploadCard
              disabled={task.status === 'ONAYLANDI'}
              onUpload={async (file) => {
                const updated = await action.run(() => taskApi.submit(task.id, file))
                if (updated) setData(updated)
                return Boolean(updated)
              }}
              busy={action.busy}
              late={left < 0}
            />
          )}
          {isAdvisor && latest && !latest.feedback && (
            <ReviewCard
              submission={latest}
              busy={action.busy}
              onReview={async (decision, comment) => {
                const updated = await action.run(() => taskApi.feedback(latest.id, decision, comment))
                if (updated) setData(updated)
              }}
            />
          )}
          {isAdvisor && (!latest || latest.feedback) && (
            <div className="card">
              <div className="card-body text-secondary small">
                {latest ? 'Son teslim değerlendirildi. Yeni teslim gelince burada değerlendirebilirsin.' : 'Grup henüz teslim yapmadı.'}
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

function SubmissionRow({ s, isLatest, onDownload }: { s: Submission; isLatest: boolean; onDownload: () => void }) {
  return (
    <li className="list-group-item">
      <div className="d-flex flex-wrap justify-content-between gap-2">
        <div>
          <span className="fw-semibold">Sürüm {s.versionNo}</span>
          {isLatest && <span className="badge text-bg-light border ms-2">son</span>}
          {s.late && <span className="badge text-bg-danger ms-2">geç teslim</span>}
          <div className="small text-secondary">
            {formatDateTime(s.submittedAt)} · {s.submittedByName}
          </div>
        </div>
        <button className="btn btn-sm btn-outline-secondary" onClick={onDownload} title="İndir">
          <i className="bi bi-download me-1" />
          {s.fileName} <span className="text-secondary">({formatSize(s.fileSizeKb)})</span>
        </button>
      </div>
      {s.feedback && (
        <div className="mt-2 p-2 rounded bg-body-tertiary small">
          <StatusBadge badge={decisionLabels[s.feedback.decision]} /> <span className="text-secondary">{s.feedback.advisorName} · {formatDateTime(s.feedback.createdAt)}</span>
          {s.feedback.comment && <div className="mt-1 text-pre-wrap">{s.feedback.comment}</div>}
        </div>
      )}
    </li>
  )
}

/** Öğrenci: dosya seç ve yükle. Boyut/tür kontrolü önce tarayıcıda, sonra sunucuda yapılır. */
function UploadCard({ onUpload, busy, disabled, late }: { onUpload: (f: File) => Promise<boolean>; busy: boolean; disabled: boolean; late: boolean }) {
  const [file, setFile] = useState<File | null>(null)
  const [localError, setLocalError] = useState<string | null>(null)
  // useRef ile <input type="file"> elemanına doğrudan erişip yükleme sonrası temizliyoruz.
  const inputRef = useRef<HTMLInputElement>(null)

  function pick(e: ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0] ?? null
    setLocalError(null)
    if (f) {
      const ext = f.name.split('.').pop()?.toLowerCase() ?? ''
      if (!ALLOWED.includes(ext)) {
        setLocalError(`İzin verilen türler: ${ALLOWED.join(', ')}`)
        setFile(null)
        return
      }
      if (f.size > MAX_BYTES) {
        setLocalError('Dosya en fazla 20 MB olabilir.')
        setFile(null)
        return
      }
    }
    setFile(f)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!file) return
    if (await onUpload(file)) {
      setFile(null)
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  return (
    <div className="card">
      <div className="card-header bg-white fw-semibold">Teslim yap</div>
      <div className="card-body">
        {disabled ? (
          <p className="text-success mb-0">
            <i className="bi bi-check-circle me-1" />
            Bu görev onaylandı; yeni teslim yapılamaz.
          </p>
        ) : (
          <form onSubmit={submit}>
            {late && <div className="alert alert-warning small py-2">Son teslim tarihi geçti; yükleyeceğin dosya “geç teslim” olarak işaretlenir.</div>}
            <ErrorAlert message={localError} />
            <input ref={inputRef} type="file" className="form-control mb-2" accept=".pdf,.doc,.docx,.zip,.pptx" onChange={pick} />
            <div className="form-text mb-3">PDF, DOC, DOCX, ZIP veya PPTX · en fazla 20 MB. Önceki teslimlerin silinmez.</div>
            <button className="btn btn-primary w-100" disabled={busy || !file}>
              {busy ? <span className="spinner-border spinner-border-sm me-2" /> : <i className="bi bi-upload me-2" />}
              Yükle
            </button>
          </form>
        )}
      </div>
    </div>
  )
}

/** Danışman: onayla / düzeltme iste. */
function ReviewCard({ submission, busy, onReview }: { submission: Submission; busy: boolean; onReview: (d: FeedbackDecision, c: string) => void }) {
  const [comment, setComment] = useState('')
  return (
    <div className="card">
      <div className="card-header bg-white fw-semibold">Sürüm {submission.versionNo} değerlendirmesi</div>
      <div className="card-body">
        <textarea className="form-control mb-2" rows={4} placeholder="Geri bildirim (düzeltme isteğinde zorunlu)" value={comment} onChange={(e) => setComment(e.target.value)} />
        <div className="d-flex gap-2">
          <button className="btn btn-success flex-grow-1" disabled={busy} onClick={() => onReview('ONAY', comment)}>
            <i className="bi bi-check-lg me-1" />
            Onayla
          </button>
          <button className="btn btn-warning flex-grow-1" disabled={busy || !comment.trim()} onClick={() => onReview('DUZELTME', comment)} title={comment.trim() ? '' : 'Düzeltme için açıklama yaz'}>
            <i className="bi bi-arrow-counterclockwise me-1" />
            Düzeltme iste
          </button>
        </div>
      </div>
    </div>
  )
}
