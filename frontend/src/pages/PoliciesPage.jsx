import { useState, useEffect, useRef } from 'react'
import { FaLock, FaTrash, FaPlus, FaSearch, FaFileAlt } from 'react-icons/fa'
import { useAuth, ROLES } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import CustomDropdown from '../components/CustomDropdown'
import policyService from '../services/policyService'
import documentService from '../services/documentService'

const PoliciesPage = () => {
    const { hasRole } = useAuth()
    const { showToast } = useToast()
    const [activeTab, setActiveTab] = useState('sla')
    const [showSlaForm, setShowSlaForm] = useState(false)
    const [showHoldForm, setShowHoldForm] = useState(false)
    const canModify = hasRole(ROLES.DEPARTMENT_UPLOAD_ADMIN)
    const canDeletePolicy = hasRole(ROLES.DEPARTMENT_ADMIN) || hasRole(ROLES.SUPER_ADMIN)
    const canRemoveHold = hasRole(ROLES.SUPER_ADMIN)
    const [policies, setPolicies] = useState([])
    const [legalHolds, setLegalHolds] = useState([])
    const [documents, setDocuments] = useState([])
    const [loading, setLoading] = useState(true)
    const [holdLoading, setHoldLoading] = useState(true)
    const [docSearch, setDocSearch] = useState('')
    const [holdSearch, setHoldSearch] = useState('')
    const [slaForm, setSlaForm] = useState({ documentId: '', ackHours: 0, esc1: 0, esc2: 0, esc3: 0 })
    const [holdForm, setHoldForm] = useState({ documentId: '', reason: '' })
    const [deleteModal, setDeleteModal] = useState({ show: false, policyId: null, policyName: '' })
    const [deleting, setDeleting] = useState(false)
    const [deleteHoldModal, setDeleteHoldModal] = useState({ show: false, holdId: null, documentName: '' })
    const [deletingHold, setDeletingHold] = useState(false)
    const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '', type: '' })
    const docSearchTimer = useRef(null)
    const holdSearchTimer = useRef(null)

    useEffect(() => { fetchPolicies(); fetchLegalHolds(); fetchDocuments() }, [])

    const fetchPolicies = async () => {
        setLoading(true)
        try { const res = await policyService.getAll(); const payload = res.data || res; setPolicies(Array.isArray(payload) ? payload : payload.content || []) }
        catch { showToast('Failed to load policies.', 'error') }
        finally { setLoading(false) }
    }
    const fetchLegalHolds = async () => {
        setHoldLoading(true)
        try { const res = await policyService.getLegalHolds(); const payload = res.data || res; setLegalHolds(Array.isArray(payload) ? payload : payload.content || []) }
        catch { /* legal holds endpoint may not exist yet */ }
        finally { setHoldLoading(false) }
    }
    const fetchDocuments = async () => {
        try { const res = await documentService.getAllDocuments(0, 50); const payload = res.data || res; setDocuments(payload.content || payload || []) }
        catch { /* ignore */ }
    }

    const filteredDocs = documents.filter(d =>
        (d.fileName || d.originalFileName || '').toLowerCase().includes(docSearch.toLowerCase()) ||
        (d.departmentName || '').toLowerCase().includes(docSearch.toLowerCase())
    )

    // Custom Dropdown Options
    const docOptions = [{ value: '', label: '-- Choose a document --' }, ...filteredDocs.map(d => ({ value: d.id, label: `${d.fileName || d.originalFileName} (${d.departmentName || ''})` }))]
    const holdDocOptions = [{ value: '', label: '-- Choose --' }, ...documents.filter(d => (d.fileName || '').toLowerCase().includes(holdSearch.toLowerCase())).map(d => ({ value: d.id, label: d.fileName || d.originalFileName }))]

    const handleAddSla = async () => {
        if (!slaForm.documentId) {
            showToast('Please select a document', 'error')
            return
        }

        // Get selected document to check grace period
        const selectedDoc = documents.find(d => d.id == slaForm.documentId)
        if (!selectedDoc) {
            showToast('Document not found', 'error')
            return
        }

        // Check grace period (30 minutes from upload)
        const uploadTime = new Date(selectedDoc.uploadDate).getTime()
        const now = Date.now()
        const minutesSinceUpload = (now - uploadTime) / (1000 * 60)
        const secondsRemaining = Math.max(0, Math.ceil(30 * 60 - (minutesSinceUpload * 60)))

        if (minutesSinceUpload > 30) {
            // Grace period expired
            setErrorModal({
                show: true,
                title: '⏰ Grace Period Expired',
                message: `Grace period for "${selectedDoc.fileName}" has expired (uploaded ${Math.floor(minutesSinceUpload)} minutes ago). Auto-SLA has been automatically applied. Manual SLA cannot be added after the 30-minute grace period.`,
                type: 'grace_expired'
            })
            return
        }

        // Grace period active - show warning if close to expiring
        if (minutesSinceUpload > 25) {
            const confirmAdd = window.confirm(
                `⚠️ Grace period expiring soon!\\n\\nOnly ${Math.ceil(secondsRemaining / 60)} minutes remaining.\\nAdd SLA now?`
            )
            if (!confirmAdd) return
        }

        try {
            await policyService.create(slaForm)
            setShowSlaForm(false)
            setSlaForm({ documentId: '', ackHours: 0, esc1: 0, esc2: 0, esc3: 0 })
            setDocSearch('')
            await fetchPolicies()
            await fetchDocuments() // Refresh documents to update SLA status
            showToast(`✅ SLA rule added successfully!`, 'success')
        } catch (error) {
            const fullError = error.response?.data?.message || error.message || 'Failed to add SLA rule.'
            const errorDetails = error.response?.data?.error || ''

            // Extract error code and user message (format: "ERROR_CODE: User message")
            const errorParts = fullError.split(':')
            const errorCode = errorParts[0]?.trim() || ''
            const userMessage = errorParts.length > 1 ? errorParts.slice(1).join(':').trim() : fullError

            // Check for specific error types
            if (errorCode === 'SLA_ALREADY_EXISTS') {
                setErrorModal({
                    show: true,
                    title: 'SLA Already Applied',
                    message: userMessage || 'Cannot add SLA timing. This document already has SLA timings applied.',
                    type: 'already_exists'
                })
            } else if (errorCode === 'SLA_GRACE_PERIOD_EXPIRED') {
                setErrorModal({
                    show: true,
                    title: 'Grace Period Expired',
                    message: userMessage || 'Cannot add SLA timing. Grace period (30 mins) has expired. Auto-SLA timings have been applied automatically.',
                    type: 'grace_expired'
                })
            } else if (errorCode === 'SLA_AUTO_ALREADY_APPLIED') {
                setErrorModal({
                    show: true,
                    title: 'Auto-SLA Already Applied',
                    message: userMessage || 'Cannot add manual SLA. Auto-SLA has already been applied to this document automatically.',
                    type: 'auto_sla_applied'
                })
            } else if (errorCode === 'SLA_UPDATE_WINDOW_EXPIRED') {
                setErrorModal({
                    show: true,
                    title: 'SLA Update Window Expired',
                    message: userMessage || 'Cannot update SLA. The update window (30 mins from when SLA was set) has expired. SLA is now locked.',
                    type: 'update_window_expired'
                })
            } else {
                // Show detailed error modal for all other errors
                setErrorModal({
                    show: true,
                    title: errorCode || 'SLA Configuration Error',
                    message: userMessage || errorDetails || 'Unable to add SLA rule. Please check the details and try again.',
                    type: 'policy_error'
                })
            }
        }
    }

    const deletePolicy = async (id) => {
        const policy = policies.find(p => p.id === id)
        const docName = documents.find(d => d.id === policy?.documentId)?.fileName || 'Unknown Document'
        setDeleteModal({ show: true, policyId: id, policyName: docName })
    }

    const confirmDeletePolicy = async () => {
        setDeleting(true)
        try {
            await policyService.delete(deleteModal.policyId)
            await fetchPolicies()
            showToast('SLA policy deleted.', 'success')
        }
        catch { showToast('Failed to delete policy.', 'error') }
        finally { setDeleting(false); setDeleteModal({ show: false, policyId: null, policyName: '' }) }
    }

    const applyHold = async () => {
        if (!holdForm.documentId || !holdForm.reason) return
        try {
            await policyService.applyLegalHold(holdForm)
            setShowHoldForm(false); setHoldForm({ documentId: '', reason: '' }); setHoldSearch('')
            await fetchLegalHolds()
            showToast('Legal hold applied.', 'warning')
        } catch { showToast('Failed to apply hold.', 'error') }
    }

    const removeHold = (docId, documentName) => {
        setDeleteHoldModal({ show: true, holdId: docId, documentName })
    }

    const confirmDeleteHold = async () => {
        setDeletingHold(true)
        try {
            await policyService.removeLegalHold(deleteHoldModal.holdId)
            await fetchLegalHolds()
            showToast('Legal hold removed.', 'success')
        }
        catch { showToast('Failed to remove hold.', 'error') }
        finally { setDeletingHold(false); setDeleteHoldModal({ show: false, holdId: null, documentName: '' }) }
    }

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Policies & Legal Holds</h1>
            </div>
            <div className="card-metro mb-4">
                <div className="flex gap-1 flex-wrap">
                    {[{ key: 'sla', label: 'SLA Configuration' }, { key: 'legal', label: 'Legal Holds' }].map(tab => (
                        <button key={tab.key} onClick={() => setActiveTab(tab.key)} className={`flex items-center gap-2 px-3 py-2 text-sm rounded border transition-colors ${activeTab === tab.key ? 'text-white border-transparent' : 'text-gray-600 border-gray-200 hover:bg-gray-50'}`} style={activeTab === tab.key ? { backgroundColor: '#0B3C5D' } : {}}>{tab.label}</button>
                    ))}
                </div>
            </div>
            {activeTab === 'sla' && (
                <div>
                    <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                        <div className="flex justify-between items-center px-4 py-3 border-b" style={{ borderColor: '#F0F0F0', backgroundColor: '#FAFAFA' }}>
                            <h2 className="text-base font-semibold" style={{ color: '#0B3C5D' }}>SLA Configuration</h2>
                            {canModify && <button className="btn-metro-primary text-sm" onClick={() => setShowSlaForm(true)}><FaPlus /> Add Rule</button>}
                        </div>
                        <div className="overflow-x-auto">
                    {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading...</div> : (
                            <table className="w-full text-sm">
                                <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Document Type</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Department</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Ack (hrs)</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Esc L1</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Esc L2</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Violation</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Status</th>
                                    {canDeletePolicy && <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Actions</th>}
                                </tr></thead>
                                <tbody>{policies.length === 0 ? <tr><td colSpan="9" className="py-6 text-center text-gray-400">No SLA policies found.</td></tr> :
                                    policies.map((p, idx) => {
                                        const docName = documents.find(d => d.id === p.documentId)?.fileName || '-'
                                        const docType = documents.find(d => d.id === p.documentId)?.documentType || 'General'
                                        return (
                                        <tr key={p.id} className={`border-b ${p.isActive === false ? 'opacity-40' : ''}`} style={{ borderColor: '#F0F0F0' }}>
                                            <td className="py-2.5 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                            <td className="py-2.5 px-3 font-medium text-gray-800 text-center">{docType}</td>
                                            <td className="py-2.5 px-3 text-gray-600 text-center">{p.departmentName || 'All'}</td>
                                            <td className="py-2.5 px-3 text-center font-semibold" style={{ color: '#0B3C5D' }}>{p.reminderHours ?? 0}</td>
                                            <td className="py-2.5 px-3 text-center text-gray-600">{p.deptAdminEscalationHours ?? 0}</td>
                                            <td className="py-2.5 px-3 text-center text-gray-600">{p.superAdminEscalationHours ?? 0}</td>
                                            <td className="py-2.5 px-3 text-center text-gray-600">{p.violationHours ?? 0}</td>
                                            <td className="py-2.5 px-3 text-center">{p.isActive !== false ? <span className="text-xs text-green-700">Active</span> : <span className="text-xs text-gray-400">Off</span>}</td>
                                            {canDeletePolicy && <td className="py-2.5 px-3 text-center">
                                                <button onClick={() => { if (!deleting) deletePolicy(p.id) }} disabled={deleting} className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded" title="Delete policy" style={{ opacity: deleting ? 0.5 : 1, cursor: deleting ? 'not-allowed' : 'pointer' }}><FaTrash /></button>
                                            </td>}
                                        </tr>
                                    )})}</tbody>
                            </table>)}
                        </div>
                    </div>
                </div>
            )}
            {activeTab === 'legal' && (
                <div>
                    <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                        <div className="flex justify-between items-center px-4 py-3 border-b" style={{ borderColor: '#F0F0F0', backgroundColor: '#FAFAFA' }}>
                            <h2 className="text-base font-semibold" style={{ color: '#0B3C5D' }}>Legal Holds</h2>
                            {canModify && <button className="btn-metro-danger text-sm" onClick={() => setShowHoldForm(true)}><FaLock /> Apply Hold</button>}
                        </div>
                        <div className="overflow-x-auto">
                    {holdLoading ? <div className="text-center py-10 text-gray-500 text-sm">Loading...</div> : (
                            <table className="w-full text-sm">
                                <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '5%', textAlign: 'center' }}>S.No</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Document</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '25%', textAlign: 'center' }}>Reason</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Date</th>
                                    <th className="py-3 px-3 font-medium text-white" style={{ width: '15%', textAlign: 'center' }}>Status</th>
                                    {canRemoveHold && <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Actions</th>}
                                </tr></thead>
                                <tbody>{legalHolds.length === 0 ? <tr><td colSpan="5" className="py-6 text-center text-gray-400">No legal holds found.</td></tr> :
                                    legalHolds.map((hold, idx) => (
                                        <tr key={hold.id || hold.documentId} className="border-b hover:bg-gray-50" style={{ borderColor: '#F0F0F0' }}>
                                            <td className="py-2.5 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                            <td className="py-2.5 px-3 font-medium text-gray-800 text-center"><FaFileAlt className="inline mr-2 text-gray-400" />{hold.documentName || hold.fileName || '-'}</td>
                                            <td className="py-2.5 px-3 text-gray-500 text-xs text-center">{hold.reason || '-'}</td>
                                            <td className="py-2.5 px-3 text-gray-500 text-xs text-center">{hold.appliedAt || hold.legalHoldDate || '-'}</td>
                                            <td className="py-2.5 px-3 text-center">
                                                <span className="text-xs text-red-700 font-medium">Active</span>
                                            </td>
                                            {canRemoveHold && <td className="py-2.5 px-3 text-center">
                                                <button onClick={() => removeHold(hold.documentId || hold.id, hold.documentName || hold.fileName || '-')} className="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded" title="Delete hold"><FaTrash /></button>
                                            </td>}
                                        </tr>
                                    ))}</tbody>
                            </table>)}
                        </div>
                    </div>
                </div>
            )}
            {showSlaForm && (
                <div className="modal-overlay" onClick={() => { setShowSlaForm(false); setDocSearch('') }}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Add SLA Rule</h3>
                            </div>
                            <div className="space-y-3">
                                <div><label className="label-metro">Search Document</label><div className="relative"><FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs" /><input className="input-metro pl-8" placeholder="Type document name or department..." value={docSearch} onChange={e => setDocSearch(e.target.value)} /></div></div>
                                <div><label className="label-metro">Select Document *</label><CustomDropdown options={docOptions} value={slaForm.documentId} onChange={val => setSlaForm({ ...slaForm, documentId: val })} /></div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div><label className="label-metro">Ack SLA (hrs)</label><input type="number" className="input-metro" min={0} value={slaForm.ackHours} onChange={e => setSlaForm({ ...slaForm, ackHours: parseInt(e.target.value) || 0 })} /></div>
                                    <div><label className="label-metro">Esc L1 (hrs)</label><input type="number" className="input-metro" min={0} value={slaForm.esc1} onChange={e => setSlaForm({ ...slaForm, esc1: parseInt(e.target.value) || 0 })} /></div>
                                    <div><label className="label-metro">Esc L2 (hrs)</label><input type="number" className="input-metro" min={0} value={slaForm.esc2} onChange={e => setSlaForm({ ...slaForm, esc2: parseInt(e.target.value) || 0 })} /></div>
                                    <div><label className="label-metro">Violation (hrs)</label><input type="number" className="input-metro" min={0} value={slaForm.esc3} onChange={e => setSlaForm({ ...slaForm, esc3: parseInt(e.target.value) || 0 })} /></div>
                                </div>
                            </div>
                            <div className="flex gap-3 justify-end mt-5 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}><button className="btn-metro-reset" onClick={() => { setShowSlaForm(false); setDocSearch('') }}>Cancel</button><button className="btn-metro-primary" onClick={handleAddSla} disabled={!slaForm.documentId}>Add Rule</button></div>
                        </div>
                    </div>
                </div>
            )}
            {showHoldForm && (
                <div className="modal-overlay" onClick={() => { setShowHoldForm(false); setHoldSearch('') }}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Apply Legal Hold</h3>
                            </div>
                            <div className="space-y-3">
                                <div><label className="label-metro">Search Document</label><div className="relative"><FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs" /><input className="input-metro pl-8" placeholder="Type document name..." value={holdSearch} onChange={e => setHoldSearch(e.target.value)} /></div></div>
                                <div><label className="label-metro">Select Document *</label><CustomDropdown options={holdDocOptions} value={holdForm.documentId} onChange={val => setHoldForm({ ...holdForm, documentId: val })} /></div>
                                <div><label className="label-metro">Reason *</label><textarea className="input-metro" rows={3} placeholder="Reason for legal hold" value={holdForm.reason} onChange={e => setHoldForm({ ...holdForm, reason: e.target.value })} /></div>
                            </div>
                            <div className="flex gap-3 justify-end mt-4 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}><button className="btn-metro-reset" onClick={() => { setShowHoldForm(false); setHoldSearch('') }}>Cancel</button><button className="btn-metro-danger" onClick={applyHold} disabled={!holdForm.documentId || !holdForm.reason}>Apply Hold</button></div>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete Policy Modal - Notifications Style */}
            {deleteModal.show && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Delete SLA Policy</h3>
                            </div>
                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Document Name</span>
                                <p className="text-sm font-semibold text-gray-800 mt-2 break-words">{deleteModal.policyName}</p>
                            </div>

                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Action</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    Remove the SLA policy for this document
                                </p>
                            </div>

                            <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Note</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    The document will revert to automatic policy-based SLA. This action cannot be undone.
                                </p>
                            </div>

                            <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                            <button
                                onClick={() => setDeleteModal({ show: false, policyId: null, policyName: '' })}
                                disabled={deleting}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #D0D7DE',
                                    borderRadius: '3px',
                                    backgroundColor: '#FFFFFF',
                                    color: '#0B3C5D',
                                    cursor: deleting ? 'not-allowed' : 'pointer',
                                    opacity: deleting ? 0.6 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deleting && (e.target.style.backgroundColor = '#E8EEF3')}
                                onMouseLeave={(e) => !deleting && (e.target.style.backgroundColor = '#FFFFFF')}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDeletePolicy}
                                disabled={deleting}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #0B3C5D',
                                    borderRadius: '3px',
                                    backgroundColor: deleting ? '#7FA3BC' : '#0B3C5D',
                                    color: 'white',
                                    cursor: deleting ? 'not-allowed' : 'pointer',
                                    opacity: deleting ? 0.7 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deleting && (e.target.style.backgroundColor = '#0E4D78')}
                                onMouseLeave={(e) => !deleting && (e.target.style.backgroundColor = '#0B3C5D')}
                            >
                                {deleting ? 'Deleting...' : 'Delete'}
                            </button>
                        </div>
                    </div>
                </div>
                </div>
            )}

            {/* SLA Error Modal - Notifications Style */}
            {errorModal.show && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>
                                    {errorModal.title}
                                </h3>
                            </div>

                            <div className="space-y-0">
                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Error Type</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    {errorModal.type === 'already_exists' ? 'SLA Configuration Conflict' : errorModal.type === 'grace_expired' ? 'Grace Period Expired' : errorModal.type === 'auto_sla_applied' ? 'Auto-SLA Already Applied' : 'SLA Configuration Error'}
                                </p>
                            </div>

                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Details</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    {errorModal.message}
                                </p>
                            </div>

                            {errorModal.type === 'already_exists' && (
                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">What Happened</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        This document already has SLA timings configured. You cannot add or modify SLA during the grace period if SLA was already set at upload time.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Recommended Actions</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>Review the existing SLA configuration for this document</li>
                                        <li>If you need different SLA timings, contact your Super Admin to modify them</li>
                                        <li>Super Admin can update SLA settings within the 30-minute window from when SLA was originally set</li>
                                    </ul>
                                </div>
                            )}

                            {errorModal.type === 'update_window_expired' && (
                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">What Happened</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        This document had SLA timings set at upload time. The 30-minute update window (from when SLA was originally set) has now expired. SLA timings are locked and cannot be modified further.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Current Status</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        ✅ SLA is LOCKED and escalations will proceed at configured times
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Recommended Actions</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>View the document details to see the locked SLA configuration</li>
                                        <li>If urgent changes are needed, contact your Super Admin immediately</li>
                                        <li>Super Admin may have override capabilities in special circumstances</li>
                                    </ul>
                                </div>
                            )}

                            {errorModal.type === 'grace_expired' && (
                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">What Happened</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        The 30-minute grace period for manually setting SLA has expired. Once the grace period ends, the system automatically applies default SLA timings based on your department's policy. SLA timings become STATIC and cannot be changed after this point.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Current Status</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        The system has automatically applied SLA timings based on your department's default policy. These timings are now permanent for this document.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Recommended Actions</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>Review the automatically applied SLA timings for this document</li>
                                        <li>If you need different SLA settings, contact your Super Admin</li>
                                        <li>For future documents, set manual SLA within the 30-minute grace period from upload time</li>
                                    </ul>
                                </div>
                            )}

                            {errorModal.type === 'auto_sla_applied' && (
                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">What Happened</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        The 30-minute grace period for manually setting SLA has expired. The system has automatically applied SLA timings based on your department's default policy. Manual SLA can no longer be added.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Auto-SLA Details</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        <strong>Timing:</strong> Automatically applied after 30 minutes from document upload<br/>
                                        <strong>Source:</strong> Department default policy<br/>
                                        <strong>Status:</strong> STATIC - Cannot be modified
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Recommended Actions</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>Review the automatically applied SLA timings</li>
                                        <li>If different SLA settings are needed, contact your Super Admin</li>
                                        <li>For future documents, set manual SLA within 30 minutes of upload</li>
                                    </ul>
                                </div>
                            )}

                            {errorModal.type === 'policy_error' && (
                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">What Happened</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        Your SLA rule could not be created or updated. This may occur if the document already has SLA timings, the update window has expired, or if the system detected a conflict with existing configurations.
                                    </p>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Possible Reasons</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>Document already has SLA timings configured</li>
                                        <li>The 30-minute update window has expired since SLA was originally set</li>
                                        <li>Grace period (30 mins from upload) has passed without SLA being set</li>
                                        <li>System configuration conflict detected</li>
                                    </ul>
                                    <span className="text-sm font-semibold text-gray-600 block mt-3">Recommended Actions</span>
                                    <ul className="text-sm text-gray-800 mt-2 list-disc list-inside">
                                        <li>Verify the document doesn't already have SLA timings applied</li>
                                        <li>If updating, ensure you're within the 30-minute window from when SLA was set</li>
                                        <li>Check if you're within the 30-minute grace period from document upload</li>
                                        <li>Contact your Super Admin if the issue persists</li>
                                    </ul>
                                </div>
                            )}
                        </div>

                        <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                            <button
                                onClick={() => setErrorModal({ show: false, title: '', message: '', type: '' })}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #0B3C5D',
                                    borderRadius: '3px',
                                    backgroundColor: '#0B3C5D',
                                    color: 'white',
                                    cursor: 'pointer',
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => (e.target.style.backgroundColor = '#0E4D78')}
                                onMouseLeave={(e) => (e.target.style.backgroundColor = '#0B3C5D')}
                            >
                                Understood
                            </button>
                        </div>
                    </div>
                </div>
                </div>
            )}

            {/* Delete Legal Hold Modal - Notifications Style */}
            {deleteHoldModal.show && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Delete Legal Hold</h3>
                            </div>
                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Document Name</span>
                                <p className="text-sm font-semibold text-gray-800 mt-2 break-words">{deleteHoldModal.documentName}</p>
                            </div>

                            <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Action</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    Remove the legal hold for this document
                                </p>
                            </div>

                            <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                <span className="text-sm font-semibold text-gray-600">Note</span>
                                <p className="text-sm text-gray-800 mt-2">
                                    The document will be available for normal operations. This action cannot be undone.
                                </p>
                            </div>

                            <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                            <button
                                onClick={() => setDeleteHoldModal({ show: false, holdId: null, documentName: '' })}
                                disabled={deletingHold}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #D0D7DE',
                                    borderRadius: '3px',
                                    backgroundColor: '#FFFFFF',
                                    color: '#0B3C5D',
                                    cursor: deletingHold ? 'not-allowed' : 'pointer',
                                    opacity: deletingHold ? 0.6 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deletingHold && (e.target.style.backgroundColor = '#E8EEF3')}
                                onMouseLeave={(e) => !deletingHold && (e.target.style.backgroundColor = '#FFFFFF')}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDeleteHold}
                                disabled={deletingHold}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #0B3C5D',
                                    borderRadius: '3px',
                                    backgroundColor: deletingHold ? '#7FA3BC' : '#0B3C5D',
                                    color: 'white',
                                    cursor: deletingHold ? 'not-allowed' : 'pointer',
                                    opacity: deletingHold ? 0.7 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deletingHold && (e.target.style.backgroundColor = '#0E4D78')}
                                onMouseLeave={(e) => !deletingHold && (e.target.style.backgroundColor = '#0B3C5D')}
                            >
                                {deletingHold ? 'Removing...' : 'Remove Hold'}
                            </button>
                        </div>
                    </div>
                </div>
                </div>
            )}
        </div>
    )
}

export default PoliciesPage
