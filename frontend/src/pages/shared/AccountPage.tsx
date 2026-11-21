/** Hesabım: profil bilgisi ve şifre değiştirme. */
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthContext'
import { useAction } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import ErrorAlert from '../../components/ErrorAlert'
import { roleLabels } from '../../utils/format'

export default function AccountPage() {
  const { user } = useAuth()
  const action = useAction()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [repeat, setRepeat] = useState('')
  const [done, setDone] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setDone(false)
    if (next !== repeat) {
      action.setError('Yeni şifreler birbirini tutmuyor.')
      return
    }
    const ok = await action.run(async () => {
      await authApi.changePassword(current, next)
      return true
    })
    if (ok) {
      setDone(true)
      setCurrent('')
      setNext('')
      setRepeat('')
    }
  }

  if (!user) return null
  return (
    <>
      <PageHeader title="Hesabım" />
      <div className="row g-3">
        <div className="col-lg-5">
          <div className="card">
            <div className="card-body">
              <dl className="row mb-0">
                <dt className="col-4">Ad soyad</dt>
                <dd className="col-8">{user.fullName}</dd>
                <dt className="col-4">E-posta</dt>
                <dd className="col-8">{user.email}</dd>
                <dt className="col-4">Rol</dt>
                <dd className="col-8">{roleLabels[user.role]}</dd>
                {user.groupName && (
                  <>
                    <dt className="col-4">Grup</dt>
                    <dd className="col-8">{user.groupName}</dd>
                    <dt className="col-4">Danışman</dt>
                    <dd className="col-8 mb-0">{user.advisorName ?? 'atanmadı'}</dd>
                  </>
                )}
              </dl>
            </div>
            <div className="card-footer bg-white small">
              <Link to="/kvkk">Kişisel verilerin ve veri silme talebi (KVKK)</Link>
            </div>
          </div>
        </div>
        <div className="col-lg-7">
          <div className="card">
            <div className="card-header bg-white fw-semibold">Şifre değiştir</div>
            <div className="card-body">
              <ErrorAlert message={action.error} onClose={() => action.setError(null)} />
              {done && <div className="alert alert-success">Şifren güncellendi.</div>}
              <form onSubmit={submit} className="row g-3">
                <div className="col-12">
                  <label className="form-label" htmlFor="cur">Mevcut şifre</label>
                  <input id="cur" type="password" className="form-control" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} required />
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="new">Yeni şifre</label>
                  <input id="new" type="password" className="form-control" autoComplete="new-password" minLength={8} value={next} onChange={(e) => setNext(e.target.value)} required />
                  <div className="form-text">En az 8 karakter.</div>
                </div>
                <div className="col-md-6">
                  <label className="form-label" htmlFor="rep">Yeni şifre (tekrar)</label>
                  <input id="rep" type="password" className="form-control" autoComplete="new-password" value={repeat} onChange={(e) => setRepeat(e.target.value)} required />
                </div>
                <div className="col-12">
                  <button className="btn btn-primary" disabled={action.busy || next.length < 8 || !current}>
                    Şifreyi güncelle
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
