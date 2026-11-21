/**
 * Rotalar (hangi adreste hangi sayfa açılır).
 *
 * React Router kavramları:
 *   <Route path="/gorevler" element={<TasksPage />} />  → adres /gorevler ise TasksPage çizilir
 *   İç içe <Route>'lar: dıştaki element (ör. Layout) çerçeveyi, içteki sayfa <Outlet /> yerini doldurur.
 *   ":id" → adresteki değişken kısım (ör. /gorevler/5 → id = "5"), sayfada useParams() ile okunur.
 *
 * Rol koruması: <RequireRole roles={['DANISMAN']} /> altındaki sayfalar yalnızca danışmana açılır.
 */
import { Navigate, Route, Routes } from 'react-router-dom'
import RequireRole from './auth/RequireRole'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import NotificationsPage from './pages/shared/NotificationsPage'
import AccountPage from './pages/shared/AccountPage'
import PrivacyPage from './pages/shared/PrivacyPage'
import TasksPage from './pages/shared/TasksPage'
import TaskDetailPage from './pages/shared/TaskDetailPage'
import MeetingsPage from './pages/shared/MeetingsPage'
import ProjectDetailPage from './pages/shared/ProjectDetailPage'
import GroupPage from './pages/student/GroupPage'
import MyProjectPage from './pages/student/MyProjectPage'
import SlotsPage from './pages/advisor/SlotsPage'
import ProjectsPage from './pages/advisor/ProjectsPage'
import AssignmentsPage from './pages/admin/AssignmentsPage'
import UsersPage from './pages/admin/UsersPage'
import ReportsPage from './pages/admin/ReportsPage'
import NotFoundPage from './pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/giris" element={<LoginPage />} />

      {/* Buradan sonrası giriş gerektirir */}
      <Route element={<RequireRole />}>
        <Route element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="bildirimler" element={<NotificationsPage />} />
          <Route path="hesabim" element={<AccountPage />} />
          <Route path="kvkk" element={<PrivacyPage />} />

          {/* Öğrenci ve danışman ortak sayfaları */}
          <Route element={<RequireRole roles={['OGRENCI', 'DANISMAN']} />}>
            <Route path="randevular" element={<MeetingsPage />} />
            <Route path="gorevler" element={<TasksPage />} />
            <Route path="gorevler/:id" element={<TaskDetailPage />} />
          </Route>

          {/* Yalnızca öğrenci */}
          <Route element={<RequireRole roles={['OGRENCI']} />}>
            <Route path="grubum" element={<GroupPage />} />
            <Route path="projem" element={<MyProjectPage />} />
          </Route>

          {/* Yalnızca danışman */}
          <Route element={<RequireRole roles={['DANISMAN']} />}>
            <Route path="musaitlik" element={<SlotsPage />} />
            <Route path="projeler" element={<ProjectsPage />} />
          </Route>

          {/* Danışman ve yönetici: proje ayrıntısı */}
          <Route element={<RequireRole roles={['DANISMAN', 'YONETICI']} />}>
            <Route path="projeler/:id" element={<ProjectDetailPage />} />
          </Route>

          {/* Yalnızca yönetici */}
          <Route path="yonetim" element={<RequireRole roles={['YONETICI']} />}>
            <Route index element={<Navigate to="eslestirme" replace />} />
            <Route path="eslestirme" element={<AssignmentsPage />} />
            <Route path="kullanicilar" element={<UsersPage />} />
            <Route path="raporlar" element={<ReportsPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
