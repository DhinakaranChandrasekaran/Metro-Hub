import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth, PERMISSIONS } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import ErrorBoundary from './components/ErrorBoundary'
import Layout from './components/layout/Layout'
import WelcomePage from './pages/WelcomePage'
import UnauthorizedPage from './pages/UnauthorizedPage'
import DashboardPage from './pages/DashboardPage'
import DocumentsPage from './pages/DocumentsPage'
import DocumentDetailsPage from './pages/DocumentDetailsPage'
import ExtractedTextPage from './pages/ExtractedTextPage'
import UploadPage from './pages/UploadPage'
import NotificationsPage from './pages/NotificationsPage'
import AcknowledgementsPage from './pages/AcknowledgementsPage'
import AcknowledgementTrackingPage from './pages/AcknowledgementTrackingPage'
import CompliancePage from './pages/CompliancePage'
import ReportsPage from './pages/ReportsPage'
import AnalyticsPage from './pages/AnalyticsPage'
import PoliciesPage from './pages/PoliciesPage'
import UsersPage from './pages/UsersPage'
import SettingsPage from './pages/SettingsPage'

// ROLE GUARD — Route-level protection
const RoleGuard = ({ permissionKey, children }) => {
  const { user } = useAuth()
  const allowed = PERMISSIONS[permissionKey] || []
  if (!user || !allowed.includes(user.role)) {
    return <Navigate to="/unauthorized" replace />
  }
  return children
}

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <Router>
            <Routes>
            <Route path="/" element={<WelcomePage />} />

            <Route element={<Layout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/documents" element={<DocumentsPage />} />
              <Route path="/documents/:id" element={<DocumentDetailsPage />} />
              <Route path="/documents/:id/extracted" element={<ExtractedTextPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/unauthorized" element={<UnauthorizedPage />} />

              {/* Upload: UPLOAD_ADMIN only */}
              <Route path="/upload" element={
                <RoleGuard permissionKey="upload"><UploadPage /></RoleGuard>
              } />

              {/* Acknowledge: UPLOAD_ADMIN, DEPT_USER */}
              <Route path="/acknowledgements" element={
              <RoleGuard permissionKey="viewAckList"><AcknowledgementsPage /></RoleGuard>
              } />
              <Route path="/acknowledgements/track/:docId" element={
              <RoleGuard permissionKey="viewAckList"><AcknowledgementTrackingPage /></RoleGuard>
              } />

              {/* Compliance: SUPER_ADMIN, DEPT_ADMIN */}
              <Route path="/compliance" element={
                <RoleGuard permissionKey="compliance"><CompliancePage /></RoleGuard>
              } />

              {/* Reports: SUPER_ADMIN, DEPT_ADMIN */}
              <Route path="/reports" element={
                <RoleGuard permissionKey="reports"><ReportsPage /></RoleGuard>
              } />

              {/* Analytics: SUPER_ADMIN, DEPT_ADMIN */}
              <Route path="/analytics" element={
                <RoleGuard permissionKey="analytics"><AnalyticsPage /></RoleGuard>
              } />

              {/* Policies: UPLOAD_ADMIN only */}
              <Route path="/policies" element={
                <RoleGuard permissionKey="createPolicy"><PoliciesPage /></RoleGuard>
              } />

              {/* Users: SUPER_ADMIN, DEPT_ADMIN */}
              <Route path="/users" element={
                <RoleGuard permissionKey="deptUsers"><UsersPage /></RoleGuard>
              } />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Router>
      </ToastProvider>
    </AuthProvider>
    </ErrorBoundary>
  )
}

export default App
