/**
 * Backend'in döndürdüğü JSON yapılarının TypeScript karşılıkları.
 *
 * TypeScript nedir? JavaScript'e "tip" ekler. Java'daki gibi bir değişkenin hangi alanlara
 * sahip olduğunu önceden söyleriz; yanlış alan adı yazarsak kod daha çalışmadan hata verir.
 *
 * Buradaki her "type", Java tarafındaki bir DTO record'una karşılık gelir
 * (ör. MeResponse → edu.maltepe.ogvpts.dto.AuthDtos.MeResponse).
 *
 * Not: Java enum'ları JSON'da metin olarak gelir ("OGRENCI"). TypeScript'te bunları
 * "birleşim tipi" (union) ile yazarız: değişken yalnızca bu değerlerden birini alabilir.
 */

export type UserRole = 'OGRENCI' | 'DANISMAN' | 'YONETICI'
export type ProjectStatus = 'BASLAMADI' | 'DEVAM_EDIYOR' | 'TAMAMLANDI'
export type MeetingStatus =
  | 'BEKLEMEDE'
  | 'ONAYLANDI'
  | 'REDDEDILDI'
  | 'YENI_SAAT_ONERILDI'
  | 'TAMAMLANDI'
  | 'IPTAL'
export type TaskStatus = 'ATANDI' | 'TESLIM_EDILDI' | 'DUZELTME_ISTENDI' | 'ONAYLANDI'
export type FeedbackDecision = 'ONAY' | 'DUZELTME'

/** Tarihler JSON'da metin gelir: "2026-11-25" (LocalDate) veya "2026-11-25T10:00:00" (LocalDateTime). */
export type IsoDate = string
export type IsoDateTime = string

// ---------------------------------------------------------------- giriş
export type Me = {
  id: number
  fullName: string
  email: string
  role: UserRole
  groupId: number | null // "| null": bu alan boş olabilir (öğrencinin grubu yoksa)
  groupName: string | null
  advisorName: string | null
}

export type LoginResponse = {
  token: string
  expiresInHours: number
  user: Me
}

// ---------------------------------------------------------------- kullanıcı / grup
export type UserSummary = {
  id: number
  fullName: string
  email: string
  role: UserRole
}

export type UserAdminView = UserSummary & {
  // "&": UserSummary'nin tüm alanları + aşağıdakiler
  createdAt: IsoDateTime
  groupName: string | null
}

export type Group = {
  id: number
  name: string
  advisor: UserSummary | null
  members: UserSummary[] // "[]": dizi (liste)
  projectId: number | null
  projectTitle: string | null
  projectStatus: ProjectStatus | null
}

export type AdvisorLoad = {
  advisor: UserSummary
  groupCount: number
  maxGroups: number
}

// ---------------------------------------------------------------- proje
export type Milestone = {
  id: number
  title: string
  dueDate: IsoDate
  status: ProjectStatus
}

export type ProjectComment = {
  id: number
  authorName: string
  body: string
  createdAt: IsoDateTime
}

export type Project = {
  id: number
  groupId: number
  groupName: string
  advisorName: string | null
  title: string
  description: string | null
  status: ProjectStatus
  createdAt: IsoDateTime
  progressPercent: number
  milestones: Milestone[]
  comments: ProjectComment[]
}

export type ProjectSummary = {
  id: number
  groupId: number
  groupName: string
  title: string
  status: ProjectStatus
  progressPercent: number
  nextDueDate: IsoDate | null
}

// ---------------------------------------------------------------- görüşme
export type Slot = {
  id: number
  start: IsoDateTime
  end: IsoDateTime
  meetingStatus: MeetingStatus | null
  groupName: string | null
}

export type MeetingNote = {
  note: string
  decisions: string | null
  advisorName: string
  updatedAt: IsoDateTime
}

export type Meeting = {
  id: number
  topic: string
  status: MeetingStatus
  start: IsoDateTime
  end: IsoDateTime
  groupId: number
  groupName: string
  advisorId: number
  advisorName: string
  requestedByName: string
  createdAt: IsoDateTime
  canCancel: boolean
  note: MeetingNote | null
}

// ---------------------------------------------------------------- görev
export type Task = {
  id: number
  groupId: number
  groupName: string
  title: string
  description: string | null
  dueDate: IsoDate
  status: TaskStatus
  createdAt: IsoDateTime
  submissionCount: number
}

export type Feedback = {
  id: number
  decision: FeedbackDecision
  comment: string | null
  advisorName: string
  createdAt: IsoDateTime
}

export type Submission = {
  id: number
  versionNo: number
  fileName: string
  fileSizeKb: number
  late: boolean
  submittedAt: IsoDateTime
  submittedByName: string
  feedback: Feedback | null
}

export type TaskDetail = {
  task: Task
  submissions: Submission[]
}

// ---------------------------------------------------------------- bildirim / rapor
export type AppNotification = {
  id: number
  type: string
  message: string
  read: boolean
  createdAt: IsoDateTime
}

export type AdvisorMeetingRow = {
  advisorId: number
  advisorName: string
  total: number
  byStatus: Record<string, number> // Record<anahtar, değer>: sözlük / Map
}

export type Report = {
  from: IsoDate
  to: IsoDate
  meetingsPerAdvisor: AdvisorMeetingRow[]
  projectStatus: Record<string, number>
  totalMeetings: number
  totalProjects: number
}
