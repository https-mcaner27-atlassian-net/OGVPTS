/** Renkli durum rozeti. Kullanım: <StatusBadge badge={meetingStatus[m.status]} /> */
export default function StatusBadge({ badge }: { badge: { label: string; color: string } }) {
  // Bootstrap 5.3'ün "text-bg-*" sınıfı, arka plana göre okunaklı yazı rengini kendisi seçer.
  return <span className={`badge text-bg-${badge.color}`}>{badge.label}</span>
}
