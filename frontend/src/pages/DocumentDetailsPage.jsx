import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { FaDownload, FaFileAlt, FaCheck, FaClock } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import documentService from '../services/documentService'

const DocumentDetailsPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { hasPermission } = useAuth()
  const { showToast } = useToast()
  const [doc, setDoc] = useState(null)
  const [slaTimings, setSlaTimings] = useState(null)
  const [slaConfig, setSlaConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [ackLoading, setAckLoading] = useState(false)
  const [hasAcknowledged, setHasAcknowledged] = useState(false)
  const canSeeOriginal = hasPermission('viewOriginalDoc')
  const canAcknowledge = hasPermission('acknowledge')
  const canViewSla = hasPermission('viewSla')

  useEffect(() => { fetchDocument() }, [id])

  const fetchDocument = async () => {
    setLoading(true)
    try {
      const data = await documentService.getDocumentById(id)
      const docData = data.data || data
      setDoc(docData)

      // Fetch SLA config + timings from dedicated API
      try {
        const slaResponse = await documentService.getSlaConfig(id)
        // slaResponse = { success, slaConfig: { reminderHours, ... }, slaTimings: { acknowledgementDeadline, ... }, isManualSla, ... }
        setSlaConfig(slaResponse?.slaConfig || null)
        setSlaTimings(slaResponse?.slaTimings || null)
      } catch {
        console.log('Could not fetch SLA config')
      }

      // Check if user has already acknowledged this document
      if (canAcknowledge) {
        try {
          const ackStatus = await documentService.hasUserAcknowledged(id)
          setHasAcknowledged(ackStatus?.acknowledged || false)
        } catch {
          console.log('Could not fetch acknowledgement status')
        }
      }
    } catch { showToast('Failed to load document.', 'error'); navigate('/documents') }
    finally { setLoading(false) }
  }

  const handleDownload = async (type) => {
    try {
      if (type === 'extracted') {
        navigate(`/documents/${id}/extracted`)
        return
      }
      const blob = await documentService.downloadOriginal(id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a'); a.href = url
      a.download = doc.fileName || 'document'
      a.click(); URL.revokeObjectURL(url)
      showToast('Original document downloaded.', 'success')
    } catch { showToast('Download failed.', 'error') }
  }

  const handleAcknowledge = async () => {
    setAckLoading(true)
    try {
      await documentService.acknowledge(id, 'Acknowledged via document details')
      setHasAcknowledged(true)
      showToast('Document acknowledged successfully.', 'success')
    } catch (error) {
      console.error('Acknowledge error:', error)
      showToast('Acknowledgement failed.', 'error')
    }
    finally { setAckLoading(false) }
  }

  const formatDate = d => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'

  // Get SLA hours — prefer slaConfig API data, fallback to document DTO
  const getSlaHours = () => {
    // Source 1: SLA Config API response (most reliable)
    if (slaConfig) {
      const ack = slaConfig.reminderHours
      const esc1 = slaConfig.deptAdminEscalationHours
      const esc2 = slaConfig.superAdminEscalationHours
      const violation = slaConfig.violationHours
      if (ack != null || esc1 != null || esc2 != null || violation != null) {
        return { ack, esc1, esc2, violation }
      }
    }
    // Source 2: Document DTO fields
    if (doc) {
      const ack = doc.slaReminderHours
      const esc1 = doc.slaDeptAdminEscalationHours
      const esc2 = doc.slaSuperAdminEscalationHours
      const violation = doc.slaViolationHours
      if (ack != null || esc1 != null || esc2 != null || violation != null) {
        return { ack, esc1, esc2, violation }
      }
    }
    // Source 3: Check if within grace period (no SLA yet)
    if (doc?.uploadDate) {
      const minutesSinceUpload = (Date.now() - new Date(doc.uploadDate).getTime()) / (1000 * 60)
      if (minutesSinceUpload <= 30) return { ack: null, esc1: null, esc2: null, violation: null, label: 'Grace period active — Auto-SLA will apply at T+30min' }
    }
    // Default: show standard auto-SLA values
    return { ack: 24, esc1: 48, esc2: 72, violation: 168, label: 'Auto-SLA (Default Policy)' }
  }

  if (loading) return <div className="animate-fade-in"><div className="text-center py-20 text-gray-500">Loading document...</div></div>
  if (!doc) return <div className="animate-fade-in"><div className="text-center py-20 text-gray-500">Document not found.</div></div>

  // Fields to display — each on its own line
  const fields = [
    { label: 'File Name', value: doc.fileName, bold: true },
    { label: 'Document Type', value: (doc.documentTypeName || doc.documentType || '-').replace(/_/g, ' '), bold: true },
    { label: 'Department', value: doc.departmentName || '-', bold: true },
    { label: 'Priority', value: doc.priority || 'N/A', type: 'priority' },
    { label: 'Status', value: doc.status || '-', type: 'status' },
    { label: 'Upload Date', value: formatDate(doc.uploadDate) },
    { label: 'Uploaded By', value: doc.uploadedByName || '-' },
    { label: 'File Size', value: doc.fileSize ? (doc.fileSize / 1024).toFixed(1) + ' KB' : '-' },
    { label: 'File Type', value: doc.fileType || doc.fileExtension || '-' },
    { label: 'Text Extracted', value: doc.isTextExtracted ? 'Yes' : 'No', type: 'extracted' },
    doc.extractionMethod ? { label: 'Extraction Method', value: doc.extractionMethod } : null,
  ].filter(Boolean)

  return (
    <div className="animate-fade-in">
      <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
        <h1 className="page-title" style={{ color: 'white', margin: 0 }}>{doc.fileName || 'Document Details'}</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Main Details — single field per line */}
        <div className="lg:col-span-2">
          <div className="card-metro mb-4">
            <h2 className="section-title">Document Information</h2>
            <div className="space-y-2">
              {fields.map((f, i) => (
                <div key={i} className={`flex ${f.fullWidth ? 'flex-col gap-1' : 'items-center justify-between'} py-2 ${i < fields.length - 1 ? 'border-b' : ''}`} style={{ borderColor: '#F0F0F0' }}>
                  <span className="text-sm font-semibold text-gray-600" style={{ minWidth: '160px' }}>{f.label}</span>
                  {f.type === 'priority' ? (
                    <span className={`px-2 py-0.5 rounded text-xs font-bold ${doc.priority === 'HIGH' ? 'bg-orange-50 text-orange-800' : doc.priority === 'MEDIUM' ? 'bg-yellow-50 text-yellow-800' : 'bg-gray-50 text-gray-600'}`}>{f.value}</span>
                  ) : f.type === 'status' ? (
                    <span className={`text-sm font-bold ${doc.status === 'ACTIVE' ? 'text-green-700' : doc.status === 'PENDING_REVIEW' ? 'text-orange-600' : 'text-gray-500'}`}>{f.value}</span>
                  ) : f.type === 'extracted' ? (
                    <span className={`text-sm font-bold ${doc.isTextExtracted ? 'text-green-700' : 'text-gray-400'}`}>{f.value}</span>
                  ) : f.bold ? (
                    <strong className="text-sm text-gray-800">{f.value}</strong>
                  ) : (
                    <span className="text-sm text-gray-700">{f.value}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
          {/* Description — separate paragraph section */}
          {doc.description && (
            <div className="card-metro mb-4">
              <h2 className="section-title">Description</h2>
              <p className="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">{doc.description}</p>
            </div>
          )}
          {/* Tags — separate section */}
          {doc.tags && (
            <div className="card-metro">
              <h2 className="section-title">Tags</h2>
              <p className="text-sm text-gray-700">{doc.tags}</p>
            </div>
          )}
        </div>

        {/* Right Sidebar */}
        <div>
          {/* Downloads — visible to ALL users */}
          <div className="card-metro mb-4">
            <h2 className="section-title">Downloads</h2>
            <div className="flex flex-col gap-2">
              <button onClick={() => handleDownload('original')} className="flex items-center gap-2 w-full px-3 py-2 text-sm rounded border transition-colors hover:bg-gray-50" style={{ color: '#0B3C5D', borderColor: '#0B3C5D' }}><FaDownload /> Download Original</button>
              {doc.isTextExtracted && (
                <button onClick={() => handleDownload('extracted')} className="flex items-center gap-2 w-full px-3 py-2 text-sm rounded border transition-colors hover:bg-gray-50" style={{ color: '#1E7E34', borderColor: '#1E7E34' }}><FaFileAlt /> View Extracted Text</button>
              )}
            </div>
          </div>

          {/* SLA Configuration — admin only */}
          {canViewSla && (
            <div className="card-metro mb-4">
              <h2 className="section-title">SLA Timings</h2>
              {(() => {
                const slaHours = getSlaHours()
                return (
                  <>
                    {slaHours.label && (
                      <div className="text-xs text-gray-500 mb-3 px-1 py-1 bg-gray-50 rounded">{slaHours.label}</div>
                    )}
                    {/* Acknowledgement Timing */}
                    <div className="py-3" style={{ borderBottom: '1px solid #F0F0F0' }}>
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-semibold text-gray-700">Acknowledgement</span>
                        <span className="text-sm font-bold text-right">{slaHours.ack != null ? `${slaHours.ack} hrs` : '—'}</span>
                      </div>
                      {slaTimings?.acknowledgementDeadline && (
                        <div className="mt-2 text-xs">
                          <div className="text-gray-600">Deadline: {new Date(slaTimings.acknowledgementDeadline).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</div>
                          <div className="font-semibold text-gray-700 mt-1">{Math.max(0, slaTimings.acknowledgementHoursRemaining)} hrs remaining</div>
                          {slaTimings?.acknowledgementStatus === 'OVERDUE' && <div style={{ color: '#B71C1C' }} className="font-semibold">OVERDUE</div>}
                          {slaTimings?.acknowledgementStatus === 'PENDING' && <div style={{ color: '#0B3C5D' }} className="font-semibold">PENDING</div>}
                        </div>
                      )}
                    </div>

                    {/* Escalation L1 Timing */}
                    <div className="py-3" style={{ borderBottom: '1px solid #F0F0F0' }}>
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-semibold text-gray-700">Escalation L1</span>
                        <span className="text-sm font-bold text-right">{slaHours.esc1 != null ? `${slaHours.esc1} hrs` : '—'}</span>
                      </div>
                      {slaTimings?.escalationL1Deadline && (
                        <div className="mt-2 text-xs">
                          <div className="text-gray-600">Deadline: {new Date(slaTimings.escalationL1Deadline).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</div>
                          <div className="font-semibold text-gray-700 mt-1">{Math.max(0, slaTimings.escalationL1HoursRemaining)} hrs remaining</div>
                          {slaTimings?.escalationL1Status === 'ESCALATED' && <div style={{ color: '#E65100' }} className="font-semibold">ESCALATED</div>}
                          {slaTimings?.escalationL1Status === 'PENDING' && <div style={{ color: '#F57F17' }} className="font-semibold">PENDING</div>}
                        </div>
                      )}
                    </div>

                    {/* Escalation L2 Timing */}
                    <div className="py-3" style={{ borderBottom: '1px solid #F0F0F0' }}>
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-semibold text-gray-700">Escalation L2</span>
                        <span className="text-sm font-bold text-right">{slaHours.esc2 != null ? `${slaHours.esc2} hrs` : '—'}</span>
                      </div>
                      {slaTimings?.escalationL2Deadline && (
                        <div className="mt-2 text-xs">
                          <div className="text-gray-600">Deadline: {new Date(slaTimings.escalationL2Deadline).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</div>
                          <div className="font-semibold text-gray-700 mt-1">{Math.max(0, slaTimings.escalationL2HoursRemaining)} hrs remaining</div>
                          {slaTimings?.escalationL2Status === 'ESCALATED' && <div style={{ color: '#C62828' }} className="font-semibold">ESCALATED</div>}
                          {slaTimings?.escalationL2Status === 'PENDING' && <div style={{ color: '#6A1B9A' }} className="font-semibold">PENDING</div>}
                        </div>
                      )}
                    </div>

                    {/* Violation Timing */}
                    <div className="py-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-semibold text-gray-700">Violation</span>
                        <span className="text-sm font-bold text-right">{slaHours.violation != null ? `${slaHours.violation} hrs` : '—'}</span>
                      </div>
                      {slaTimings?.violationDeadline && (
                        <div className="mt-2 text-xs">
                          <div className="text-gray-600">Deadline: {new Date(slaTimings.violationDeadline).toLocaleString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })}</div>
                          <div className="font-semibold text-gray-700 mt-1">{Math.max(0, slaTimings.violationHoursRemaining)} hrs remaining</div>
                          {slaTimings?.violationStatus === 'VIOLATED' && <div style={{ color: '#B71C1C' }} className="font-semibold">VIOLATED</div>}
                          {slaTimings?.violationStatus === 'PENDING' && <div style={{ color: '#33691E' }} className="font-semibold">PENDING</div>}
                        </div>
                      )}
                    </div>
                  </>
                )
              })()}
            </div>
          )}

          {/* Acknowledge — DEPARTMENT_USER only */}
          {canAcknowledge && (
            <div className="card-metro">
              <h2 className="section-title">Acknowledgement</h2>
              {hasAcknowledged ? (
                <button disabled className="flex items-center justify-center gap-2 w-full px-3 py-2 text-sm rounded font-bold" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D', border: '2px solid #0B3C5D' }}><strong>Acknowledged</strong></button>
              ) : (
                <button onClick={handleAcknowledge} disabled={ackLoading} className="flex items-center justify-center gap-2 w-full px-3 py-2 text-sm rounded text-white transition-colors" style={{ backgroundColor: ackLoading ? '#999' : '#0B3C5D' }}><FaCheck /> {ackLoading ? 'Acknowledging...' : 'Acknowledge Document'}</button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
export default DocumentDetailsPage
