/**
 * Yönetici: grup – danışman eşleştirmesi (FR-10, BR-01).
 *
 * Kontenjan doluysa sunucu 409 + "ADVISOR_QUOTA_FULL" kodu döner. Bu durumda kullanıcıya
 * uyarıyı gösterip onay isteriz; onaylarsa aynı isteği force=true ile tekrar göndeririz.
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { adminApi } from '../../api/endpoints'
import { ApiError } from '../../api/client'
import type { Group } from '../../api/types'
import { useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import StatusBadge from '../../components/StatusBadge'
import { projectStatus } from '../../utils/format'

export default function AssignmentsPage() {
  const { data, setData, loading, error, reload } = useApi(async () => {
    const [groups, advisors] = await Promise.all([adminApi.groups(), adminApi.advisors()])
    return { groups, advisors }
  }, [])
  const [busyGroup, setBusyGroup] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [onlyUnassigned, setOnlyUnassigned] = useState(false)

  async function assign(group: Group, advisorId: number, force = false) {
    setBusyGroup(group.id)
    setActionError(null)
    try {
      const updated = await adminApi.assignAdvisor(group.id, advisorId, force)
      // Tabloda yalnızca bu satırı güncelle (tüm listeyi yeniden çekmeye gerek yok)
      setData((d) => (d ? { ...d, groups: d.groups.map((g) => (g.id === updated.id ? updated : g)) } : d))
      void reloadAdvisors()
    } catch (e) {
      if (e instanceof ApiError && e.code === 'ADVISOR_QUOTA_FULL') {
        if (window.confirm(`${e.message}`)) {
          await assign(group, advisorId, true)
          return
        }
      } else {
        setActionError(e instanceof ApiError ? e.message : 'Atama yapılamadı.')
      }
    } finally {
      setBusyGroup(null)
    }
  }

  async function reloadAdvisors() {
    const advisors = await adminApi.advisors()
    setData((d) => (d ? { ...d, advisors } : d))
  }

  if (loading) return <Loading />
  if (error || !data) return <ErrorAlert message={error} />

  const groups = onlyUnassigned ? data.groups.filter((g) => !g.advisor) : data.groups

  return (
    <>
      <PageHeader
        title="Grup – danışman eşleştirme"
        subtitle="Her grubun tek bir sorumlu danışmanı olur; öğrenciler yalnızca bu danışmandan randevu alabilir."
        actions={
          <button className="btn btn-outline-secondary" onClick={() => void reload()}>
            <i className="bi bi-arrow-clockwise" />
          </button>
        }
      />
      <ErrorAlert message={actionError} onClose={() => setActionError(null)} />

      <div className="d-flex flex-wrap gap-2 mb-3">
        {data.advisors.map((a) => (
          <span key={a.advisor.id} className={`badge border fw-normal p-2 ${a.groupCount >= a.maxGroups ? 'text-bg-danger' : 'text-bg-light'}`}>
            {a.advisor.fullName}: {a.groupCount}/{a.maxGroups}
          </span>
        ))}
      </div>

      <div className="form-check form-switch mb-3">
        <input className="form-check-input" type="checkbox" id="onlyUn" checked={onlyUnassigned} onChange={(e) => setOnlyUnassigned(e.target.checked)} />
        <label className="form-check-label" htmlFor="onlyUn">
          Yalnızca danışmanı olmayan gruplar
        </label>
      </div>

      {groups.length === 0 ? (
        <EmptyState icon="people" title="Grup yok" />
      ) : (
        <div className="card">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
              <thead>
                <tr>
                  <th>Grup</th>
                  <th>Öğrenciler</th>
                  <th>Proje</th>
                  <th style={{ minWidth: 220 }}>Danışman</th>
                </tr>
              </thead>
              <tbody>
                {groups.map((g) => (
                  <tr key={g.id} className={g.advisor ? '' : 'table-warning'}>
                    <td className="fw-semibold">{g.name}</td>
                    <td className="small">{g.members.map((m) => m.fullName).join(', ')}</td>
                    <td className="small">
                      {g.projectId ? (
                        <>
                          <Link to={`/projeler/${g.projectId}`}>{g.projectTitle}</Link>{' '}
                          {g.projectStatus && <StatusBadge badge={projectStatus[g.projectStatus]} />}
                        </>
                      ) : (
                        <span className="text-secondary">—</span>
                      )}
                    </td>
                    <td>
                      <select
                        className="form-select form-select-sm"
                        value={g.advisor?.id ?? ''}
                        disabled={busyGroup === g.id}
                        onChange={(e) => e.target.value && assign(g, Number(e.target.value))}
                        aria-label={`${g.name} danışmanı`}
                      >
                        <option value="">Danışman seç…</option>
                        {data.advisors.map((a) => (
                          <option key={a.advisor.id} value={a.advisor.id}>
                            {a.advisor.fullName} ({a.groupCount}/{a.maxGroups})
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}
