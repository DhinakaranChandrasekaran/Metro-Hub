import { useState, useEffect } from 'react'
import { FaExclamationTriangle, FaCheckCircle, FaClock, FaEye, FaHistory } from 'react-icons/fa'
import { useAuth, ROLES } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import ErrorModal from '../components/ErrorModal'
import api from '../services/api'

const CompliancePage = () => {
    const { hasRole, user } = useAuth()
    const { showToast } = useToast()
    const [selectedViolation, setSelectedViolation] = useState(null)
    const [resolveRemarks, setResolveRemarks] = useState('')
    const [violations, setViolations] = useState([])
    const [loading, setLoading] = useState(true)
    const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })

    useEffect(() => { fetchViolations() }, [])

    const fetchViolations = async () => {
        setLoading(true)
        try {
            const response = await api.get('/violations/admin')
            const wrapper = response.data
            const list = wrapper.data || wrapper.content || wrapper || []
            setViolations(Array.isArray(list) ? list : [])
        } catch { showToast('Failed to load violations.', 'error') }
        finally { setLoading(false) }
    }

    const getEscalationBadge = (level) => {
        if (!level || level === 0) return <span className="text-xs text-gray-400">—</span>
        const colors = { 1: '#0B3C5D', 2: '#0B3C5D', 3: '#0B3C5D' }
        return <span className="px-2 py-0.5 rounded text-xs font-medium text-white" style={{ backgroundColor: colors[level] || '#0B3C5D' }}>Level {level}</span>
    }

    const handleResolve = async () => {
        if (!resolveRemarks.trim() || !selectedViolation) {
          setErrorModal({
            show: true,
            title: 'Validation Error',
            message: 'Please enter resolution remarks before submitting.'
          })
          return
        }
        try {
            await api.post(`/violations/${selectedViolation.id}/resolve`, { remarks: resolveRemarks, resolution: resolveRemarks })
            setSelectedViolation(null)
            setResolveRemarks('')
            await fetchViolations()
            showToast('Violation resolved.', 'success')
        } catch { showToast('Failed to resolve.', 'error') }
    }

    const pendingCount = violations.filter(v => v.status === 'PENDING' || v.status === 'OPEN').length
    const resolvedCount = violations.filter(v => v.status === 'RESOLVED').length

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Compliance & Violations</h1>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-5">
                <div className="stat-card"><div><p className="stat-label">Total Violations</p><p className="stat-value" style={{ color: '#0B3C5D' }}>{violations.length}</p></div><FaExclamationTriangle className="text-2xl text-gray-300" /></div>
                <div className="stat-card"><div><p className="stat-label">Pending</p><p className="stat-value" style={{ color: '#0B3C5D' }}>{pendingCount}</p></div><FaClock className="text-2xl text-gray-300" /></div>
                <div className="stat-card"><div><p className="stat-label">Resolved</p><p className="stat-value" style={{ color: '#0B3C5D' }}>{resolvedCount}</p></div><FaCheckCircle className="text-2xl text-gray-300" /></div>
            </div>
            <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                <h2 className="section-title p-4 mb-0">Violation Records</h2>
                <div className="overflow-x-auto">
                {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading...</div> : (
                    <table className="w-full text-sm">
                        <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '4%', textAlign: 'center' }}>S.No</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>User</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Department</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '18%', textAlign: 'center' }}>Document</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Days Delayed</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Escalation</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Status</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Actions</th>
                        </tr></thead>
                        <tbody>{violations.length === 0 ? <tr><td colSpan="8" className="py-6 text-center text-gray-400">No violations found.</td></tr> :
                            violations.map((v, idx) => (
                                <tr key={v.id} className={`border-b hover:bg-gray-50 ${v.status === 'RESOLVED' ? 'opacity-60' : (v.daysDelayed || 0) >= 3 ? '' : ''}`} style={{ borderColor: '#0B3C5D' }}>
                                    <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                    <td className="py-3 px-3 font-medium text-center">{v.userName || v.user || '-'}</td>
                                    <td className="py-3 px-3 text-center">{v.departmentName || v.dept || '-'}</td>
                                    <td className="py-3 px-3 text-sm text-center">{v.documentName || v.document || '-'}</td>
                                    <td className="py-3 px-3 text-center"><span className={(v.daysDelayed || 0) >= 3 ? 'font-bold text-red-700' : (v.daysDelayed || 0) >= 1 ? 'text-orange-700' : 'text-gray-500'}>{v.daysDelayed || 0}</span></td>
                                    <td className="py-3 px-3 text-center">{getEscalationBadge(v.escalationLevel)}</td>
                                    <td className="py-3 px-3 text-center">{v.status === 'RESOLVED' ? <span className="text-xs text-green-700 font-semibold">Resolved</span> : <span className="text-xs text-red-700 font-semibold">Pending</span>}</td>
                                    <td className="py-3 px-3 text-center"><button onClick={() => setSelectedViolation(v)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded"><FaEye className="text-sm" /></button></td>
                                </tr>
                            ))}</tbody>
                    </table>)}
                </div>
            </div>
            {selectedViolation && (
                <div className="modal-overlay" onClick={() => setSelectedViolation(null)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '600px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Violation Details</h3>
                            </div>
                            <div className="grid grid-cols-2 gap-3 text-sm mb-4">
                                <div><span className="text-gray-500">User:</span> <strong>{selectedViolation.userName || selectedViolation.user}</strong></div>
                                <div><span className="text-gray-500">Department:</span> <strong>{selectedViolation.departmentName || selectedViolation.dept}</strong></div>
                                <div className="col-span-2"><span className="text-gray-500">Document:</span> <strong>{selectedViolation.documentName || selectedViolation.document}</strong></div>
                                <div><span className="text-gray-500">Days Delayed:</span> <strong className="risk-critical">{selectedViolation.daysDelayed || 0}</strong></div>
                                <div><span className="text-gray-500">Escalation:</span> {getEscalationBadge(selectedViolation.escalationLevel)}</div>
                            </div>
                            {(selectedViolation.auditLog || []).length > 0 && (
                                <div className="mb-4"><h4 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-2"><FaHistory /> Audit Trail</h4>
                                    <div className="border rounded" style={{ borderColor: '#D0D7DE' }}>{selectedViolation.auditLog.map((log, idx) => (
                                        <div key={idx} className={`px-3 py-2 text-xs flex gap-3 ${idx % 2 === 0 ? 'bg-gray-50' : ''}`}><span className="text-gray-400 whitespace-nowrap">{log.time || log.timestamp}</span><span className="text-gray-700">{log.action}</span><span className="text-gray-400 ml-auto">by {log.by || log.performedBy}</span></div>
                                    ))}</div></div>
                            )}
                            {(selectedViolation.status === 'PENDING' || selectedViolation.status === 'OPEN') && (
                                <div className="border-t pt-4" style={{ borderColor: '#D0D7DE' }}>
                                    <label className="label-metro">Resolution Remarks <span className="required-asterisk">*</span></label>
                                    <textarea className="input-metro" rows={3} placeholder="Enter resolution remarks..." value={resolveRemarks} onChange={e => setResolveRemarks(e.target.value)}></textarea>
                                    <div className="flex gap-3 justify-end mt-3"><button className="btn-metro-reset" onClick={() => setSelectedViolation(null)}>Close</button><button className="btn-metro-primary" onClick={handleResolve} disabled={!resolveRemarks.trim()}><FaCheckCircle /> Resolve</button></div>
                                </div>
                            )}
                            {selectedViolation.status === 'RESOLVED' && (
                                <div className="border-t pt-4" style={{ borderColor: '#D0D7DE' }}>
                                    <p className="text-sm text-gray-600"><strong>Resolved by:</strong> {selectedViolation.resolvedBy || '-'}</p>
                                    <p className="text-sm text-gray-600"><strong>Resolved at:</strong> {selectedViolation.resolvedAt || '-'}</p>
                                    <p className="text-sm text-gray-600"><strong>Remarks:</strong> {selectedViolation.remarks || selectedViolation.resolution || '-'}</p>
                                    <div className="flex justify-end mt-3"><button className="btn-metro-reset" onClick={() => setSelectedViolation(null)}>Close</button></div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

      {/* Error Modal */}
      <ErrorModal
        show={errorModal.show}
        title={errorModal.title}
        message={errorModal.message}
        onClose={() => setErrorModal({ show: false, title: '', message: '' })}
      />
        </div>
    )
}
export default CompliancePage
