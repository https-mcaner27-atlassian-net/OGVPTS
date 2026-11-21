/**
 * Öğrencinin proje grubu (FR-12).
 * - Grubu yoksa: grup oluşturma formu
 * - Grubu varsa: üyeler, danışman ve (yer varsa) arkadaş ekleme formu
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { groupApi } from '../../api/endpoints'
import { useApi, useAction } from '../../hooks/useApi'
import { useAuth } from '../../auth/AuthContext'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import StatusBadge from '../../components/StatusBadge'
import { projectStatus } from '../../utils/format'

const MAX_MEMBERS = 2

export default function GroupPage() {
  const { refresh } = useAuth()
  const { data: group, setData, loading, error } = useApi(() => groupApi.mine(), [])
  const action = useAction()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [info, setInfo] = useState<string | null>(null)

  async function createGroup(e: FormEvent) {
    e.preventDefault()
    const created = await action.run(() => groupApi.create(name))
    if (created) {
      setData(created) // sunucunun döndürdüğü yeni grubu ekrana koy
      await refresh() // üst menüdeki kullanıcı bilgisini (groupId) tazele
    }
  }

  async function addMember(e: FormEvent) {
    e.preventDefault()
    if (!group) return
    const updated = await action.run(() => groupApi.addMember(group.id, email))
    if (updated) {
      setData(updated)
      setEmail('')
      setInfo('Öğrenci gruba eklendi ve bilgilendirildi.')
    }
  }

  if (loading) return <Loading />
  if (error) return <ErrorAlert message={error} />

  // ---------------------------------------------------------- grubu yok
  if (!group) {
    return (
      <>
        <PageHeader title="Proje grubu" subtitle="Projeni en fazla iki kişilik bir grupla yürütürsün." />
        <div className="card" style={{ maxWidth: 560 }}>
          <div className="card-body">
            <h2 className="h5">Yeni grup oluştur</h2>
            <p className="text-secondary small">
              Grubu oluşturduktan sonra arkadaşını e-postasıyla ekleyebilirsin. Bir öğrenci yalnızca bir grupta yer alabilir.
            </p>
            <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
            <form onSubmit={createGroup} className="d-flex flex-column flex-sm-row gap-2">
              <input
                className="form-control"
                placeholder="Grup adı (ör. Akıllı Kampüs Ekibi)"
                value={name}
                onChange={(e) => setName(e.target.value)}
                maxLength={150}
                required
              />
              <button className="btn btn-primary text-nowrap" disabled={action.busy || !name.trim()}>
                <i className="bi bi-plus-lg me-1" />
                Oluştur
              </button>
            </form>
          </div>
        </div>
      </>
    )
  }

  // ---------------------------------------------------------- grubu var
  const hasRoom = group.members.length < MAX_MEMBERS
  return (
    <>
      <PageHeader title={group.name} subtitle="Proje grubun" />
      <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
      {info && (
        <div className="alert alert-success alert-dismissible">
          {info}
          <button type="button" className="btn-close" onClick={() => setInfo(null)} aria-label="Kapat" />
        </div>
      )}

      <div className="row g-3">
        <div className="col-lg-6">
          <div className="card h-100">
            <div className="card-header bg-white fw-semibold">
              Üyeler ({group.members.length}/{MAX_MEMBERS})
            </div>
            <ul className="list-group list-group-flush">
              {group.members.map((m) => (
                <li key={m.id} className="list-group-item d-flex align-items-center gap-2">
                  <i className="bi bi-person-circle fs-4 text-secondary" />
                  <div>
                    <div className="fw-semibold">{m.fullName}</div>
                    <div className="small text-secondary">{m.email}</div>
                  </div>
                </li>
              ))}
            </ul>
            {hasRoom && (
              <div className="card-body border-top">
                <form onSubmit={addMember} className="d-flex flex-column flex-sm-row gap-2">
                  <input
                    type="email"
                    className="form-control"
                    placeholder="Arkadaşının okul e-postası"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                  />
                  <button className="btn btn-outline-primary text-nowrap" disabled={action.busy || !email}>
                    <i className="bi bi-person-plus me-1" />
                    Ekle
                  </button>
                </form>
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="card mb-3">
            <div className="card-header bg-white fw-semibold">Danışman</div>
            <div className="card-body">
              {group.advisor ? (
                <>
                  <div className="fw-semibold">{group.advisor.fullName}</div>
                  <div className="small text-secondary">{group.advisor.email}</div>
                </>
              ) : (
                <p className="text-secondary mb-0">Henüz atanmadı. Danışman ataması yönetici tarafından yapılır.</p>
              )}
            </div>
          </div>
          <div className="card">
            <div className="card-header bg-white fw-semibold">Proje</div>
            <div className="card-body">
              {group.projectId ? (
                <div className="d-flex justify-content-between align-items-center gap-2">
                  <Link to="/projem" className="fw-semibold">
                    {group.projectTitle}
                  </Link>
                  {group.projectStatus && <StatusBadge badge={projectStatus[group.projectStatus]} />}
                </div>
              ) : (
                <p className="mb-0">
                  Henüz proje tanımlanmadı. <Link to="/projem">Projeyi oluştur</Link>
                </p>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
