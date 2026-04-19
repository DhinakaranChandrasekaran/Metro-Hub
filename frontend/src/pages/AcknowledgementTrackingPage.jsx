import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { FaClock } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import documentService from '../services/documentService'

const AcknowledgementTrackingPage = () => {
    const { docId } = useParams()
    const { showToast } = useToast()
    const [activeTab, setActiveTab] = useState('acknowledged')
    const [document, setDocument] = useState(null)
    const [acknowledged, setAcknowledged] = useState([])
    const [pending, setPending] = useState([])
    const [loading, setLoading] = useState(true)

    useEffect(() => { fetchData() }, [docId])

    const fetchData = async () => {
        setLoading(true)
        try {
            // Fetch document details
            const docRes = await documentService.getDocumentById(docId)
            const doc = docRes.data || docRes
            setDocument(doc)

            // Fetch acknowledged users
            const ackRes = await documentService.getAcknowledgementsForDoc(docId)
            const ackList = Array.isArray(ackRes) ? ackRes : (ackRes.data || [])
            setAcknowledged(ackList)

            // Fetch all department users
            const deptId = doc.departmentId
            const usersRes = await documentService.getDepartmentUsers(deptId)
            const allDeptUsers = Array.isArray(usersRes) ? usersRes : (usersRes.data || [])

            // Calculate pending: ONLY DEPARTMENT_USER role who haven't acknowledged
            const acknowledgedIds = new Set(ackList.map(a => a.userId || a.user?.id))
            const departmentUsersOnly = allDeptUsers.filter(u => u.role === 'DEPARTMENT_USER' || u.userRole === 'DEPARTMENT_USER')
            const pendingList = departmentUsersOnly.filter(u => !acknowledgedIds.has(u.id))
            setPending(pendingList)
        } catch { showToast('Failed to load acknowledgement details.', 'error') }
        finally { setLoading(false) }
    }

    const formatDateTime = d => d ? new Date(d).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'

    if (loading) {
        return (
            <div className="animate-fade-in">
                <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                    <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Loading...</h1>
                </div>
                <div className="card-metro text-center py-10 text-gray-500">Loading acknowledgement details...</div>
            </div>
        )
    }

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Acknowledgement Tracking</h1>
            </div>

            {/* Document Info */}
            {document && (
                <div className="card-metro mb-4">
                    <h2 className="text-base font-semibold" style={{ color: '#0B3C5D', backgroundColor: '#FAFAFA', padding: '12px 0', marginBottom: '16px', borderBottom: '1px solid #F0F0F0' }}>Document Details</h2>
                    <div className="space-y-3">
                        <div className="flex justify-between py-2 border-b" style={{ borderColor: '#F0F0F0' }}>
                            <span className="text-sm font-semibold text-gray-600">Document Name</span>
                            <span className="text-sm font-bold text-gray-800">{document.fileName || document.originalFileName}</span>
                        </div>
                        <div className="flex justify-between py-2 border-b" style={{ borderColor: '#F0F0F0' }}>
                            <span className="text-sm font-semibold text-gray-600">Department</span>
                            <span className="text-sm font-bold text-gray-800">{document.departmentName || '-'}</span>
                        </div>
                        <div className="flex justify-between py-2 border-b" style={{ borderColor: '#F0F0F0' }}>
                            <span className="text-sm font-semibold text-gray-600">Priority</span>
                            <span className={`text-sm font-bold px-2 py-1 rounded ${document.priority === 'CRITICAL' ? 'bg-red-50 text-red-800' : document.priority === 'HIGH' ? 'bg-orange-50 text-orange-800' : document.priority === 'MEDIUM' ? 'bg-yellow-50 text-yellow-800' : 'bg-gray-50 text-gray-600'}`}>{document.priority || 'MEDIUM'}</span>
                        </div>
                        <div className="flex justify-between py-2" style={{ borderColor: '#F0F0F0' }}>
                            <span className="text-sm font-semibold text-gray-600">Status</span>
                            <span className={`text-sm font-bold ${document.status === 'ACTIVE' ? 'text-green-700' : 'text-gray-500'}`}>{document.status || 'ACTIVE'}</span>
                        </div>
                    </div>
                </div>
            )}

            {/* Tabs */}
            <div className="card-metro mb-4">
                <div className="flex gap-1">
                    {[
                        { key: 'acknowledged', label: `Acknowledged (${acknowledged.length})` },
                        { key: 'pending', label: `Pending (${pending.length})` }
                    ].map(t => (
                        <button
                            key={t.key}
                            onClick={() => setActiveTab(t.key)}
                            className={`px-4 py-2.5 text-sm font-medium rounded-none border-b-2 transition-colors`}
                            style={activeTab === t.key ? { color: '#0B3C5D', borderBottomColor: '#0B3C5D' } : { color: '#6B7280', borderBottomColor: 'transparent' }}
                        >
                            {t.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Acknowledged Tab */}
            {activeTab === 'acknowledged' && (
                <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                    <div className="overflow-x-auto">
                    {acknowledged.length === 0 ? (
                        <div className="text-center py-10 text-gray-400 text-sm">No acknowledgements yet.</div>
                    ) : (
                        <table className="w-full text-sm table-metro">
                            <thead>
                                <tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '20%', textAlign: 'center' }}>User Name</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Email</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Acknowledged At</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Notes</th>
                                </tr>
                            </thead>
                            <tbody>
                                {acknowledged.filter(ack => ack.userRole === 'DEPARTMENT_USER' || !ack.userRole).map((ack, idx) => (
                                    <tr key={idx} className="border-b hover:bg-gray-50 transition-colors" style={{ borderColor: '#D0D7DE' }}>
                                        <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                        <td className="py-3 px-3 font-semibold text-gray-800 text-center">{ack.userName || ack.user?.name || '-'}</td>
                                        <td className="py-3 px-3 text-gray-600 text-center">{ack.userEmail || ack.user?.email || '-'}</td>
                                        <td className="py-3 px-3 text-gray-600 text-center"><span className="text-xs font-medium">{formatDateTime(ack.acknowledgedAt || ack.createdAt)}</span></td>
                                        <td className="py-3 px-3 text-gray-500 text-xs text-center">{ack.notes || '-'}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                    </div>
                </div>
            )}

            {/* Pending Tab */}
            {activeTab === 'pending' && (
                <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                    <div className="overflow-x-auto">
                    {pending.length === 0 ? (
                        <div className="text-center py-10 text-gray-400 text-sm">All users have acknowledged this document!</div>
                    ) : (
                        <table className="w-full text-sm table-metro">
                            <thead>
                                <tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>User Name</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '30%', textAlign: 'center' }}>Email</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Designation</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {pending.map((user, idx) => (
                                    <tr key={idx} className="border-b hover:bg-gray-50 transition-colors" style={{ borderColor: '#D0D7DE' }}>
                                        <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                        <td className="py-3 px-3 font-semibold text-gray-800 text-center">{user.name || '-'}</td>
                                        <td className="py-3 px-3 text-gray-600 text-center">{user.email || '-'}</td>
                                        <td className="py-3 px-3 text-gray-600 text-sm text-center">{user.designation || '-'}</td>
                                        <td className="py-3 px-3 text-center">
                                            <span className="text-xs font-medium" style={{ color: '#F59E0B' }}>Awaiting</span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                    </div>
                </div>
            )}
        </div>
    )
}

export default AcknowledgementTrackingPage
