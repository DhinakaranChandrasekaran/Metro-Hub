import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { FaArrowRight, FaFileAlt, FaCloudUploadAlt, FaCheckCircle, FaBell, FaChartLine, FaChartBar, FaExclamationTriangle, FaClock } from 'react-icons/fa'
import { useAuth, ROLES } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import dashboardService from '../services/dashboardService'
import documentService from '../services/documentService'

// Dashboard Page - Role-based data fetching
const DashboardPage = () => {
  const { user, hasRole, hasPermission } = useAuth()
  const { showToast } = useToast()
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({ total: 0, highPriority: 0, pendingAck: 0, complianceScore: 0 })

  const isSuperAdmin = hasRole(ROLES.SUPER_ADMIN)

  useEffect(() => { fetchDashboardData() }, [])

  const fetchDashboardData = async () => {
    setLoading(true)
    try {
      // Try dashboard summary endpoint first
      try {
        let summaryRes
        if (isSuperAdmin) {
          // Super Admin: Get global summary
          summaryRes = await dashboardService.getSummary()
        } else {
          // Dept Admin / Upload Admin / User: Get department-specific summary
          try {
            summaryRes = await dashboardService.getDepartmentDashboard()
          } catch {
            // Fallback to global summary if dept endpoint fails
            summaryRes = await dashboardService.getSummary()
          }
        }
        const summary = summaryRes.data || summaryRes  // Unwrap {success, data: {...}}
        // Backend returns SummaryCard objects {label, count, icon, color} - extract .count
        const extractCount = (val) => typeof val === 'object' && val !== null ? (val.count || 0) : (val || 0)

        if (summary.totalDepartmentUsers != null) {
          // Department dashboard response
          const deptDocs = summary.departmentDocuments
          const deptSummary = deptDocs?.data || deptDocs || {}
          setStats({
            total: extractCount(deptSummary.totalDocuments) || summary.totalDepartmentUsers || 0,
            highPriority: extractCount(deptSummary.highPriorityDocuments) || 0,
            pendingAck: summary.acknowledgementStats?.pendingAcknowledgements || summary.unreadAlerts || 0,
            complianceScore: deptSummary.complianceScore || 0,
          })
        } else {
          setStats({
            total: extractCount(summary.totalDocuments),
            highPriority: extractCount(summary.highPriorityDocuments),
            pendingAck: extractCount(summary.pendingActionsCount) || summary.pendingAcknowledgements || 0,
            complianceScore: summary.complianceScore || 0,
          })
        }
      } catch {
        // Fallback: calculate from documents
        const response = await documentService.getAllDocuments(0, 30)
        const docs = response.content || response || []
        const docList = Array.isArray(docs) ? docs : []
        const highPri = docList.filter(d => d.priority === 'HIGH' || d.priority === 'CRITICAL').length
        const pending = docList.filter(d => d.status === 'PENDING' || d.status === 'PROCESSING').length
        const completed = docList.filter(d => d.status === 'ACTIVE' || d.status === 'ACKNOWLEDGED').length
        const score = docList.length > 0 ? Math.round((completed / docList.length) * 100) : 100
        setStats({ total: docList.length, highPriority: highPri, pendingAck: pending, complianceScore: score })
      }

      // Fetch recent documents (8 items)
      const docResponse = await documentService.getAllDocuments(0, 8)
      const docList = docResponse.content || docResponse || []
      setDocuments(Array.isArray(docList) ? docList : [])
    } catch (err) {
      showToast('Unable to load dashboard. Check if backend is running.', 'error')
    } finally {
      setLoading(false)
    }
  }

  const formatDate = (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'
  const canSeeOriginal = hasPermission('viewOriginalDoc')
  const roleLabel = {
    [ROLES.SUPER_ADMIN]: 'System Administrator',
    [ROLES.DEPARTMENT_ADMIN]: 'Department Administrator',
    [ROLES.DEPARTMENT_UPLOAD_ADMIN]: 'Upload Administrator',
    [ROLES.DEPARTMENT_USER]: 'Department User',
  }[user?.role] || 'User'
  const today = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })

  const handleDownload = async (doc, type) => {
    try {
      const blob = type === 'original'
        ? await documentService.downloadOriginal(doc.id)
        : await documentService.downloadExtracted(doc.id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = type === 'original' ? (doc.fileName || 'document') : (doc.fileName?.replace(/\.[^.]+$/, '') + '_extracted.txt')
      a.click()
      URL.revokeObjectURL(url)
      showToast(`${type === 'original' ? 'Original' : 'Extracted'} document downloaded.`, 'success')
    } catch {
      showToast('Download failed.', 'error')
    }
  }

  return (
    <div className="animate-fade-in">
      {/* Welcome Section */}
      <div className="mb-6" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '32px', borderRadius: '8px' }}>
        <div>
          <h1 className="text-2xl font-bold" style={{ color: 'white', margin: 0 }}>Welcome back, {user?.name || 'User'}</h1>
          <p className="text-sm mt-2" style={{ color: 'rgba(255,255,255,0.9)' }}>{roleLabel} • {user?.department || 'METRO-HUB'}</p>
          <p className="text-xs mt-1" style={{ color: 'rgba(255,255,255,0.7)' }}>{today}</p>
        </div>
      </div>

      {/* Stats Grid */}
      <div className={`grid gap-4 mb-6 ${hasRole(ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN) ? 'grid-cols-2 md:grid-cols-4' : 'grid-cols-1 md:grid-cols-3'}`}>
        {hasRole(ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN) && (
          <>
            <StatCard icon={FaFileAlt} label="Total Documents" value={loading ? '—' : stats.total} color="#0B3C5D" />
            <StatCard icon={FaExclamationTriangle} label="High Priority" value={loading ? '—' : stats.highPriority} color="#0B3C5D" />
            <StatCard icon={FaClock} label="Pending Acknowledgement" value={loading ? '—' : stats.pendingAck} color="#0B3C5D" />
            <StatCard icon={FaCheckCircle} label="Compliance Score" value={loading ? '—' : `${stats.complianceScore}%`} color="#0B3C5D" />
          </>
        )}
        {hasRole(ROLES.DEPARTMENT_UPLOAD_ADMIN) && (
          <>
            <StatCard icon={FaCloudUploadAlt} label="Total Uploads" value={loading ? '—' : stats.total} color="#0B3C5D" />
            <StatCard icon={FaClock} label="Pending Review" value={loading ? '—' : stats.pendingAck} color="#0B3C5D" />
            <StatCard icon={FaExclamationTriangle} label="High Priority" value={loading ? '—' : stats.highPriority} color="#0B3C5D" />
          </>
        )}
        {hasRole(ROLES.DEPARTMENT_USER) && (
          <>
            <StatCard icon={FaFileAlt} label="My Documents" value={loading ? '—' : stats.total} color="#0B3C5D" />
            <StatCard icon={FaClock} label="Pending Acknowledgement" value={loading ? '—' : stats.pendingAck} color="#0B3C5D" />
            <StatCard icon={FaCheckCircle} label="Completed" value={loading ? '—' : (stats.total - stats.pendingAck)} color="#0B3C5D" />
          </>
        )}
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Documents */}
        <div className="lg:col-span-2">
          <div className="card-metro" style={{ overflow: 'hidden', padding: 0 }}>
            <div className="flex justify-between items-center px-5 py-4 border-b" style={{ borderColor: '#E5E9EC', backgroundColor: '#F8FAFB' }}>
              <h2 className="text-base font-semibold" style={{ color: '#0B3C5D', margin: 0 }}>📄 Recent Documents</h2>
              <Link to="/documents" className="text-xs flex items-center gap-1 hover:underline" style={{ color: '#0B3C5D' }}>View All <FaArrowRight style={{ fontSize: '10px' }} /></Link>
            </div>
            <div className="overflow-x-auto">
              {loading ? (
                <div className="text-center py-12"><div className="inline-block animate-spin"><div className="text-2xl mb-2" style={{ color: '#0B3C5D' }}>↻</div></div><p className="text-sm text-gray-500 mt-2">Loading recent documents...</p></div>
              ) : documents.length === 0 ? (
                <div className="text-center py-12"><FaFileAlt className="text-4xl mx-auto mb-2 text-gray-300" /><p className="text-sm text-gray-500">No documents found</p></div>
              ) : (
                <table className="w-full text-sm">
                  <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '28%', textAlign: 'center' }}>Document</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '16%', textAlign: 'center' }}>Department</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Priority</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '14%', textAlign: 'center' }}>Date</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Status</th>
                    <th className="py-3 px-4 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Action</th>
                  </tr></thead>
                  <tbody>{documents.map((doc, idx) => (
                    <tr key={doc.id} className="border-b hover:bg-gray-50 transition-colors" style={{ borderColor: '#F0F0F0' }}>
                      <td className="py-3 px-4 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                      <td className="py-3 px-4 font-medium text-gray-800 text-center flex items-center justify-center gap-2"><FaFileAlt className="text-gray-400" style={{ fontSize: '11px' }} /><span className="truncate">{doc.originalFileName || doc.fileName || 'Unnamed'}</span></td>
                      <td className="py-3 px-4 text-gray-600 text-xs text-center">{doc.departmentName || doc.department?.name || '-'}</td>
                      <td className="py-3 px-4 text-center"><span className={`px-2.5 py-1 rounded text-xs font-semibold ${doc.priority === 'CRITICAL' ? 'bg-red-50 text-red-800' : doc.priority === 'HIGH' ? 'bg-orange-50 text-orange-800' : doc.priority === 'MEDIUM' ? 'bg-yellow-50 text-yellow-800' : 'bg-gray-50 text-gray-600'}`}>{doc.priority || 'N/A'}</span></td>
                      <td className="py-3 px-4 text-gray-500 text-xs text-center">{formatDate(doc.uploadDate || doc.createdAt)}</td>
                      <td className="py-3 px-4 text-center"><span className={`text-xs font-semibold px-2 py-1 rounded ${doc.status === 'ACTIVE' || doc.status === 'ACKNOWLEDGED' ? 'bg-green-50 text-green-700' : doc.status === 'PENDING' ? 'bg-orange-50 text-orange-600' : 'bg-gray-50 text-gray-500'}`}>{doc.status || 'N/A'}</span></td>
                      <td className="py-3 px-4 text-center">
                        <div className="flex gap-1.5 justify-center">
                          {canSeeOriginal && doc.filePath && (
                            <button onClick={() => handleDownload(doc, 'original')} className="px-2.5 py-1 rounded text-xs font-semibold border border-blue-300 bg-blue-50 text-blue-700 hover:bg-blue-100 transition-colors" title="Download Original">PDF</button>
                          )}
                          {doc.extractedFilePath && (
                            <button onClick={() => handleDownload(doc, 'extracted')} className="px-2.5 py-1 rounded text-xs font-semibold border border-green-300 bg-green-50 text-green-700 hover:bg-green-100 transition-colors" title="Download Extracted">Text</button>
                          )}
                          {!doc.filePath && !doc.extractedFilePath && <span className="text-xs text-gray-300">—</span>}
                        </div>
                      </td>
                    </tr>
                  ))}</tbody>
                </table>
              )}
            </div>
          </div>
        </div>

        {/* Quick Actions Sidebar */}
        <div>
          <div className="card-metro">
            <h2 className="section-title">Quick Actions</h2>
            <div className="space-y-2">
              {hasPermission('upload') && <ActionCard icon={FaCloudUploadAlt} to="/upload" title="Upload Document" desc="Add new document" />}
              <ActionCard icon={FaFileAlt} to="/documents" title="Browse Documents" desc="Search & filter" />
              {hasPermission('acknowledge') && <ActionCard icon={FaCheckCircle} to="/acknowledgements" title="Acknowledgements" desc="Review pending" />}
              <ActionCard icon={FaBell} to="/notifications" title="Notifications" desc="View all alerts" />
              {hasPermission('compliance') && <ActionCard icon={FaExclamationTriangle} to="/compliance" title="Compliance" desc="SLA violations" />}
              {hasPermission('reports') && <ActionCard icon={FaChartBar} to="/reports" title="Reports" desc="Generate reports" />}
              {hasPermission('analytics') && <ActionCard icon={FaChartLine} to="/analytics" title="Analytics" desc="Risk assessment" />}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

const StatCard = ({ icon: Icon, label, value, color = '#0B3C5D' }) => (
  <div className="card-metro">
    <div className="flex items-start justify-between">
      <div className="flex-1">
        <p className="text-xs text-gray-500 mb-2 font-medium">{label}</p>
        <p className="text-3xl font-bold" style={{ color }}>{value}</p>
      </div>
      {Icon && <Icon className="text-3xl opacity-15" style={{ color }} />}
    </div>
  </div>
)

const ActionCard = ({ icon: Icon, to, title, desc }) => (
  <Link to={to} className="block p-3 rounded border bg-white hover:bg-blue-50 transition-colors" style={{ borderColor: '#E5E9EC' }}>
    <div className="flex items-center gap-2.5">
      <div className="flex-1">
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500">{desc}</p>
      </div>
    </div>
  </Link>
)
export default DashboardPage
