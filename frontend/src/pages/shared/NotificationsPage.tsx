/** Bildirimler (FR-08): randevu, görev, teslim ve hatırlatma bildirimleri. */
import { notificationApi } from '../../api/endpoints'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import EmptyState from '../../components/EmptyState'
import { formatDateTime } from '../../utils/format'

/** Bildirim türü → ikon */
const ICONS: Record<string, string> = {
  RANDEVU: 'calendar-event',
  RANDEVU_HATIRLATMA: 'alarm',
  TESLIM_HATIRLATMA: 'alarm',
  PROJE: 'kanban',
  GOREV: 'list-check',
  TESLIM: 'upload',
  GERI_BILDIRIM: 'chat-left-text',
  GRUP: 'people',
  KVKK: 'shield-lock',
  HESAP: 'person-lock',
}

export default function NotificationsPage() {
  const { data, setData, loading, error } = useApi(() => notificationApi.list(), [])
  const action = useAction()

  async function markRead(id: number) {
    await action.run(() => notificationApi.markRead(id))
    // Sunucuya tekrar sormadan listede yalnızca o bildirimi "okundu" yap:
    setData((list) => list?.map((n) => (n.id === id ? { ...n, read: true } : n)) ?? null)
  }

  async function markAll() {
    await action.run(() => notificationApi.markAllRead())
    setData((list) => list?.map((n) => ({ ...n, read: true })) ?? null)
  }

  const unread = data?.filter((n) => !n.read).length ?? 0

  return (
    <>
      <PageHeader
        title="Bildirimler"
        subtitle={unread ? `${unread} okunmamış bildirim` : 'Tüm bildirimler okundu'}
        actions={
          unread > 0 && (
            <button className="btn btn-outline-primary" disabled={action.busy} onClick={markAll}>
              <i className="bi bi-check2-all me-1" />
              Tümünü okundu yap
            </button>
          )
        }
      />
      <ErrorAlert message={error ?? action.error} />
      {loading && <Loading />}
      {data?.length === 0 && <EmptyState icon="bell" title="Henüz bildirim yok" />}
      <div className="list-group">
        {data?.map((n) => (
          <button
            key={n.id}
            type="button"
            className={`list-group-item list-group-item-action d-flex gap-3 align-items-start ${n.read ? '' : 'list-group-item-primary'}`}
            onClick={() => !n.read && markRead(n.id)}
          >
            <i className={`bi bi-${ICONS[n.type] ?? 'bell'} fs-5 mt-1`} />
            <div className="flex-grow-1 text-start">
              <div className={n.read ? '' : 'fw-semibold'}>{n.message}</div>
              <div className="small text-secondary">{formatDateTime(n.createdAt)}</div>
            </div>
            {!n.read && <span className="badge text-bg-primary">yeni</span>}
          </button>
        ))}
      </div>
    </>
  )
}
