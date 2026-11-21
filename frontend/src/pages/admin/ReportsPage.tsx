/**
 * Yönetici: dönem raporu (FR-11).
 * - Danışman başına görüşme sayısı (duruma göre dağılım)
 * - Proje durum dağılımı
 * - Excel indirme; PDF için tarayıcının "Yazdır → PDF olarak kaydet" özelliği (menü gizlenir)
 */
import { useState } from 'react'
import { adminApi } from '../../api/endpoints'
import type { MeetingStatus, ProjectStatus } from '../../api/types'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import { formatDate, meetingStatus, projectStatus, todayIso } from '../../utils/format'

/** Varsayılan dönem: içinde bulunduğumuz akademik dönemin başı (Eylül veya Şubat) → bugün */
function termStart(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  if (m >= 9) return `${y}-09-01`
  if (m >= 2) return `${y}-02-01`
  return `${y - 1}-09-01`
}

const MEETING_COLUMNS: MeetingStatus[] = ['BEKLEMEDE', 'YENI_SAAT_ONERILDI', 'ONAYLANDI', 'TAMAMLANDI', 'REDDEDILDI', 'IPTAL']
const PROJECT_ROWS: ProjectStatus[] = ['BASLAMADI', 'DEVAM_EDIYOR', 'TAMAMLANDI']

export default function ReportsPage() {
  const [from, setFrom] = useState(termStart())
  const [to, setTo] = useState(todayIso())
  // "Raporla" düğmesine basınca bu değerler güncellenir ve useApi raporu yeniden çeker
  const [range, setRange] = useState({ from, to })
  const report = useApi(() => adminApi.report(range.from, range.to), [range.from, range.to])
  const action = useAction()

  const r = report.data
  return (
    <>
      <PageHeader
        title="Raporlar"
        subtitle={r ? `${formatDate(r.from)} – ${formatDate(r.to)}` : undefined}
        actions={
          <div className="d-flex gap-2 no-print">
            <button className="btn btn-outline-success" disabled={action.busy || !r} onClick={() => action.run(() => adminApi.downloadReport(range.from, range.to))}>
              <i className="bi bi-file-earmark-excel me-1" />
              Excel
            </button>
            <button className="btn btn-outline-secondary" disabled={!r} onClick={() => window.print()}>
              <i className="bi bi-printer me-1" />
              Yazdır / PDF
            </button>
          </div>
        }
      />

      <form
        className="row g-2 align-items-end mb-4 no-print"
        onSubmit={(e) => {
          e.preventDefault()
          setRange({ from, to })
        }}
      >
        <div className="col-6 col-md-3">
          <label className="form-label" htmlFor="rfrom">Başlangıç</label>
          <input id="rfrom" type="date" className="form-control" value={from} onChange={(e) => setFrom(e.target.value)} required />
        </div>
        <div className="col-6 col-md-3">
          <label className="form-label" htmlFor="rto">Bitiş</label>
          <input id="rto" type="date" className="form-control" value={to} min={from} onChange={(e) => setTo(e.target.value)} required />
        </div>
        <div className="col-md-2 d-grid">
          <button className="btn btn-primary">Raporla</button>
        </div>
      </form>

      <ErrorAlert message={report.error ?? action.error} />
      {report.loading && <Loading />}

      {r && !report.loading && (
        <div className="row g-3">
          <div className="col-xl-8">
            <div className="card h-100">
              <div className="card-header bg-white fw-semibold">Danışman başına görüşmeler (toplam {r.totalMeetings})</div>
              <div className="table-responsive">
                <table className="table table-sm align-middle mb-0">
                  <thead>
                    <tr>
                      <th>Danışman</th>
                      <th className="text-end">Toplam</th>
                      {MEETING_COLUMNS.map((s) => (
                        <th key={s} className="text-end small">
                          {meetingStatus[s].label}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {r.meetingsPerAdvisor.length === 0 && (
                      <tr>
                        <td colSpan={2 + MEETING_COLUMNS.length} className="text-secondary">
                          Bu dönemde görüşme yok.
                        </td>
                      </tr>
                    )}
                    {r.meetingsPerAdvisor.map((row) => (
                      <tr key={row.advisorId}>
                        <td>{row.advisorName}</td>
                        <td className="text-end fw-semibold">{row.total}</td>
                        {MEETING_COLUMNS.map((s) => (
                          <td key={s} className="text-end">
                            {row.byStatus[s] ?? 0}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div className="col-xl-4">
            <div className="card h-100">
              <div className="card-header bg-white fw-semibold">Proje durum dağılımı (toplam {r.totalProjects})</div>
              <div className="card-body">
                {PROJECT_ROWS.map((s) => {
                  const count = r.projectStatus[s] ?? 0
                  const pct = r.totalProjects ? Math.round((count / r.totalProjects) * 100) : 0
                  return (
                    <div key={s} className="mb-3">
                      <div className="d-flex justify-content-between small mb-1">
                        <span>{projectStatus[s].label}</span>
                        <span className="fw-semibold">
                          {count} <span className="text-secondary fw-normal">(%{pct})</span>
                        </span>
                      </div>
                      <div className="progress" style={{ height: 10 }}>
                        <div className={`progress-bar bg-${projectStatus[s].color}`} style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
