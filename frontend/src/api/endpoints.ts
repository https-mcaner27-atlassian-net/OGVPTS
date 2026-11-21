/**
 * Backend uç noktalarının (REST API) tek listesi.
 * Sayfalar URL yazmaz; buradaki fonksiyonları çağırır. URL değişirse yalnızca burayı düzeltiriz.
 * Her fonksiyonun yanındaki FR/BR kodu, JIRA'daki gereksinime karşılık gelir.
 */
import { api } from './client'
import type {
  AdvisorLoad,
  AppNotification,
  FeedbackDecision,
  Group,
  LoginResponse,
  Me,
  Meeting,
  MeetingStatus,
  Project,
  ProjectStatus,
  ProjectSummary,
  Report,
  Slot,
  Task,
  TaskDetail,
  UserAdminView,
  UserRole,
  UserSummary,
} from './types'

/** Sorgu parametrelerini (?a=1&b=2) boş olanları atlayarak üretir. */
function query(params: Record<string, string | number | null | undefined>): string {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') q.append(key, String(value))
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

// ---------------------------------------------------------------- giriş / hesap (FR-01)
export const authApi = {
  login: (email: string, password: string) => api.post<LoginResponse>('/api/auth/login', { email, password }),
  me: () => api.get<Me>('/api/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.put<void>('/api/auth/password', { currentPassword, newPassword }),
  requestDataDeletion: () => api.post<void>('/api/me/data-deletion-request'), // NFR-03 KVKK
}

// ---------------------------------------------------------------- gruplar (FR-12)
export const groupApi = {
  mine: () => api.get<Group | null>('/api/groups/mine'),
  create: (name: string) => api.post<Group>('/api/groups', { name }),
  addMember: (groupId: number, email: string) => api.post<Group>(`/api/groups/${groupId}/members`, { email }),
  advisorGroups: () => api.get<Group[]>('/api/groups'),
}

// ---------------------------------------------------------------- projeler (FR-05, FR-06, FR-07)
export type NewMilestone = { title: string; dueDate: string }

export const projectApi = {
  mine: () => api.get<Project | null>('/api/projects/mine'),
  list: () => api.get<ProjectSummary[]>('/api/projects'),
  get: (id: number) => api.get<Project>(`/api/projects/${id}`),
  create: (title: string, description: string, milestones: NewMilestone[]) =>
    api.post<Project>('/api/projects', { title, description, milestones }),
  update: (id: number, title: string, description: string) =>
    api.put<Project>(`/api/projects/${id}`, { title, description }),
  addMilestone: (projectId: number, m: NewMilestone) => api.post<Project>(`/api/projects/${projectId}/milestones`, m),
  setMilestoneStatus: (milestoneId: number, status: ProjectStatus) =>
    api.patch<Project>(`/api/milestones/${milestoneId}/status`, { status }),
  deleteMilestone: (milestoneId: number) => api.delete<Project>(`/api/milestones/${milestoneId}`),
  comment: (projectId: number, body: string) => api.post<Project>(`/api/projects/${projectId}/comments`, { body }),
}

// ---------------------------------------------------------------- müsaitlik ve görüşmeler (FR-02/03/04/09)
export const slotApi = {
  mine: () => api.get<Slot[]>('/api/slots/mine'),
  myFree: () => api.get<Slot[]>('/api/slots/mine/free'),
  available: () => api.get<Slot[]>('/api/slots/available'),
  create: (start: string, end: string) => api.post<Slot>('/api/slots', { start, end }),
  remove: (id: number) => api.delete<void>(`/api/slots/${id}`),
}

export type MeetingFilter = { status?: MeetingStatus | ''; from?: string; to?: string }

export const meetingApi = {
  list: (f: MeetingFilter = {}) => api.get<Meeting[]>(`/api/meetings${query({ status: f.status, from: f.from, to: f.to })}`),
  request: (slotId: number, topic: string) => api.post<Meeting>('/api/meetings', { slotId, topic }),
  approve: (id: number) => api.post<Meeting>(`/api/meetings/${id}/approve`),
  reject: (id: number) => api.post<Meeting>(`/api/meetings/${id}/reject`),
  propose: (id: number, slotId: number) => api.post<Meeting>(`/api/meetings/${id}/propose`, { slotId }),
  acceptProposal: (id: number) => api.post<Meeting>(`/api/meetings/${id}/accept-proposal`),
  declineProposal: (id: number) => api.post<Meeting>(`/api/meetings/${id}/decline-proposal`),
  cancel: (id: number) => api.post<Meeting>(`/api/meetings/${id}/cancel`),
  complete: (id: number) => api.post<Meeting>(`/api/meetings/${id}/complete`),
  saveNote: (id: number, note: string, decisions: string) =>
    api.put<Meeting>(`/api/meetings/${id}/note`, { note, decisions }),
}

// ---------------------------------------------------------------- görevler (FR-13/14/15)
export const taskApi = {
  list: () => api.get<Task[]>('/api/tasks'),
  detail: (id: number) => api.get<TaskDetail>(`/api/tasks/${id}`),
  create: (groupId: number, title: string, description: string, dueDate: string) =>
    api.post<Task>('/api/tasks', { groupId, title, description, dueDate }),
  submit: (taskId: number, file: File) => {
    const form = new FormData() // dosyalar JSON ile değil, "multipart/form-data" ile gönderilir
    form.append('file', file)
    return api.post<TaskDetail>(`/api/tasks/${taskId}/submissions`, form)
  },
  feedback: (submissionId: number, decision: FeedbackDecision, comment: string) =>
    api.post<TaskDetail>(`/api/submissions/${submissionId}/feedback`, { decision, comment }),
  download: (submissionId: number, fileName: string) => api.download(`/api/submissions/${submissionId}/file`, fileName),
}

// ---------------------------------------------------------------- bildirimler (FR-08)
export const notificationApi = {
  list: () => api.get<AppNotification[]>('/api/notifications'),
  unreadCount: () => api.get<{ count: number }>('/api/notifications/unread-count'),
  markRead: (id: number) => api.post<void>(`/api/notifications/${id}/read`),
  markAllRead: () => api.post<void>('/api/notifications/read-all'),
}

// ---------------------------------------------------------------- yönetici (FR-10, FR-11)
export const adminApi = {
  users: (role?: UserRole) => api.get<UserAdminView[]>(`/api/admin/users${query({ role })}`),
  createUser: (fullName: string, email: string, role: UserRole, password: string) =>
    api.post<UserSummary>('/api/admin/users', { fullName, email, role, password }),
  resetPassword: (userId: number, password: string) => api.put<void>(`/api/admin/users/${userId}/password`, { password }),
  groups: () => api.get<Group[]>('/api/admin/groups'),
  advisors: () => api.get<AdvisorLoad[]>('/api/admin/advisors'),
  assignAdvisor: (groupId: number, advisorId: number, force = false) =>
    api.put<Group>(`/api/admin/groups/${groupId}/advisor`, { advisorId, force }),
  report: (from: string, to: string) => api.get<Report>(`/api/admin/reports${query({ from, to })}`),
  downloadReport: (from: string, to: string) =>
    api.download(`/api/admin/reports/excel${query({ from, to })}`, `ogvpts-rapor-${from}-${to}.xlsx`),
}
