/** Danışman: sorumlu olduğum grupların projeleri (FR-07) ve projesi olmayan gruplar. */
import { Link } from 'react-router-dom'
import { groupApi, projectApi } from '../../api/endpoints'
import { useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import StatusBadge from '../../components/StatusBadge'
import { daysUntil, formatDate, projectStatus } from '../../utils/format'

export default function ProjectsPage() {
  const { data, loading, error } = useApi(async () => {
    const [projects, groups] = await Promise.all([projectApi.list(), groupApi.advisorGroups()])
    return { projects, groups }
  }, [])

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const groupsWithoutProject = data.groups.filter((g) => !g.projectId)

  return (
    <>
      <PageHeader title="Projeler" subtitle={`Sorumlu olduğun ${data.groups.length} grup`} />
      {data.projects.length === 0 && <EmptyState icon="kanban" title="Henüz proje yok" />}
      <div className="row g-3">
        {data.projects.map((p) => {
          const left = p.nextDueDate ? daysUntil(p.nextDueDate) : null
          return (
            <div className="col-md-6 col-xl-4" key={p.id}>
              <Link to={`/projeler/${p.id}`} className="card h-100 text-decoration-none text-body">
                <div className="card-body">
                  <div className="d-flex justify-content-between gap-2 mb-1">
                    <span className="small text-secondary">{p.groupName}</span>
                    <StatusBadge badge={projectStatus[p.status]} />
                  </div>
                  <h2 className="h6">{p.title}</h2>
                  <div className="progress my-3" style={{ height: 8 }}>
                    <div className="progress-bar bg-success" style={{ width: `${p.progressPercent}%` }} />
                  </div>
                  <div className={`small ${left !== null && left < 0 ? 'text-danger' : 'text-secondary'}`}>
                    {p.nextDueDate ? `Sıradaki aşama: ${formatDate(p.nextDueDate)}` : 'Tüm aşamalar tamamlandı'}
                  </div>
                </div>
              </Link>
            </div>
          )
        })}
      </div>

      {groupsWithoutProject.length > 0 && (
        <div className="mt-4">
          <h2 className="h6 text-secondary">Henüz proje tanımlamamış gruplar</h2>
          <ul className="list-inline">
            {groupsWithoutProject.map((g) => (
              <li key={g.id} className="list-inline-item badge text-bg-light border fw-normal p-2">
                {g.name} ({g.members.map((m) => m.fullName).join(', ')})
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  )
}
