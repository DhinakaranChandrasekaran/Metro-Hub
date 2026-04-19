import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaCheck, FaChevronDown, FaChevronRight, FaUsers, FaFileAlt, FaChartBar, FaClock } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import ErrorModal from '../components/ErrorModal'
import documentService from '../services/documentService'

// ACKNOWLEDGEMENTS PAGE — User: pending acks | Admin: per-document tracking
const AcknowledgementsPage = () => {
    const navigate = useNavigate()
    const { showToast } = useToast()
    const { hasPermission, user } = useAuth()
    // Only regular DEPARTMENT_USER can acknowledge
    // Admins (DEPARTMENT_ADMIN, DEPARTMENT_UPLOAD_ADMIN, SUPER_ADMIN) only track acknowledgements
    const isAdmin = user?.role === 'SUPER_ADMIN' || user?.role === 'DEPARTMENT_ADMIN' || user?.role === 'DEPARTMENT_UPLOAD_ADMIN'
    const isRegularUser = user?.role === 'DEPARTMENT_USER'
    const [showModal, setShowModal] = useState(null)
    const [activeTab, setActiveTab] = useState(isAdmin ? 'tracking' : 'pending')
    const [pendingItems, setPendingItems] = useState([])
    const [allDocs, setAllDocs] = useState([])
    const [loading, setLoading] = useState(true)
    const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })
    const [acknowledgedIds, setAcknowledgedIds] = useState(new Set())

    useEffect(() => { fetchData() }, [])

    const fetchData = async () => {
        setLoading(true)
        try {
            // Fetch pending acks for user
            const pendRes = await documentService.getPendingAcknowledgements(0, 50).catch(() => ({ content: [] }))
            const payload = pendRes.data || pendRes
            const pendList = (payload.content || payload || []).map(d => ({
                id: d.id, title: d.documentName || d.fileName || d.originalFileName || 'Document',
                dept: d.departmentName || '', priority: d.priority || 'MEDIUM',
                deadline: d.deadline || d.slaDeadline || '', status: 'PENDING',
            }))
            setPendingItems(pendList)

            // For admins, also fetch all documents for tracking (using pagination)
            if (isAdmin) {
                try {
                    const allRes = await documentService.getAllDocuments(0, 50).catch(() => ({ content: [] }))
                    const allPayload = allRes.data || allRes
                    const docList = (allPayload.content || allPayload || []).map(d => ({
                        id: d.id, title: d.documentName || d.fileName || d.originalFileName || 'Document',
                        dept: d.departmentName || '', priority: d.priority || 'MEDIUM',
                        date: d.uploadDate || d.createdAt || '', status: d.status || 'ACTIVE',
                        isTextExtracted: d.isTextExtracted,
                    }))
                    setAllDocs(docList)
                } catch (err) {
                    console.error('Error fetching documents:', err)
                    setAllDocs([])
                }
            }

            // Fetch which documents current user has already acknowledged
            const ackIds = new Set()
            for (const doc of pendList) {
                try {
                    const ackStatus = await documentService.hasUserAcknowledged(doc.id)
                    if (ackStatus?.acknowledged) {
                        ackIds.add(doc.id)
                    }
                } catch {
                    // Ignore errors checking acknowledgement status
                }
            }
            setAcknowledgedIds(ackIds)
        } catch (err) {
            console.error('Failed to load acknowledgements:', err)
            showToast('Failed to load acknowledgements.', 'error')
        }
        finally { setLoading(false) }
    }

    const handleAcknowledge = async (id) => {
        try {
            await documentService.acknowledge(id, 'Acknowledged via dashboard')
            setShowModal(null)
            setAcknowledgedIds(prev => new Set([...prev, id]))
            await fetchData()
            showToast('Document acknowledged successfully.', 'success')
        } catch (error) {
            console.error('Acknowledge error:', error)
            const errorMsg = error?.message || error?.response?.data?.message || 'Failed to acknowledge. Please try again.'
            setErrorModal({
                show: true,
                title: 'Acknowledgement Failed',
                message: errorMsg
            })
            setShowModal(null)
        }
    }

    const formatDate = d => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'
    const formatDateTime = d => d ? new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'
    const priorityClass = p => p === 'CRITICAL' ? 'bg-red-50 text-red-800' : p === 'HIGH' ? 'bg-orange-50 text-orange-800' : p === 'MEDIUM' ? 'bg-yellow-50 text-yellow-800' : 'bg-gray-50 text-gray-600'

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Acknowledgements</h1>
                <p style={{ color: 'rgba(255,255,255,0.9)', fontSize: '12px', margin: '8px 0 0' }}>
                    {isAdmin ? 'Admin View - Document Tracking' : 'My Pending Acknowledgements'}
                </p>
            </div>

            {loading ? (
                <div className="card-metro text-center py-10 text-gray-500">
                    <div className="text-sm">Loading acknowledgements...</div>
                </div>
            ) : isAdmin === undefined ? (
                <div className="card-metro text-center py-10 text-gray-500">
                    <div className="text-sm">Checking permissions...</div>
                </div>
            ) : (
                <>
                <div className="card-metro mb-4">
                    <div className="flex justify-between items-center">
                        <div className="flex gap-1 flex-wrap">
                            {[{ key: 'tracking', label: 'Document Tracking', icon: FaChartBar }, { key: 'pending', label: 'My Pending', icon: FaClock }].map(t => (
                                <button key={t.key} onClick={() => setActiveTab(t.key)} className={`flex items-center gap-2 px-4 py-2.5 text-sm font-medium rounded-none border-b-2 transition-colors ${activeTab === t.key ? 'border-b-2' : 'border-b-transparent'}`} style={activeTab === t.key ? { color: '#0B3C5D', borderBottomColor: '#0B3C5D' } : { color: '#6B7280', borderBottomColor: 'transparent' }}><t.icon /> {t.label}</button>
                            ))}
                        </div>
                        <div className="flex gap-3 text-sm">
                            <span style={{ color: 'rgba(0,0,0,0.6)' }}>Documents: <strong style={{ color: '#0B3C5D' }}>{allDocs.length}</strong></span>
                            <span style={{ color: 'rgba(0,0,0,0.6)' }}>Pending: <strong style={{ color: '#0B3C5D' }}>{pendingItems.filter(p => !acknowledgedIds.has(p.id)).length}</strong></span>
                        </div>
                    </div>
                </div>
                </>
            )}

            {/* ADMIN TRACKING TAB — All documents with expandable ack details */}
            {activeTab === 'tracking' && isAdmin && (
                <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                    <div className="overflow-x-auto">
                        {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading...</div> :
                        allDocs.length === 0 ? <div className="text-center py-10 text-gray-400 text-sm">No documents found.</div> : (
                            <table className="w-full text-sm table-metro">
                                <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '30%', textAlign: 'center' }}>Document</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Department</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Priority</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Date</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Status</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Track</th>
                                </tr></thead>
                                <tbody>{allDocs.map((doc, idx) => (
                                    <tr key={doc.id} className="border-b hover:bg-gray-50 transition-colors" style={{ borderColor: '#D0D7DE' }}>
                                        <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                        <td className="py-3 px-3 font-semibold text-gray-800 text-center"><FaFileAlt className="inline mr-2 text-gray-400" />{doc.title}</td>
                                        <td className="py-3 px-3 text-gray-600 text-sm text-center">{doc.dept}</td>
                                        <td className="py-3 px-3 text-center"><span className={`px-2 py-1 rounded text-xs font-bold ${priorityClass(doc.priority)}`}>{doc.priority}</span></td>
                                        <td className="py-3 px-3 text-xs text-gray-600 text-center">{formatDate(doc.date)}</td>
                                        <td className="py-3 px-3 text-center"><span className={`text-xs font-bold ${doc.status === 'ACTIVE' ? 'text-green-700' : 'text-gray-500'}`}>{doc.status}</span></td>
                                        <td className="py-3 px-3 text-center">
                                            <button onClick={() => navigate(`/acknowledgements/track/${doc.id}`)} className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded transition-colors" title="View tracking details">
                                                <FaUsers />
                                            </button>
                                        </td>
                                    </tr>
                                ))}</tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* PENDING TAB — Documents pending current user's acknowledgement */}
            {activeTab === 'pending' && (
                <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                    <div className="overflow-x-auto">
                        {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading...</div> :
                        pendingItems.length === 0 ? <div className="text-center py-10 text-gray-400 text-sm">No pending acknowledgements.</div> : (
                            <table className="w-full text-sm table-metro">
                                <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '30%', textAlign: 'center' }}>Document</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Department</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Priority</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Deadline</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Action</th>
                                </tr></thead>
                                <tbody>{pendingItems.map((item, idx) => {
                                    const isAcknowledged = acknowledgedIds.has(item.id)
                                    return (
                                    <tr key={item.id} className="border-b hover:bg-gray-50 transition-colors" style={{ borderColor: '#D0D7DE' }}>
                                        <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                        <td className="py-3 px-3 font-semibold text-gray-800 text-center"><FaFileAlt className="inline mr-2 text-gray-400" />{item.title}</td>
                                        <td className="py-3 px-3 text-gray-600 text-sm text-center">{item.dept}</td>
                                        <td className="py-3 px-3 text-center"><span className={`px-2 py-1 rounded text-xs font-bold ${priorityClass(item.priority)}`}>{item.priority}</span></td>
                                        <td className="py-3 px-3 text-xs text-gray-600 text-center">{formatDate(item.deadline)}</td>
                                        <td className="py-3 px-3 text-center">
                                            {isAcknowledged ? (
                                                <span className="px-4 py-2 text-xs font-bold rounded inline-flex items-center gap-2" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D', border: '2px solid #0B3C5D' }}>Acknowledged</span>
                                            ) : isRegularUser ? (
                                                <button onClick={() => setShowModal(item)} className="px-4 py-2 text-xs font-bold rounded text-white inline-flex items-center gap-2 border-2 transition-all hover:shadow-md hover:scale-105" style={{ backgroundColor: '#0B3C5D', borderColor: '#0B3C5D' }} title="Acknowledge this document"><FaCheck /> Acknowledge</button>
                                            ) : (
                                                <span className="px-4 py-2 text-xs font-bold rounded inline-flex items-center gap-2 text-gray-500">Tracking Only</span>
                                            )}
                                        </td>
                                    </tr>
                                    )
                                })}</tbody>
                            </table>
                        )}
                    </div>
                </div>
            )}

            {/* Acknowledge modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(null)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Confirm Acknowledgement</h3>
                            </div>
                            <p className="text-sm text-gray-600 mb-4">You are about to acknowledge: <strong>{showModal.title}</strong></p>
                            <div className="flex gap-3 justify-end pt-4 border-t" style={{ borderColor: '#D0D7DE' }}><button className="btn-metro-reset" onClick={() => setShowModal(null)}>Cancel</button><button className="btn-metro-primary" onClick={() => handleAcknowledge(showModal.id)}><FaCheck /> Acknowledge</button></div>
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
export default AcknowledgementsPage
