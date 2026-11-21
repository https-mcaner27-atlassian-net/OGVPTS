/**
 * Danışman: müsait saatlerim.
 * Öğrenciler randevu talebini yalnızca buradaki boş saatlerden seçebilir (FR-02).
 *
 * Kolaylık: "Tekrarla" alanıyla aynı saati sonraki haftalara da ekleyebilir,
 * "kaç oturum" alanıyla art arda oturumlar (ör. 10:00, 10:30, 11:00) oluşturabilirsin.
 */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { slotApi } from '../../api/endpoints'
import type { Slot } from '../../api/types'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import StatusBadge from '../../components/StatusBadge'
import { formatTime, formatWeekday, meetingStatus, todayIso } from '../../utils/format'

/** "2026-11-25" + "10:00" + dakika ekle → "2026-11-25T10:30" (backend LocalDateTime biçimi) */
function addMinutes(date: string, time: string, minutes: number): string {
  const [y, m, d] = date.split('-').map(Number)
  const [hh, mm] = time.split(':').map(Number)
  const t = new Date(y, m - 1, d, hh, mm + minutes)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}T${pad(t.getHours())}:${pad(t.getMinutes())}`
}

export default function SlotsPage() {
  const slots = useApi(() => slotApi.mine(), [])
  const action = useAction()
  const [date, setDate] = useState(todayIso(1))
  const [time, setTime] = useState('10:00')
  const [duration, setDuration] = useState(30)
  const [count, setCount] = useState(1)
  const [weeks, setWeeks] = useState(1)
  const [result, setResult] = useState<string | null>(null)

  async function add(e: FormEvent) {
    e.preventDefault()
    setResult(null)
    let ok = 0
    const errors: string[] = []
    // İki iç içe döngü: her hafta için, art arda "count" oturum
    for (let w = 0; w < weeks; w++) {
      for (let i = 0; i < count; i++) {
        const offset = w * 7 * 24 * 60 + i * duration
        const start = addMinutes(date, time, offset)
        const end = addMinutes(date, time, offset + duration)
        const created = await action.run(() => slotApi.create(start, end))
        if (created) ok++
        else errors.push(start.replace('T', ' '))
      }
    }
    setResult(`${ok} saat eklendi.${errors.length ? ` Eklenemeyen: ${errors.join(', ')}` : ''}`)
    void slots.reload()
  }

  async function remove(s: Slot) {
    if (!window.confirm('Bu saati silmek istediğine emin misin?')) return
    const done = await action.run(async () => {
      await slotApi.remove(s.id)
      return true
    })
    if (done) void slots.reload()
  }

  // Güne göre grupla
  const byDay = new Map<string, Slot[]>()
  for (const s of slots.data ?? []) {
    const key = s.start.substring(0, 10)
    byDay.set(key, [...(byDay.get(key) ?? []), s])
  }

  return (
    <>
      <PageHeader title="Müsaitlik" subtitle="Öğrencilerin randevu talep edebileceği saatler" />

      <div className="card mb-4">
        <div className="card-header bg-white fw-semibold">Saat ekle</div>
        <div className="card-body">
          <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
          {result && <div className="alert alert-info py-2">{result}</div>}
          <form onSubmit={add} className="row g-3 align-items-end">
            <div className="col-6 col-md-3">
              <label className="form-label" htmlFor="sdate">Tarih</label>
              <input id="sdate" type="date" className="form-control" min={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} required />
            </div>
            <div className="col-6 col-md-2">
              <label className="form-label" htmlFor="stime">Başlangıç</label>
              <input id="stime" type="time" className="form-control" step={300} value={time} onChange={(e) => setTime(e.target.value)} required />
            </div>
            <div className="col-4 col-md-2">
              <label className="form-label" htmlFor="sdur">Süre (dk)</label>
              <select id="sdur" className="form-select" value={duration} onChange={(e) => setDuration(Number(e.target.value))}>
                {[15, 20, 30, 45, 60, 90].map((d) => (
                  <option key={d} value={d}>{d}</option>
                ))}
              </select>
            </div>
            <div className="col-4 col-md-2">
              <label className="form-label" htmlFor="scount">Art arda</label>
              <input id="scount" type="number" min={1} max={8} className="form-control" value={count} onChange={(e) => setCount(Math.max(1, Math.min(8, Number(e.target.value))))} />
            </div>
            <div className="col-4 col-md-1">
              <label className="form-label" htmlFor="sweeks">Hafta</label>
              <input id="sweeks" type="number" min={1} max={12} className="form-control" value={weeks} onChange={(e) => setWeeks(Math.max(1, Math.min(12, Number(e.target.value))))} />
            </div>
            <div className="col-md-2 d-grid">
              <button className="btn btn-primary" disabled={action.busy}>
                <i className="bi bi-plus-lg me-1" />
                Ekle ({count * weeks})
              </button>
            </div>
          </form>
        </div>
      </div>

      {slots.loading && <Loading />}
      <ErrorAlert message={slots.error} />
      {slots.data?.length === 0 && <EmptyState icon="calendar-plus" title="Henüz müsaitlik eklemedin" />}

      <div className="row g-3">
        {[...byDay.entries()].map(([day, list]) => (
          <div className="col-md-6 col-xl-4" key={day}>
            <div className="card h-100">
              <div className="card-header bg-white fw-semibold text-capitalize">{formatWeekday(list[0].start)}</div>
              <ul className="list-group list-group-flush">
                {list.map((s) => (
                  <li key={s.id} className="list-group-item d-flex justify-content-between align-items-center gap-2">
                    <span>
                      {formatTime(s.start)} – {formatTime(s.end)}
                    </span>
                    {s.meetingStatus ? (
                      <span className="text-end small">
                        <StatusBadge badge={meetingStatus[s.meetingStatus]} />
                        <div className="text-secondary">{s.groupName}</div>
                      </span>
                    ) : (
                      <span className="d-flex align-items-center gap-2">
                        <span className="badge text-bg-light border">boş</span>
                        <button className="btn btn-sm btn-outline-danger" title="Sil" disabled={action.busy} onClick={() => remove(s)}>
                          <i className="bi bi-trash" />
                        </button>
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
