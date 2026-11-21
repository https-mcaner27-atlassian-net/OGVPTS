/**
 * KVKK aydınlatma metni ve veri silme talebi (NFR-03).
 * Not: Metin ders projesi için hazırlanmış örnektir; gerçek kullanımda üniversitenin
 * hukuk birimince onaylanmış metin konmalıdır.
 */
import { useState } from 'react'
import { authApi } from '../../api/endpoints'
import { useAction } from '../../hooks/useApi'
import PageHeader from '../../components/PageHeader'
import ErrorAlert from '../../components/ErrorAlert'

export default function PrivacyPage() {
  const action = useAction()
  const [sent, setSent] = useState(false)

  async function requestDeletion() {
    if (!window.confirm('Kişisel verilerinin silinmesi için yöneticiye talep gönderilecek. Devam edilsin mi?')) return
    const ok = await action.run(async () => {
      await authApi.requestDataDeletion()
      return true
    })
    if (ok) setSent(true)
  }

  return (
    <>
      <PageHeader title="KVKK aydınlatma metni" subtitle="6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında bilgilendirme" />
      <div className="card mb-3">
        <div className="card-body">
          <h2 className="h6">Hangi verileri işliyoruz?</h2>
          <p>Ad soyad, okul e-posta adresi, kullanıcı rolü; proje grubu, proje ve görev bilgileri; görüşme talepleri ve notları; yüklenen teslim dosyaları ve bildirimler.</p>
          <h2 className="h6">Hangi amaçla?</h2>
          <p>Öğrenci–danışman görüşmelerinin planlanması, projelerin ve görevlerin takibi, teslimlerin değerlendirilmesi ve bölüm raporlaması.</p>
          <h2 className="h6">Kimler erişebilir?</h2>
          <p>Öğrenciler yalnızca kendi gruplarının kayıtlarını, danışmanlar yalnızca sorumlu oldukları grupların kayıtlarını görür. Yöneticiler eşleştirme ve raporlama için gerekli bilgilere erişir. Şifreler geri döndürülemez biçimde (hash) saklanır.</p>
          <h2 className="h6 mb-2">Haklarınız</h2>
          <p className="mb-0">KVKK'nın 11. maddesi uyarınca verilerinize erişme, düzeltilmesini ve silinmesini talep etme hakkına sahipsiniz. Silme talebinizi aşağıdan iletebilirsiniz.</p>
        </div>
      </div>
      <ErrorAlert message={action.error} />
      {sent ? (
        <div className="alert alert-success">Talebin yöneticiye iletildi. Sonuç sana bildirim olarak gelecek.</div>
      ) : (
        <button className="btn btn-outline-danger" disabled={action.busy} onClick={requestDeletion}>
          <i className="bi bi-shield-x me-1" />
          Verilerimin silinmesini talep et
        </button>
      )}
    </>
  )
}
