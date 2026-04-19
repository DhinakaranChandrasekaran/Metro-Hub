import { useState, useEffect } from 'react'
import { FaChartBar, FaChartPie, FaUsers, FaHistory, FaChartLine, FaDownload } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import { useAuth, ROLES } from '../context/AuthContext'
import ErrorModal from '../components/ErrorModal'
import reportService from '../services/reportService'

const ReportsPage = () => {
  const { showToast } = useToast()
  const { hasRole, user } = useAuth()
  const [activeTab, setActiveTab] = useState('compliance')
  const [loading, setLoading] = useState(true)
  const [complianceData, setComplianceData] = useState([])
  const [defaulters, setDefaulters] = useState([])
  const [auditTrail, setAuditTrail] = useState([])
  const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })

  const isSuperAdmin = hasRole(ROLES.SUPER_ADMIN)

  const tabs = [
    { key: 'compliance', label: 'Compliance Summary', icon: FaChartBar },
    { key: 'department', label: 'Department Compliance', icon: FaChartPie },
    { key: 'defaulters', label: 'User Defaulters', icon: FaUsers },
    { key: 'audit', label: 'Audit Trail', icon: FaHistory },
    { key: 'trends', label: 'Violation Trends', icon: FaChartLine },
  ]

  useEffect(() => { fetchTabData() }, [activeTab])

  // Helper: extract data from backend API response { success, data, count }
  const extractData = (res) => {
    if (!res) return []
    if (res.data && Array.isArray(res.data)) return res.data
    if (Array.isArray(res)) return res
    if (res.content && Array.isArray(res.content)) return res.content
    if (res.departments && Array.isArray(res.departments)) return res.departments
    return []
  }

  const fetchTabData = async () => {
    setLoading(true)
    try {
      if (activeTab === 'compliance' || activeTab === 'department') {
        const res = await reportService.getDepartmentCompliance()
        let list = extractData(res)
        // For Dept Admin: filter to only their department if backend returns all
        if (!isSuperAdmin && user?.department) {
          const filtered = list.filter(d =>
            (d.departmentName || '').toLowerCase() === (user.department || '').toLowerCase()
          )
          if (filtered.length > 0) list = filtered
        }
        setComplianceData(list)
      } else if (activeTab === 'defaulters') {
        // For Dept Admin, pass their department ID to filter
        const res2 = await reportService.getDefaulters()
        let list2 = extractData(res2)
        // For Dept Admin: filter by department
        if (!isSuperAdmin && user?.department) {
          const filtered = list2.filter(d =>
            (d.departmentName || '').toLowerCase() === (user.department || '').toLowerCase()
          )
          if (filtered.length > 0) list2 = filtered
        }
        setDefaulters(list2)
      } else if (activeTab === 'audit') {
        const res3 = await reportService.getAuditTrail()
        const payload3 = res3.data || res3
        setAuditTrail(payload3.content || payload3 || [])
      }
    } catch { showToast('Failed to load report data.', 'error') }
    finally { setLoading(false) }
  }

  const handleExport = async (format) => {
    try {
      const blob = format === 'pdf' ? await reportService.exportPdf(activeTab) : await reportService.exportExcel(activeTab)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a'); a.href = url; a.download = `report_${activeTab}.${format}`; a.click(); URL.revokeObjectURL(url)
      showToast(`${format.toUpperCase()} report downloaded.`, 'success')
    } catch { showToast('Export failed. Backend may not support this format.', 'error') }
  }

  const formatDate = d => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'

  return (
    <div className="animate-fade-in">
      <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
        <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Reports & Analytics</h1>
        {!isSuperAdmin && user?.department && <p className="text-sm mt-2" style={{ color: 'rgba(255,255,255,0.8)' }}>{user.department} — Department Reports</p>}
      </div>
      <div className="card-metro mb-4">
        <div className="flex justify-between items-center flex-wrap gap-3">
          <div className="flex gap-1 flex-wrap">
            {tabs.map(tab => (
              <button key={tab.key} onClick={() => setActiveTab(tab.key)} className={`flex items-center gap-2 px-3 py-2 text-sm rounded border transition-colors ${activeTab === tab.key ? 'text-white border-transparent' : 'text-gray-600 border-gray-200 hover:bg-gray-50'}`} style={activeTab === tab.key ? { backgroundColor: '#0B3C5D' } : {}}><tab.icon className="text-xs" />{tab.label}</button>
            ))}
          </div>
          <div className="flex gap-2">
            <button className="btn-metro-secondary text-xs" onClick={() => handleExport('pdf')}><FaDownload /> PDF</button>
            <button className="btn-metro-secondary text-xs" onClick={() => handleExport('excel')}><FaDownload /> Excel</button>
          </div>
        </div>
      </div>
      {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading report data...</div> : (
        <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
          <div className="overflow-x-auto">
          {(activeTab === 'compliance' || activeTab === 'department') && (
            <table className="w-full text-sm"><thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '5%' }}>S.No</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>Department</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Total</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Acknowledged</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Pending</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '30%' }}>Compliance</th>
            </tr></thead><tbody>{complianceData.length === 0 ? <tr><td colSpan="6" className="py-6 text-center text-gray-400">No data.</td></tr> :
              complianceData.map((d, i) => {
                const score = d.complianceScore || d.score || 0
                return (
                  <tr key={d.departmentId || i} className="border-b hover:bg-gray-50" style={{ borderColor: '#F0F0F0' }}>
                    <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{i + 1}</td>
                    <td className="py-3 px-3 font-medium text-gray-800 text-center">{d.departmentName || d.dept || '-'}</td>
                    <td className="py-3 px-3 text-center text-gray-600">{d.documentsReceived || d.totalDocuments || d.total || 0}</td>
                    <td className="py-3 px-3 text-center" style={{ color: '#0B3C5D' }}>{d.documentsAcknowledged || d.acknowledgedCount || d.acknowledged || 0}</td>
                    <td className="py-3 px-3 text-center" style={{ color: '#0B3C5D' }}>{d.documentsPending || d.pendingCount || d.pending || 0}</td>
                    <td className="py-3 px-3 text-center"><div className="flex items-center gap-2"><div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden"><div className="h-full rounded-full" style={{ width: `${score}%`, backgroundColor: '#0B3C5D' }}></div></div><span className="text-xs w-10 text-right">{score}%</span></div></td>
                  </tr>)
              })}</tbody></table>
          )}
          {activeTab === 'defaulters' && (
            <table className="w-full text-sm"><thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '5%' }}>S.No</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>User</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>Department</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Violations</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Pending</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '15%' }}>Category</th>
            </tr></thead><tbody>{defaulters.length === 0 ? <tr><td colSpan="6" className="py-6 text-center text-gray-400">No defaulters.</td></tr> :
              defaulters.map((d, i) => (
                <tr key={d.userId || i} className="border-b hover:bg-gray-50" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{i + 1}</td>
                  <td className="py-3 px-3 font-medium text-gray-800 text-center">{d.userName || d.name || '-'}</td>
                  <td className="py-3 px-3 text-gray-600 text-center">{d.departmentName || d.department || '-'}</td>
                  <td className="py-3 px-3 text-center" style={{ color: '#0B3C5D' }}>{d.totalViolations || d.violations || 0}</td>
                  <td className="py-3 px-3 text-center" style={{ color: '#0B3C5D' }}>{d.unresolvedViolations || d.documentsPending || d.pending || 0}</td>
                  <td className="py-3 px-3 text-center"><span className="px-2 py-0.5 rounded text-xs font-medium" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D' }}>{d.defaulterCategory || 'NONE'}</span></td>
                </tr>
              ))}</tbody></table>
          )}
          {activeTab === 'audit' && (
            <table className="w-full text-sm"><thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '5%' }}>S.No</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>Timestamp</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>User</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '20%' }}>Action</th>
              <th className="py-3 px-3 font-medium text-white text-center" style={{ width: '35%' }}>Details</th>
            </tr></thead><tbody>{auditTrail.length === 0 ? <tr><td colSpan="5" className="py-6 text-center text-gray-400">No audit records.</td></tr> :
              auditTrail.map((a, i) => (
                <tr key={i} className="border-b hover:bg-gray-50" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{i + 1}</td>
                  <td className="py-3 px-3 text-xs text-gray-500 text-center">{formatDate(a.timestamp || a.createdAt)}</td>
                  <td className="py-3 px-3 text-gray-800 text-center">{a.userName || a.user || '-'}</td>
                  <td className="py-3 px-3 text-gray-600 text-center">{a.action || '-'}</td>
                  <td className="py-3 px-3 text-xs text-gray-500 text-center">{a.details || a.description || '-'}</td>
                </tr>
              ))}</tbody></table>
          )}
          {activeTab === 'trends' && <div className="text-center py-10 text-gray-400 text-sm">Violation trend data will appear once backend generates sufficient history.</div>}
          </div>
        </div>)}

      <ErrorModal
        show={errorModal.show}
        title={errorModal.title}
        message={errorModal.message}
        onClose={() => setErrorModal({ show: false, title: '', message: '' })}
      />
    </div>
  )
}
export default ReportsPage
