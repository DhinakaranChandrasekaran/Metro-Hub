import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import { FaCloudUploadAlt, FaFileAlt, FaTimes } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import CustomDropdown from '../components/CustomDropdown'
import documentService from '../services/documentService'
import { DEPARTMENTS, DOCUMENT_TYPES, PRIORITIES, ALLOWED_FILE_TYPES, MAX_FILE_SIZE } from '../utils/constants'

// UPLOAD PAGE — Upload + SLA Configuration
const UploadPage = () => {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [file, setFile] = useState(null)
  const [formData, setFormData] = useState({
    description: '', departmentId: '', documentType: '', priority: 'MEDIUM', tags: '',
    slaAckHours: 0, slaEsc1: 0, slaEsc2: 0, slaEsc3: 0,
  })
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [extracting, setExtracting] = useState(false)
  const [extractionDone, setExtractionDone] = useState(false)
  const [error, setError] = useState('')
  const [showErrorModal, setShowErrorModal] = useState(false)

  // Custom Dropdown Options
  const departmentOptions = [{ value: '', label: 'Select Department' }, ...DEPARTMENTS.map(d => ({ value: d.id, label: d.name }))]
  const docTypeOptions = [{ value: '', label: 'Select Type' }, ...DOCUMENT_TYPES.map(t => ({ value: t.value, label: t.label }))]
  const priorityOptions = (PRIORITIES || ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).map(p => {
    const val = typeof p === 'string' ? p : p.value
    const label = typeof p === 'string' ? p : p.label
    return { value: val, label }
  })

  const onDrop = useCallback((acceptedFiles, rejectedFiles) => {
    if (rejectedFiles.length > 0) {
      setError('File type not allowed or file too large (max 25MB).')
      setShowErrorModal(true)
      return
    }
    if (acceptedFiles.length > 0) {
      setFile(acceptedFiles[0])
      setError('')
      setShowErrorModal(false)
      showToast('File selected successfully. Fill in the document details below.', 'info')
    }
  }, [showToast])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: 1,
    maxSize: MAX_FILE_SIZE || 25 * 1024 * 1024,
    accept: ALLOWED_FILE_TYPES ? Object.fromEntries(
      (Array.isArray(ALLOWED_FILE_TYPES) ? ALLOWED_FILE_TYPES : []).map(t => [t, []])
    ) : undefined,
  })

  const pollExtraction = async (docId) => {
    setExtracting(true)
    let attempts = 0
    const maxAttempts = 30 // 60 seconds max
    const poll = setInterval(async () => {
      try {
        const doc = await documentService.getDocumentById(docId)
        const data = doc.data || doc
        if (data.isTextExtracted) {
          clearInterval(poll)
          setExtracting(false)
          setExtractionDone(true)
          showToast('Document uploaded and text extracted successfully!', 'success')
          setTimeout(() => navigate('/documents'), 2000)
        }
      } catch { /* ignore poll errors */ }
      attempts++
      if (attempts >= maxAttempts) {
        clearInterval(poll)
        setExtracting(false)
        setExtractionDone(true)
        showToast('Upload complete. Text extraction may still be processing.', 'info')
        setTimeout(() => navigate('/documents'), 2000)
      }
    }, 2000)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!file) {
      setError('Please select a file to upload.')
      setShowErrorModal(true)
      return
    }
    if (!formData.departmentId) {
      setError('Please select a department.')
      setShowErrorModal(true)
      return
    }

    setUploading(true)
    setUploadProgress(0)
    setExtracting(false)
    setExtractionDone(false)
    setError('')
    setShowErrorModal(false)
    try {
      const data = new FormData()
      data.append('file', file)
      data.append('description', formData.description)
      data.append('departmentId', formData.departmentId)
      if (formData.documentType) data.append('documentType', formData.documentType)
      if (formData.priority) data.append('priority', formData.priority)
      if (formData.tags) data.append('tags', formData.tags)
      data.append('slaAckHours', formData.slaAckHours)
      data.append('slaEsc1', formData.slaEsc1)
      data.append('slaEsc2', formData.slaEsc2)
      data.append('slaEsc3', formData.slaEsc3)

      const result = await documentService.uploadDocument(data, (progress) => setUploadProgress(progress))
      setUploadProgress(100)

      // Start extraction polling
      const docId = result?.data?.id || result?.id
      if (docId) {
        pollExtraction(docId)
      } else {
        setExtracting(false)
        setExtractionDone(true)
        showToast('Document uploaded successfully!', 'success')
        setTimeout(() => navigate('/documents'), 2000)
      }
    } catch (err) {
      showToast('Upload failed. Please check your file and try again.', 'error')
      setError(err.response?.data?.message || 'Upload failed. Please try again.')
      setShowErrorModal(true)
      setUploading(false)
    }
  }

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  return (
    <div className="animate-fade-in">
      <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
        <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Upload Document</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* Left: File + Metadata */}
          <div className="lg:col-span-2 space-y-4">
            {/* Dropzone */}
            <div className="card-metro">
              <h2 className="section-title">Select File</h2>
              <div {...getRootProps()} className={`upload-zone ${isDragActive ? 'upload-zone-active' : ''}`}>
                <input {...getInputProps()} />
                {file ? (
                  <div className="flex items-center justify-center gap-3">
                    <FaFileAlt className="text-2xl" style={{ color: '#0B3C5D' }} />
                    <div className="text-left">
                      <p className="text-sm font-medium text-gray-800">{file.name}</p>
                      <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                    </div>
                    <button type="button" onClick={(e) => { e.stopPropagation(); setFile(null) }} className="p-1 text-gray-400 hover:text-red-500">
                      <FaTimes />
                    </button>
                  </div>
                ) : (
                  <div>
                    <FaCloudUploadAlt className="text-4xl text-gray-400 mx-auto mb-2" />
                    <p className="text-sm text-gray-600">
                      {isDragActive ? 'Drop file here...' : 'Drag & drop a file, or click to browse'}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">Max file size: 25MB</p>
                  </div>
                )}
              </div>
            </div>

            {/* Metadata */}
            <div className="card-metro">
              <h2 className="section-title">Document Details</h2>
              <div className="space-y-4">
                <div>
                  <label className="label-metro">Department <span className="required-asterisk">*</span></label>
                  <CustomDropdown options={departmentOptions} value={formData.departmentId} onChange={val => { setFormData({ ...formData, departmentId: val }); setShowErrorModal(false) }} required />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="label-metro">Document Type</label>
                    <CustomDropdown options={docTypeOptions} value={formData.documentType} onChange={val => setFormData({ ...formData, documentType: val })} />
                  </div>
                  <div>
                    <label className="label-metro">Priority</label>
                    <CustomDropdown options={priorityOptions} value={formData.priority} onChange={val => setFormData({ ...formData, priority: val })} />
                  </div>
                </div>
                <div>
                  <label className="label-metro">Description</label>
                  <textarea className="input-metro" rows={3} placeholder="Brief description of the document..." value={formData.description} onChange={e => setFormData({ ...formData, description: e.target.value })} />
                </div>
                <div>
                  <label className="label-metro">Tags</label>
                  <input className="input-metro" placeholder="Comma-separated (e.g., safety, audit, quarterly)" value={formData.tags} onChange={e => setFormData({ ...formData, tags: e.target.value })} />
                </div>
              </div>
            </div>
          </div>

          {/* Right: SLA Config */}
          <div className="space-y-4">
            <div className="card-metro">
              <h2 className="section-title">SLA Configuration</h2>
              <p className="text-xs text-gray-500 mb-4">Set the acknowledgement and escalation timelines for this document.</p>
              <div className="space-y-3">
                <div>
                  <label className="label-metro">Acknowledgement Deadline (hours)</label>
                  <input type="number" className="input-metro" value={formData.slaAckHours} onChange={e => { const v = parseInt(e.target.value); setFormData({ ...formData, slaAckHours: isNaN(v) || v < 0 ? 0 : v }) }} min={0} />
                </div>
                <div>
                  <label className="label-metro">Escalation Level 1 (hours)</label>
                  <input type="number" className="input-metro" value={formData.slaEsc1} onChange={e => { const v = parseInt(e.target.value); setFormData({ ...formData, slaEsc1: isNaN(v) || v < 0 ? 0 : v }) }} min={0} />
                </div>
                <div>
                  <label className="label-metro">Escalation Level 2 (hours)</label>
                  <input type="number" className="input-metro" value={formData.slaEsc2} onChange={e => { const v = parseInt(e.target.value); setFormData({ ...formData, slaEsc2: isNaN(v) || v < 0 ? 0 : v }) }} min={0} />
                </div>
                <div>
                  <label className="label-metro">SLA Violation (hours)</label>
                  <input type="number" className="input-metro" value={formData.slaEsc3} onChange={e => { const v = parseInt(e.target.value); setFormData({ ...formData, slaEsc3: isNaN(v) || v < 0 ? 0 : v }) }} min={0} />
                </div>
              </div>
            </div>

            {/* Submit */}
            <div className="flex flex-col gap-2">
              <button type="submit" className="btn-metro-primary w-full justify-center" disabled={uploading || !file}>
                {uploading ? 'Uploading...' : <><FaCloudUploadAlt /> Upload Document</>}
              </button>
              <button type="button" className="btn-metro-reset w-full justify-center" onClick={() => navigate('/documents')}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      </form>

      {/* Upload + Extraction Progress Popup */}
      {(uploading || extracting || extractionDone) && (
        <div className="modal-overlay">
          <div className="modal-content card-metro" style={{ maxWidth: '400px' }}>
            {/* Phase 1: Uploading */}
            {uploading && !extracting && !extractionDone && (
              <div>
                <div style={{ padding: '20px' }}>
                  <h3 className="section-title" style={{ margin: 0 }}>Upload in Progress</h3>
                  <p className="text-sm text-gray-700 mb-4 truncate font-medium">{file?.name || 'document'}</p>
                  <div className="w-full bg-gray-200 rounded-full h-2 mb-3">
                    <div className="h-2 rounded-full transition-all duration-300" style={{ width: `${uploadProgress}%`, backgroundColor: uploadProgress === 100 ? '#1E7E34' : '#0B3C5D' }}></div>
                  </div>
                  <p className="text-xs font-medium" style={{ color: uploadProgress === 100 ? '#1E7E34' : '#0B3C5D' }}>{uploadProgress === 100 ? 'Upload Complete!' : `${uploadProgress}% Uploading...`}</p>
                </div>
              </div>
            )}
            {/* Phase 2: Extracting text */}
            {extracting && (
              <div>
                <div style={{ padding: '20px' }}>
                  <h3 className="section-title" style={{ margin: 0 }}>Processing Text</h3>
                  <p className="text-sm text-gray-700 mb-4 font-medium">{file?.name || 'document'}</p>
                  <div className="mx-auto mb-4" style={{ width: '40px', height: '40px' }}>
                    <svg className="animate-spin" viewBox="0 0 50 50" style={{ width: '100%', height: '100%' }}>
                      <circle cx="25" cy="25" r="20" fill="none" stroke="#E5E9EC" strokeWidth="4" />
                      <circle cx="25" cy="25" r="20" fill="none" stroke="#0B3C5D" strokeWidth="4" strokeDasharray="100" strokeDashoffset="60" strokeLinecap="round" />
                    </svg>
                  </div>
                  <p className="text-xs text-gray-500">Processing document content...</p>
                </div>
              </div>
            )}
            {/* Phase 3: Done */}
            {extractionDone && !extracting && (
              <div>
                <div style={{ padding: '20px' }}>
                  <h3 className="section-title" style={{ margin: 0 }}>Upload Complete</h3>
                  <p className="text-sm text-gray-700 mb-3 font-medium">{file?.name || 'document'}</p>
                  <p className="text-xs text-gray-500">Document uploaded and processed successfully. Redirecting...</p>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

    </div>
  )
}

export default UploadPage
