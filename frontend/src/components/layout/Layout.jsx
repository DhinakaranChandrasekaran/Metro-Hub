import { Outlet, Navigate } from 'react-router-dom'
import GovHeader from './GovHeader'
import SystemHeader from './SystemHeader'
import Sidebar from './Sidebar'
import Footer from './Footer'
import { useAuth } from '../../context/AuthContext'

// LAYOUT COMPONENT
const Layout = () => {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="w-10 h-10 border-3 border-t-transparent rounded-full animate-spin mx-auto mb-3"
            style={{ borderColor: '#0B3C5D', borderTopColor: 'transparent' }}></div>
          <p className="text-sm text-gray-500">Loading MetroHub...</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      {/* Government Header */}
      <GovHeader />

      {/* System Header */}
      <SystemHeader />

      {/* Main Area: Sidebar + Content */}
      <div className="flex flex-1">
        <Sidebar />
        <main className="flex-1 p-6 overflow-auto" style={{ backgroundColor: '#F5F7FA' }}>
          <Outlet />
        </main>
      </div>

      {/* Footer */}
      <Footer />
    </div>
  )
}

export default Layout
