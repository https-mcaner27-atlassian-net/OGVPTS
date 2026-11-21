/** Danışman / yönetici: bir projenin ayrıntısı (FR-07). Danışman yorum yazabilir. */
import { Link, useParams } from 'react-router-dom'
import { projectApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { useApi } from '../../hooks/useApi'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import ProjectView from '../../components/ProjectView'

export default function ProjectDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const { data, setData, loading, error } = useApi(() => projectApi.get(Number(id)), [id])

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  return (
    <>
      <nav className="small mb-3">
        <Link to={user?.role === 'YONETICI' ? '/yonetim/eslestirme' : '/projeler'}>← Geri</Link>
      </nav>
      <ProjectView project={data} onChange={setData} canComment={user?.role === 'DANISMAN'} />
    </>
  )
}
