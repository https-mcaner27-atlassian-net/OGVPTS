/** Yönetici: kullanıcı listesi, yeni kullanıcı ekleme ve şifre sıfırlama. */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { adminApi } from '../../api/endpoints'
import type { UserAdminView, UserRole } from '../../api/types'
import { useAction, useApi } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import Loading from '../../components/Loading'
import ErrorAlert from '../../components/ErrorAlert'
import Modal from '../../components/Modal'
import { formatDate, roleLabels } from '../../utils/format'

const ROLES: UserRole[] = ['OGRENCI', 'DANISMAN', 'YONETICI']

export default function UsersPage() {
  const [role, setRole] = useState<UserRole | ''>('')
  const [search, setSearch] = useState('')
  const users = useApi(() => adminApi.users(role || undefined), [role])
  const [showCreate, setShowCreate] = useState(false)
  const [resetFor, setResetFor] = useState<UserAdminView | null>(null)

  const q = search.trim().toLocaleLowerCase('tr')
  const visible = (users.data ?? []).filter((u) => !q || u.fullName.toLocaleLowerCase('tr').includes(q) || u.email.includes(q))

  return (
    <>
      <PageHeader
        title="Kullanıcılar"
        actions={
          <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
            <i className="bi bi-person-plus me-1" />
            Yeni kullanıcı
          </button>
        }
      />
      <div className="row g-2 mb-3">
        <div className="col-sm-8">
          <input className="form-control" placeholder="Ad veya e-posta ara" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="col-sm-4">
          <select className="form-select" value={role} onChange={(e) => setRole(e.target.value as UserRole | '')} aria-label="Rol">
            <option value="">Tüm roller</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {roleLabels[r]}
              </option>
            ))}
          </select>
        </div>
      </div>

      {users.loading && <Loading />}
      <ErrorAlert message={users.error} />
      {users.data && (
        <div className="card">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr>
                  <th>Ad soyad</th>
                  <th>E-posta</th>
                  <th>Rol</th>
                  <th>Grup</th>
                  <th>Kayıt</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {visible.map((u) => (
                  <tr key={u.id}>
                    <td className="fw-semibold">{u.fullName}</td>
                    <td className="small">{u.email}</td>
                    <td>
                      <span className="badge text-bg-light border">{roleLabels[u.role]}</span>
                    </td>
                    <td className="small">{u.groupName ?? (u.role === 'OGRENCI' ? <span className="text-warning-emphasis">grubu yok</span> : '—')}</td>
                    <td className="small text-secondary">{formatDate(u.createdAt)}</td>
                    <td className="text-end">
                      <button className="btn btn-sm btn-outline-secondary" onClick={() => setResetFor(u)} title="Şifre sıfırla">
                        <i className="bi bi-key" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showCreate && (
        <CreateUserModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false)
            void users.reload()
          }}
        />
      )}
      {resetFor && <ResetPasswordModal user={resetFor} onClose={() => setResetFor(null)} />}
    </>
  )
}

function CreateUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const action = useAction()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<UserRole>('OGRENCI')
  const [password, setPassword] = useState('')

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (await action.run(() => adminApi.createUser(fullName, email, role, password))) onCreated()
  }

  return (
    <Modal title="Yeni kullanıcı" onClose={onClose}>
      <form onSubmit={submit}>
        <ErrorAlert message={action.error} />
        <div className="mb-3">
          <label className="form-label" htmlFor="ufn">Ad soyad</label>
          <input id="ufn" className="form-control" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="uem">Okul e-postası</label>
          <input id="uem" type="email" className="form-control" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="uro">Rol</label>
          <select id="uro" className="form-select" value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {roleLabels[r]}
              </option>
            ))}
          </select>
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="upw">Geçici şifre</label>
          <input id="upw" type="password" className="form-control" autoComplete="new-password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
          <div className="form-text">En az 8 karakter. Kullanıcı ilk girişten sonra “Hesabım” sayfasından değiştirebilir.</div>
        </div>
        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-light" onClick={onClose}>Vazgeç</button>
          <button className="btn btn-primary" disabled={action.busy || password.length < 8}>Oluştur</button>
        </div>
      </form>
    </Modal>
  )
}

function ResetPasswordModal({ user, onClose }: { user: UserAdminView; onClose: () => void }) {
  const action = useAction()
  const [password, setPassword] = useState('')
  const [done, setDone] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    const ok = await action.run(async () => {
      await adminApi.resetPassword(user.id, password)
      return true
    })
    if (ok) setDone(true)
  }

  return (
    <Modal title={`Şifre sıfırla: ${user.fullName}`} onClose={onClose}>
      {done ? (
        <>
          <div className="alert alert-success">Şifre güncellendi ve kullanıcıya bildirim gönderildi.</div>
          <button className="btn btn-primary" onClick={onClose}>Tamam</button>
        </>
      ) : (
        <form onSubmit={submit}>
          <ErrorAlert message={action.error} />
          <label className="form-label" htmlFor="rpw">Yeni geçici şifre</label>
          <input id="rpw" type="password" className="form-control mb-3" autoComplete="new-password" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
          <div className="d-flex justify-content-end gap-2">
            <button type="button" className="btn btn-light" onClick={onClose}>Vazgeç</button>
            <button className="btn btn-primary" disabled={action.busy || password.length < 8}>Kaydet</button>
          </div>
        </form>
      )}
    </Modal>
  )
}
