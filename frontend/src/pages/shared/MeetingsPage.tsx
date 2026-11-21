/**
 * Randevular sayfası — rolüne göre iki farklı görünüm:
 *
 * Öğrenci (FR-02, FR-09):
 *   1) Danışmanın boş saatlerinden birini seç + konu yaz → talep gönder (durum: Beklemede)
 *   2) Görüşmelerim: danışman yeni saat önerdiyse kabul/ret, iptal, görüşme notunu okuma
 *
 * Danışman (FR-03, FR-04, FR-09):
 *   1) Bekleyen talepler: Onayla / Reddet / Yeni saat öner
 *   2) Görüşmelerim: yapıldı olarak işaretle, not ve kararları gir
 *
 * Durum geçişleri Sprint 2'deki "Randevu State Diagram"a göredir.
 */
import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { meetingApi, slotApi } from '../../api/endpoints'
import type { MeetingFilter } from '../../api/endpoints'
import type { Meeting, MeetingStatus, Slot } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import StatusBadge from '../../components/StatusBadge'
import Modal from '../../components/Modal'
import { formatDateTime, formatTime, formatWeekday, meetingStatus } from '../../utils/format'

export default function MeetingsPage() {
  const { user } = useAuth()
  if (user?.role === 'DANISMAN') return <AdvisorMeetings />
  return <StudentMeetings />
}

// ======================================================================= ortak parçalar

/** Saatleri güne göre grupla: { "Çarşamba 25 Kasım": [slot, slot], ... } */
function groupByDay(slots: Slot[]): [string, Slot[]][] {
  const map = new Map<string, Slot[]>()
  for (const s of slots) {
    const day = formatWeekday(s.start)
    map.set(day, [...(map.get(day) ?? []), s])
  }
  return [...map.entries()]
}

/** FR-09: durum ve tarih aralığı filtresi. */
function MeetingFilters({ value, onChange }: { value: MeetingFilter; onChange: (f: MeetingFilter) => void }) {
  const statuses = Object.keys(meetingStatus) as MeetingStatus[]
  return (
    <div className="row g-2 mb-3">
      <div className="col-sm-4">
        <select className="form-select" value={value.status ?? ''} onChange={(e) => onChange({ ...value, status: e.target.value as MeetingStatus | '' })} aria-label="Durum">
          <option value="">Tüm durumlar</option>
          {statuses.map((s) => (
            <option key={s} value={s}>
              {meetingStatus[s].label}
            </option>
          ))}
        </select>
      </div>
      <div className="col-6 col-sm-3">
        <input type="date" className="form-control" value={value.from ?? ''} onChange={(e) => onChange({ ...value, from: e.target.value })} aria-label="Başlangıç tarihi" />
      </div>
      <div className="col-6 col-sm-3">
        <input type="date" className="form-control" value={value.to ?? ''} onChange={(e) => onChange({ ...value, to: e.target.value })} aria-label="Bitiş tarihi" />
      </div>
      <div className="col-sm-2 d-grid">
        <button className="btn btn-outline-secondary" onClick={() => onChange({})}>
          Temizle
        </button>
      </div>
    </div>
  )
}

/** Tek bir görüşmenin kartı. "actions": role göre değişen düğmeler (children gibi dışarıdan verilir). */
function MeetingCard({ m, actions, showGroup }: { m: Meeting; actions?: ReactNode; showGroup?: boolean }) {
  return (
    <div className="card mb-2">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between gap-2">
          <div>
            <div className="fw-semibold">{m.topic}</div>
            <div className="small text-secondary">
              <i className="bi bi-clock me-1" />
              {formatDateTime(m.start)} – {formatTime(m.end)}
              {showGroup ? ` · ${m.groupName}` : ` · ${m.advisorName}`}
              {' · talep eden: '}
              {m.requestedByName}
            </div>
          </div>
          <div>
            <StatusBadge badge={meetingStatus[m.status]} />
          </div>
        </div>
        {m.note && (
          <div className="mt-3 p-3 bg-body-tertiary rounded">
            <div className="small fw-semibold mb-1">
              <i className="bi bi-journal-text me-1" />
              Görüşme notu ({m.note.advisorName})
            </div>
            <div className="text-pre-wrap small">{m.note.note}</div>
            {m.note.decisions && (
              <>
                <div className="small fw-semibold mt-2 mb-1">Alınan kararlar</div>
                <div className="text-pre-wrap small">{m.note.decisions}</div>
              </>
            )}
          </div>
        )}
        {actions && <div className="d-flex flex-wrap gap-2 mt-3">{actions}</div>}
      </div>
    </div>
  )
}

// ======================================================================= öğrenci

function StudentMeetings() {
  const { user } = useAuth()
  const [filter, setFilter] = useState<MeetingFilter>({})
  // filter değişince liste yeniden yüklenir (bağımlılık listesinde)
  const meetings = useApi(() => meetingApi.list(filter), [filter.status, filter.from, filter.to])
  const slots = useApi(() => (user?.advisorName ? slotApi.available() : Promise.resolve([])), [user?.advisorName])
  const action = useAction()
  const [selectedSlot, setSelectedSlot] = useState<number | null>(null)
  const [topic, setTopic] = useState('')
  const [success, setSuccess] = useState<string | null>(null)

  if (!user?.groupId) {
    return (
      <EmptyState icon="people" title="Randevu almak için önce bir proje grubunda olmalısın">
        <Link to="/grubum">Grubunu oluştur</Link>
      </EmptyState>
    )
  }

  async function requestMeeting(e: FormEvent) {
    e.preventDefault()
    if (selectedSlot === null) return
    const created = await action.run(() => meetingApi.request(selectedSlot, topic))
    if (created) {
      setSuccess('Talebin danışmanına iletildi. Durum: Beklemede.')
      setSelectedSlot(null)
      setTopic('')
      void meetings.reload()
      void slots.reload()
    }
  }

  /** Bir işlemi çalıştır, sonra listeleri tazele. */
  async function act(fn: () => Promise<Meeting>, confirmText?: string) {
    if (confirmText && !window.confirm(confirmText)) return
    const result = await action.run(fn)
    if (result) {
      void meetings.reload()
      void slots.reload()
    }
  }

  return (
    <>
      <PageHeader title="Randevular" subtitle={user.advisorName ? `Danışmanın: ${user.advisorName}` : undefined} />
      <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
      {success && (
        <div className="alert alert-success alert-dismissible">
          {success}
          <button type="button" className="btn-close" onClick={() => setSuccess(null)} aria-label="Kapat" />
        </div>
      )}

      <div className="row g-4">
        <div className="col-lg-5">
          <div className="card">
            <div className="card-header bg-white fw-semibold">Randevu talep et</div>
            <div className="card-body">
              {!user.advisorName && <p className="text-secondary mb-0">Grubuna danışman atanınca boş saatler burada görünecek.</p>}
              {slots.loading && user.advisorName && <Loading />}
              <ErrorAlert message={slots.error} />
              {slots.data && slots.data.length === 0 && user.advisorName && (
                <p className="text-secondary mb-0">Danışmanının şu an boş saati yok. Daha sonra tekrar bak.</p>
              )}
              {slots.data && slots.data.length > 0 && (
                <form onSubmit={requestMeeting}>
                  <p className="small text-secondary">1) Bir saat seç</p>
                  {groupByDay(slots.data).map(([day, list]) => (
                    <div key={day} className="mb-3">
                      <div className="small fw-semibold mb-1 text-capitalize">{day}</div>
                      <div className="d-flex flex-wrap gap-2">
                        {list.map((s) => (
                          <button
                            type="button"
                            key={s.id}
                            className={`btn btn-sm slot-btn ${selectedSlot === s.id ? 'btn-primary' : 'btn-outline-primary'}`}
                            onClick={() => setSelectedSlot(s.id)}
                            aria-pressed={selectedSlot === s.id}
                          >
                            {formatTime(s.start)} – {formatTime(s.end)}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                  <label className="small text-secondary mb-1" htmlFor="topic">
                    2) Görüşme konusu
                  </label>
                  <textarea id="topic" className="form-control mb-3" rows={2} maxLength={300} value={topic} onChange={(e) => setTopic(e.target.value)} placeholder="ör. Veritabanı tasarımı hakkında geri bildirim" required />
                  <button className="btn btn-primary w-100" disabled={action.busy || selectedSlot === null || !topic.trim()}>
                    <i className="bi bi-send me-1" />
                    Talep gönder
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>

        <div className="col-lg-7">
          <h2 className="h5">Görüşmelerim</h2>
          <MeetingFilters value={filter} onChange={setFilter} />
          {meetings.loading && <Loading />}
          <ErrorAlert message={meetings.error} />
          {meetings.data?.length === 0 && <EmptyState icon="calendar" title="Görüşme bulunamadı" />}
          {meetings.data?.map((m) => (
            <MeetingCard
              key={m.id}
              m={m}
              actions={
                <>
                  {m.status === 'YENI_SAAT_ONERILDI' && (
                    <>
                      <span className="small text-info-emphasis align-self-center me-1">Danışmanın bu saati önerdi:</span>
                      <button className="btn btn-sm btn-success" disabled={action.busy} onClick={() => act(() => meetingApi.acceptProposal(m.id))}>
                        <i className="bi bi-check-lg me-1" />
                        Kabul et
                      </button>
                      <button className="btn btn-sm btn-outline-danger" disabled={action.busy} onClick={() => act(() => meetingApi.declineProposal(m.id), 'Önerilen saati reddetmek istediğine emin misin?')}>
                        Reddet
                      </button>
                    </>
                  )}
                  {m.canCancel && (
                    <button className="btn btn-sm btn-outline-secondary" disabled={action.busy} onClick={() => act(() => meetingApi.cancel(m.id), 'Bu görüşmeyi iptal etmek istediğine emin misin?')}>
                      İptal et
                    </button>
                  )}
                  {m.status === 'ONAYLANDI' && !m.canCancel && <span className="small text-secondary">24 saatten az kaldığı için iptal edilemez (BR-03).</span>}
                </>
              }
            />
          ))}
        </div>
      </div>
    </>
  )
}

// ======================================================================= danışman

function AdvisorMeetings() {
  const [filter, setFilter] = useState<MeetingFilter>({})
  const meetings = useApi(() => meetingApi.list(filter), [filter.status, filter.from, filter.to])
  const pendingReq = useApi(() => meetingApi.list({ status: 'BEKLEMEDE' }), [])
  const action = useAction()

  // Yeni saat önerme penceresi için seçili görüşme ve boş saatler
  const [proposeFor, setProposeFor] = useState<Meeting | null>(null)
  const [freeSlots, setFreeSlots] = useState<Slot[] | null>(null)
  // Not girme penceresi
  const [noteFor, setNoteFor] = useState<Meeting | null>(null)

  function reloadAll() {
    void meetings.reload()
    void pendingReq.reload()
  }

  async function act(fn: () => Promise<Meeting>, confirmText?: string) {
    if (confirmText && !window.confirm(confirmText)) return
    if (await action.run(fn)) reloadAll()
  }

  async function openPropose(m: Meeting) {
    setProposeFor(m)
    setFreeSlots(null)
    const slots = await action.run(() => slotApi.myFree())
    setFreeSlots(slots ?? [])
  }

  async function propose(slotId: number) {
    if (!proposeFor) return
    if (await action.run(() => meetingApi.propose(proposeFor.id, slotId))) {
      setProposeFor(null)
      reloadAll()
    }
  }

  // Şu anki zamanı render sırasında değil, bileşen ilk açıldığında bir kez alıyoruz.
  // (Render fonksiyonu "saf" olmalı: aynı girdiyle hep aynı çıktıyı üretmeli.)
  const [now] = useState(() => Date.now())

  return (
    <>
      <PageHeader title="Randevular" actions={<Link to="/musaitlik" className="btn btn-outline-primary"><i className="bi bi-calendar-plus me-1" />Müsaitlik ekle</Link>} />
      <ErrorAlert message={action.error} onClose={() => action.setError(null)} />

      <h2 className="h5">Bekleyen talepler</h2>
      {pendingReq.loading && <Loading />}
      {pendingReq.data?.length === 0 && <p className="text-secondary">Bekleyen talep yok.</p>}
      {pendingReq.data?.map((m) => (
        <MeetingCard
          key={m.id}
          m={m}
          showGroup
          actions={
            <>
              <button className="btn btn-sm btn-success" disabled={action.busy} onClick={() => act(() => meetingApi.approve(m.id))}>
                <i className="bi bi-check-lg me-1" />
                Onayla
              </button>
              <button className="btn btn-sm btn-outline-danger" disabled={action.busy} onClick={() => act(() => meetingApi.reject(m.id), 'Talebi reddetmek istediğine emin misin?')}>
                <i className="bi bi-x-lg me-1" />
                Reddet
              </button>
              <button className="btn btn-sm btn-outline-primary" disabled={action.busy} onClick={() => openPropose(m)}>
                <i className="bi bi-arrow-repeat me-1" />
                Yeni saat öner
              </button>
            </>
          }
        />
      ))}

      <h2 className="h5 mt-4">Tüm görüşmeler</h2>
      <MeetingFilters value={filter} onChange={setFilter} />
      {meetings.loading && <Loading />}
      <ErrorAlert message={meetings.error} />
      {meetings.data?.length === 0 && <EmptyState icon="calendar" title="Görüşme bulunamadı" />}
      {meetings.data?.map((m) => (
        <MeetingCard
          key={m.id}
          m={m}
          showGroup
          actions={
            <>
              {m.status === 'ONAYLANDI' && new Date(m.start).getTime() <= now && (
                <button className="btn btn-sm btn-dark" disabled={action.busy} onClick={() => act(() => meetingApi.complete(m.id))}>
                  <i className="bi bi-check2-all me-1" />
                  Görüşme yapıldı
                </button>
              )}
              {m.status === 'TAMAMLANDI' && (
                <button className="btn btn-sm btn-outline-primary" onClick={() => setNoteFor(m)}>
                  <i className="bi bi-journal-text me-1" />
                  {m.note ? 'Notu düzenle' : 'Not ve karar gir'}
                </button>
              )}
              {m.canCancel && m.status !== 'BEKLEMEDE' && (
                <button className="btn btn-sm btn-outline-secondary" disabled={action.busy} onClick={() => act(() => meetingApi.cancel(m.id), 'Bu görüşmeyi iptal etmek istediğine emin misin?')}>
                  İptal et
                </button>
              )}
            </>
          }
        />
      ))}

      {proposeFor && (
        <Modal title="Yeni saat öner" onClose={() => setProposeFor(null)}>
          <p className="small text-secondary">
            “{proposeFor.topic}” için boş saatlerinden birini seç. Öğrenciler kabul ederse görüşme onaylanır.
          </p>
          {freeSlots === null && <Loading />}
          {freeSlots?.length === 0 && (
            <p className="mb-0">
              Boş saatin yok. Önce <Link to="/musaitlik">müsaitlik ekle</Link>.
            </p>
          )}
          {freeSlots &&
            groupByDay(freeSlots).map(([day, list]) => (
              <div key={day} className="mb-2">
                <div className="small fw-semibold text-capitalize">{day}</div>
                <div className="d-flex flex-wrap gap-2">
                  {list.map((s) => (
                    <button key={s.id} className="btn btn-sm btn-outline-primary slot-btn" disabled={action.busy} onClick={() => propose(s.id)}>
                      {formatTime(s.start)} – {formatTime(s.end)}
                    </button>
                  ))}
                </div>
              </div>
            ))}
        </Modal>
      )}

      {noteFor && (
        <NoteModal
          meeting={noteFor}
          onClose={() => setNoteFor(null)}
          onSaved={() => {
            setNoteFor(null)
            reloadAll()
          }}
        />
      )}
    </>
  )
}

/** FR-04: görüşme notu ve kararlar. */
function NoteModal({ meeting, onClose, onSaved }: { meeting: Meeting; onClose: () => void; onSaved: () => void }) {
  const action = useAction()
  // useState'in başlangıç değeri: mevcut not varsa onunla doldur
  const [note, setNote] = useState(meeting.note?.note ?? '')
  const [decisions, setDecisions] = useState(meeting.note?.decisions ?? '')

  async function save(e: FormEvent) {
    e.preventDefault()
    if (await action.run(() => meetingApi.saveNote(meeting.id, note, decisions))) onSaved()
  }

  return (
    <Modal title="Görüşme notu" onClose={onClose}>
      <form onSubmit={save}>
        <ErrorAlert message={action.error} />
        <p className="small text-secondary">{meeting.topic} · {meeting.groupName}</p>
        <label className="form-label" htmlFor="note">Not *</label>
        <textarea id="note" className="form-control mb-3" rows={4} value={note} onChange={(e) => setNote(e.target.value)} required />
        <label className="form-label" htmlFor="decisions">Alınan kararlar</label>
        <textarea id="decisions" className="form-control mb-3" rows={3} value={decisions} onChange={(e) => setDecisions(e.target.value)} />
        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-light" onClick={onClose}>Vazgeç</button>
          <button className="btn btn-primary" disabled={action.busy || !note.trim()}>Kaydet</button>
        </div>
      </form>
    </Modal>
  )
}
