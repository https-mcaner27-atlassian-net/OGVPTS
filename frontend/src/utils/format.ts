/**
 * Tarih biçimlendirme ve durum etiketleri.
 * Backend durumları büyük harfli kodlarla gönderir (ör. "YENI_SAAT_ONERILDI");
 * kullanıcıya Türkçe ve renkli rozetler göstermek için buradaki tabloları kullanırız.
 */
import type { FeedbackDecision, MeetingStatus, ProjectStatus, TaskStatus, UserRole } from '../api/types'

const dateFmt = new Intl.DateTimeFormat('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' })
const dateTimeFmt = new Intl.DateTimeFormat('tr-TR', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const timeFmt = new Intl.DateTimeFormat('tr-TR', { hour: '2-digit', minute: '2-digit' })
const weekdayFmt = new Intl.DateTimeFormat('tr-TR', { weekday: 'long', day: 'numeric', month: 'long' })

/** "2026-11-25" → "25 Kasım 2026". Saat dilimi kayması olmasın diye yerel tarih olarak ayrıştırılır. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [y, m, d] = iso.substring(0, 10).split('-').map(Number)
  return dateFmt.format(new Date(y, m - 1, d))
}

/** "2026-11-25T10:00:00" → "25 Kas 2026 10:00" */
export function formatDateTime(iso: string | null | undefined): string {
  return iso ? dateTimeFmt.format(new Date(iso)) : '—'
}

export function formatTime(iso: string): string {
  return timeFmt.format(new Date(iso))
}

export function formatWeekday(iso: string): string {
  return weekdayFmt.format(new Date(iso))
}

/** Bugünün tarihi "YYYY-MM-DD" biçiminde (input type="date" için). */
export function todayIso(offsetDays = 0): string {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Kaç gün kaldı? (negatifse gecikmiş) */
export function daysUntil(iso: string): number {
  const [y, m, d] = iso.substring(0, 10).split('-').map(Number)
  const target = new Date(y, m - 1, d).getTime()
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  return Math.round((target - today) / 86_400_000)
}

/** Rozet: Türkçe metin + Bootstrap renk sınıfı. */
type Badge = { label: string; color: string }

export const roleLabels: Record<UserRole, string> = {
  OGRENCI: 'Öğrenci',
  DANISMAN: 'Danışman',
  YONETICI: 'Yönetici',
}

export const projectStatus: Record<ProjectStatus, Badge> = {
  BASLAMADI: { label: 'Başlamadı', color: 'secondary' },
  DEVAM_EDIYOR: { label: 'Devam ediyor', color: 'primary' },
  TAMAMLANDI: { label: 'Tamamlandı', color: 'success' },
}

export const meetingStatus: Record<MeetingStatus, Badge> = {
  BEKLEMEDE: { label: 'Beklemede', color: 'warning' },
  YENI_SAAT_ONERILDI: { label: 'Yeni saat önerildi', color: 'info' },
  ONAYLANDI: { label: 'Onaylandı', color: 'success' },
  TAMAMLANDI: { label: 'Tamamlandı', color: 'dark' },
  REDDEDILDI: { label: 'Reddedildi', color: 'danger' },
  IPTAL: { label: 'İptal edildi', color: 'secondary' },
}

export const taskStatus: Record<TaskStatus, Badge> = {
  ATANDI: { label: 'Atandı', color: 'secondary' },
  TESLIM_EDILDI: { label: 'Teslim edildi', color: 'primary' },
  DUZELTME_ISTENDI: { label: 'Düzeltme istendi', color: 'warning' },
  ONAYLANDI: { label: 'Onaylandı', color: 'success' },
}

export const decisionLabels: Record<FeedbackDecision, Badge> = {
  ONAY: { label: 'Onaylandı', color: 'success' },
  DUZELTME: { label: 'Düzeltme istendi', color: 'warning' },
}

export function formatSize(kb: number): string {
  return kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`
}
