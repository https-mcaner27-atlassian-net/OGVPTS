/**
 * Ana panel: kullanıcının rolüne göre farklı bir özet gösterir.
 * Bu bileşen yalnızca "hangi paneli çizeceğine" karar verir; asıl içerik alt bileşenlerde.
 */
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useApi } from '../hooks/useApi'
import { adminApi, meetingApi, projectApi, taskApi } from '../api/endpoints'
import type { Meeting, Task } from '../api/types'
import PageHeader from '../components/PageHeader'
import Loading from '../components/Loading'
import ErrorAlert from '../components/ErrorAlert'
import StatusBadge from '../components/StatusBadge'
import EmptyState from '../components/EmptyState'
import { daysUntil, formatDate, formatDateTime, meetingStatus, projectStatus, taskStatus } from '../utils/format'

export default function DashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <>
      <PageHeader title={`Merhaba, ${user.fullName}`} subtitle={subtitleFor(user.role)} />
      {user.role === 'OGRENCI' && <StudentDashboard />}
      {user.role === 'DANISMAN' && <AdvisorDashboard />}
      {user.role === 'YONETICI' && <AdminDashboard />}
    </>
  )
}

function subtitleFor(role: string) {
  if (role === 'OGRENCI') return 'Grubunun projesi, randevuların ve görevlerin burada.'
  if (role === 'DANISMAN') return 'Bekleyen talepler, yaklaşan görüşmeler ve değerlendirilecek teslimler.'
  return 'Eşleştirme durumu ve sistem özeti.'
}

/** Küçük sayı kartı. Aynı görünümü birçok yerde kullanmak için ayrı bileşen yaptık. */
function StatCard({ icon, label, value, to, color = 'primary' }: { icon: string; label: string; value: number | string; to: string; color?: string }) {
  return (
    <div className="col-6 col-lg-3">
      <Link to={to} className="card stat-card h-100 text-decoration-none">
        <div className="card-body">
          <div className={`text-${color} mb-2`}>
            <i className={`bi bi-${icon} fs-4`} />
          </div>
          <div className="stat-value text-body">{value}</div>
          <div className="text-secondary small">{label}</div>
        </div>
      </Link>
    </div>
  )
}

function upcoming(meetings: Meeting[]) {
  const now = Date.now()
  return meetings
    .filter((m) => m.status === 'ONAYLANDI' && new Date(m.start).getTime() > now)
    .sort((a, b) => a.start.localeCompare(b.start))
}

function MeetingList({ items }: { items: Meeting[] }) {
  if (items.length === 0) return <p className="text-secondary small mb-0">Yaklaşan görüşme yok.</p>
  return (
    <ul className="list-group list-group-flush">
      {items.slice(0, 5).map((m) => (
        <li key={m.id} className="list-group-item px-0 d-flex justify-content-between align-items-start gap-2">
          <div>
            <div className="fw-semibold">{m.topic}</div>
            <div className="small text-secondary">
              {formatDateTime(m.start)} · {m.groupName}
            </div>
          </div>
          <StatusBadge badge={meetingStatus[m.status]} />
        </li>
      ))}
    </ul>
  )
}

function TaskList({ items }: { items: Task[] }) {
  if (items.length === 0) return <p className="text-secondary small mb-0">Görev yok.</p>
  return (
    <ul className="list-group list-group-flush">
      {items.slice(0, 5).map((t) => {
        const left = daysUntil(t.dueDate)
        return (
          <li key={t.id} className="list-group-item px-0 d-flex justify-content-between align-items-start gap-2">
            <div>
              <Link to={`/gorevler/${t.id}`} className="fw-semibold text-decoration-none">
                {t.title}
              </Link>
              <div className={`small ${left < 0 ? 'text-danger' : left <= 2 ? 'text-warning-emphasis' : 'text-secondary'}`}>
                Son tarih {formatDate(t.dueDate)} {left >= 0 ? `(${left} gün)` : '(gecikti)'}
              </div>
            </div>
            <StatusBadge badge={taskStatus[t.status]} />
          </li>
        )
      })}
    </ul>
  )
}

// ------------------------------------------------------------------ öğrenci paneli
function StudentDashboard() {
  const { user } = useAuth()
  // Promise.all: üç isteği aynı anda gönderir, hepsi bitince devam eder.
  const { data, loading, error } = useApi(async () => {
    if (!user?.groupId) return null
    const [project, meetings, tasks] = await Promise.all([projectApi.mine(), meetingApi.list(), taskApi.list()])
    return { project, meetings, tasks }
  }, [user?.groupId])

  if (!user?.groupId) {
    return (
      <EmptyState icon="people" title="Henüz bir proje grubunda değilsin">
        Başlamak için <Link to="/grubum">grubunu oluştur</Link> ya da bir arkadaşının seni eklemesini iste.
      </EmptyState>
    )
  }
  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const openTasks = data.tasks.filter((t) => t.status !== 'ONAYLANDI')
  const pending = data.meetings.filter((m) => m.status === 'BEKLEMEDE' || m.status === 'YENI_SAAT_ONERILDI')

  return (
    <>
      {!user.advisorName && (
        <div className="alert alert-warning">
          <i className="bi bi-info-circle me-2" />
          Grubuna henüz danışman atanmadı. Atama yapılınca randevu talep edebileceksin.
        </div>
      )}
      <div className="row g-3 mb-4">
        <StatCard icon="kanban" label="Proje ilerlemesi" value={data.project ? `%${data.project.progressPercent}` : '—'} to="/projem" />
        <StatCard icon="calendar-check" label="Yaklaşan görüşme" value={upcoming(data.meetings).length} to="/randevular" color="success" />
        <StatCard icon="hourglass-split" label="Yanıt bekleyen talep" value={pending.length} to="/randevular" color="warning" />
        <StatCard icon="list-check" label="Açık görev" value={openTasks.length} to="/gorevler" color="danger" />
      </div>
      <div className="row g-3">
        <div className="col-lg-6">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">Yaklaşan görüşmeler</div>
            <div className="card-body">
              <MeetingList items={upcoming(data.meetings)} />
            </div>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">Açık görevler</div>
            <div className="card-body">
              <TaskList items={openTasks} />
            </div>
          </div>
        </div>
        {data.project && (
          <div className="col-12">
            <div className="card">
              <div className="card-body d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div>
                  <div className="small text-secondary">Projen</div>
                  <Link to="/projem" className="h5 text-decoration-none">
                    {data.project.title}
                  </Link>
                </div>
                <StatusBadge badge={projectStatus[data.project.status]} />
                <div className="flex-grow-1" style={{ minWidth: 200 }}>
                  <div className="progress" role="progressbar" aria-valuenow={data.project.progressPercent} aria-valuemin={0} aria-valuemax={100}>
                    <div className="progress-bar" style={{ width: `${data.project.progressPercent}%` }} />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  )
}

// ------------------------------------------------------------------ danışman paneli
function AdvisorDashboard() {
  const { data, loading, error } = useApi(async () => {
    const [meetings, tasks, projects] = await Promise.all([meetingApi.list(), taskApi.list(), projectApi.list()])
    return { meetings, tasks, projects }
  }, [])

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const pending = data.meetings.filter((m) => m.status === 'BEKLEMEDE')
  const toReview = data.tasks.filter((t) => t.status === 'TESLIM_EDILDI')

  return (
    <>
      <div className="row g-3 mb-4">
        <StatCard icon="inbox" label="Bekleyen randevu talebi" value={pending.length} to="/randevular" color="warning" />
        <StatCard icon="calendar-check" label="Yaklaşan görüşme" value={upcoming(data.meetings).length} to="/randevular" color="success" />
        <StatCard icon="file-earmark-check" label="Değerlendirilecek teslim" value={toReview.length} to="/gorevler" color="danger" />
        <StatCard icon="kanban" label="Takip edilen proje" value={data.projects.length} to="/projeler" />
      </div>
      <div className="row g-3">
        <div className="col-lg-6">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold d-flex justify-content-between">
              Bekleyen talepler <Link to="/randevular" className="small">Tümü</Link>
            </div>
            <div className="card-body">
              {pending.length === 0 ? <p className="text-secondary small mb-0">Bekleyen talep yok.</p> : <MeetingList items={pending} />}
            </div>
          </div>
        </div>
        <div className="col-lg-6">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">Değerlendirilecek teslimler</div>
            <div className="card-body">
              <TaskList items={toReview} />
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

// ------------------------------------------------------------------ yönetici paneli
function AdminDashboard() {
  const { data, loading, error } = useApi(async () => {
    const [groups, advisors, users] = await Promise.all([adminApi.groups(), adminApi.advisors(), adminApi.users()])
    return { groups, advisors, users }
  }, [])

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const unassigned = data.groups.filter((g) => !g.advisor)
  const students = data.users.filter((u) => u.role === 'OGRENCI')
  const withoutGroup = students.filter((s) => !s.groupName)

  return (
    <>
      <div className="row g-3 mb-4">
        <StatCard icon="people" label="Proje grubu" value={data.groups.length} to="/yonetim/eslestirme" />
        <StatCard icon="exclamation-circle" label="Danışmanı olmayan grup" value={unassigned.length} to="/yonetim/eslestirme" color="danger" />
        <StatCard icon="person-badge" label="Danışman" value={data.advisors.length} to="/yonetim/kullanicilar" color="success" />
        <StatCard icon="person" label="Grubu olmayan öğrenci" value={withoutGroup.length} to="/yonetim/kullanicilar" color="warning" />
      </div>
      <div className="card">
        <div className="card-header bg-white fw-semibold">Danışman kontenjanları</div>
        <div className="card-body">
          {data.advisors.map((a) => {
            const pct = Math.min(100, Math.round((a.groupCount / a.maxGroups) * 100))
            return (
              <div key={a.advisor.id} className="mb-3">
                <div className="d-flex justify-content-between small mb-1">
                  <span>{a.advisor.fullName}</span>
                  <span className="text-secondary">
                    {a.groupCount}/{a.maxGroups} grup
                  </span>
                </div>
                <div className="progress" style={{ height: 8 }}>
                  <div className={`progress-bar ${pct >= 100 ? 'bg-danger' : ''}`} style={{ width: `${pct}%` }} />
                </div>
              </div>
            )
          })}
          {data.advisors.length === 0 && <p className="text-secondary small mb-0">Henüz danışman yok.</p>}
        </div>
      </div>
    </>
  )
}
