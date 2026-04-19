import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaSearch, FaDownload, FaFileAlt, FaEye, FaTrash, FaTimes, FaExclamationTriangle } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import CustomDropdown from '../components/CustomDropdown'
import ErrorModal from '../components/ErrorModal'
import documentService from '../services/documentService'

const DocumentsPage = () => {
  const navigate = useNavigate()
  const { hasPermission } = useAuth()
  const { showToast } = useToast()
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [priorityFilter, setPriorityFilter] = useState('ALL')
  const [deleteModal, setDeleteModal] = useState({ show: false, docId: null, docName: '' })
  const [deleting, setDeleting] = useState(false)
  const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })
  const debounceTimer = useRef(null)

  const handleSearch = useCallback(async (searchVal, typeVal, prioVal, pageVal) => {
    setLoading(true)
    try {
      const params = { page: pageVal || 0, size: 15 }
      if (searchVal) params.keyword = searchVal
      if (typeVal !== 'ALL') params.documentType = typeVal
      if (prioVal !== 'ALL') params.priority = prioVal

      let responseData
      if (searchVal || typeVal !== 'ALL' || prioVal !== 'ALL') {
        const response = await documentService.searchDocuments(params)
        responseData = response || {}
      } else {
        responseData = await documentService.getAllDocuments(pageVal || 0, 15)
      }

      const docs = responseData.documents || responseData.content || []
      const pages = responseData.totalPages || 1

      setDocuments(Array.isArray(docs) ? docs : [])
      setTotalPages(pages)
    } catch (err) {
      showToast('Search failed.', 'error')
    }
    finally { setLoading(false) }
  }, [showToast])

  // Debounced search effect
  useEffect(() => {
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => {
      handleSearch(search, typeFilter, priorityFilter, page)
    }, 300)
    return () => clearTimeout(debounceTimer.current)
  }, [search, typeFilter, priorityFilter, page, handleSearch])

  // Re-fetch documents when page regains focus
  useEffect(() => {
    const handleRefresh = () => {
      setPage(0)
      setSearch('')
      setTypeFilter('ALL')
      setPriorityFilter('ALL')
      handleSearch('', 'ALL', 'ALL', 0)
    }
    window.addEventListener('focus', handleRefresh)
    window.addEventListener('metrohub:refresh-documents', handleRefresh)
    return () => {
      window.removeEventListener('focus', handleRefresh)
      window.removeEventListener('metrohub:refresh-documents', handleRefresh)
    }
  }, [handleSearch])

  const handleDownload = async (e, doc, type) => {
    e.stopPropagation()
    try {
      const blob = type === 'original' ? await documentService.downloadOriginal(doc.id) : await documentService.downloadExtracted(doc.id)
      if (type === 'extracted') {
        // Build formatted HTML and trigger print-to-PDF download
        const text = await blob.text()
        const html = buildExtractedPdfHtml(text, doc.fileName || 'Document')
        const printFrame = document.createElement('iframe')
        printFrame.style.cssText = 'position:fixed;left:-9999px;top:-9999px;width:900px;height:1200px;'
        document.body.appendChild(printFrame)
        printFrame.contentDocument.write(html)
        printFrame.contentDocument.close()
        setTimeout(() => {
          printFrame.contentWindow.focus()
          printFrame.contentWindow.print()
          setTimeout(() => document.body.removeChild(printFrame), 2000)
        }, 500)
        showToast('Print dialog opened — select "Save as PDF" to download.', 'info')
      } else {
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a'); a.href = url
        a.download = doc.fileName || 'document'
        a.click(); URL.revokeObjectURL(url)
        showToast('Original downloaded.', 'success')
      }
    } catch { showToast('Download failed.', 'error') }
  }

  const buildExtractedPdfHtml = (text, title) => {
    const lines = text.split(/\n+/).filter(l => l.trim())
    let body = ''
    const deadlineRe = /\b(deadline|due date|expir\w*|last date|final date|overdue)\b/gi
    const dateRe = /\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\w*\s+\d{4})\b/gi
    const importantRe = /\b(important|urgent|critical|mandatory|required|compulsory|warning|notice|attention|safety|hazard|violation|penalty|fine)\b/gi
    lines.forEach(line => {
      const trimmed = line.trim()
      if (trimmed.length < 80 && (trimmed === trimmed.toUpperCase() && /[A-Z]/.test(trimmed))) {
        body += `<h3 class="doc-heading">${trimmed}</h3>\n`
      } else if (trimmed.endsWith(':') && trimmed.length < 80) {
        body += `<h4 class="doc-subheading">${trimmed}</h4>\n`
      } else {
        let highlighted = trimmed
          .replace(deadlineRe, '<span class="hl-deadline">$1</span>')
          .replace(dateRe, '<span class="hl-date">$1</span>')
          .replace(importantRe, '<span class="hl-important">$1</span>')
        body += `<p class="doc-para">${highlighted}</p>\n`
      }
    })
    return `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>${title} — Extracted Text</title>
<style>
  @media print { .no-print { display: none !important; } body { margin: 0; } }
  * { box-sizing: border-box; }
  body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 900px; margin: 40px auto; padding: 30px 40px; background: #f8f9fa; color: #2c3e50; }
  .header { background: linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%); color: white; padding: 24px 30px; border-radius: 10px 10px 0 0; margin: -30px -40px 0 -40px; display: flex; justify-content: space-between; align-items: center; }
  .header h1 { margin:0; font-size:1.3em; font-weight:600; } .header p { margin:4px 0 0; opacity:0.8; font-size:0.85em; }
  .print-btn { background: rgba(255,255,255,0.15); color: white; border: 1px solid rgba(255,255,255,0.3); padding: 8px 16px; border-radius: 6px; cursor: pointer; font-size: 0.82em; font-weight: 600; }
  .print-btn:hover { background: rgba(255,255,255,0.25); }
  .content { background: white; padding: 30px 35px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); margin: 0 -40px; }
  .doc-heading { color: #0B3C5D; font-weight: 700; font-size:1.05em; margin: 1.5em 0 0.5em; border-bottom: 2px solid #E5E9EC; padding-bottom: 4px; }
  .doc-subheading { color: #1a4a6b; font-weight: 600; font-size: 0.95em; margin: 1.2em 0 0.3em; }
  .doc-para { margin: 0.4em 0; text-align: justify; line-height: 1.75; font-size: 0.92em; color: #34495e; }
  .hl-deadline { background: #ffe0e0; color: #c0392b; font-weight: 700; padding: 1px 4px; border-radius: 3px; }
  .hl-date { background: #fff3e0; color: #e65100; font-weight: 600; padding: 1px 4px; border-radius: 3px; }
  .hl-important { background: #e3f0ff; color: #0B3C5D; font-weight: 700; padding: 1px 4px; border-radius: 3px; }
  .footer { text-align: center; margin-top: 30px; padding-top: 15px; border-top: 1px solid #e0e0e0; font-size: 0.75em; color: #999; }
  .legend { display: flex; gap: 16px; flex-wrap: wrap; padding: 12px 20px; background: #fafafa; border-radius: 6px; margin-bottom: 16px; font-size: 0.78em; }
  .legend span { display: flex; align-items: center; gap: 4px; }
</style></head><body>
<div class="header">
  <div><h1>📄 ${title}</h1><p>Extracted Text — MetroHub Document Management</p></div>
  <button class="print-btn no-print" onclick="window.print()">🖨️ Save as PDF</button>
</div>
<div class="content">
  <div class="legend no-print">
    <span><span class="hl-deadline">Deadline</span> Deadline / Due dates</span>
    <span><span class="hl-date">01 Jan 2026</span> Dates</span>
    <span><span class="hl-important">Important</span> Critical keywords</span>
  </div>
  ${body}
</div>
<div class="footer">Generated by MetroHub • ${new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</div>
</body></html>`
  }

  const confirmDelete = async () => {
    setDeleting(true)
    try {
      await documentService.deleteDocument(deleteModal.docId)
      handleSearch(search, typeFilter, priorityFilter, page)
      showToast('Document deleted successfully.', 'success')
    } catch { showToast('Delete failed.', 'error') }
    finally { setDeleting(false); setDeleteModal({ show: false, docId: null, docName: '' }) }
  }

  const formatDate = d => d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'
  const types = ['ALL', 'JOB_CARD', 'INVOICE', 'POLICY', 'SAFETY_CIRCULAR', 'LEGAL_NOTICE', 'CONTRACT', 'MANUAL', 'REPORT', 'MEMO', 'OTHER']
  const priorities = ['ALL', 'HIGH', 'MEDIUM', 'LOW']

  const typeOptions = types.map(t => ({ value: t, label: t === 'ALL' ? 'All Types' : t.replace(/_/g, ' ') }))
  const priorityOptions = priorities.map(p => ({ value: p, label: p === 'ALL' ? 'All Priorities' : p }))

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '20px 24px', borderRadius: '12px' }}>
        <h1 className="page-title" style={{ color: 'white', margin: 0, fontSize: '24px', fontWeight: '600' }}>Document Repository</h1>
      </div>

      {/* Search & Filters Card */}
      <div className="card-metro mb-4">
        <div className="flex gap-3 flex-wrap items-center">
          <div className="relative flex-1 min-w-[250px]">
            <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs" />
            <input
              className="input-metro pl-8 w-full"
              placeholder="Search documents..."
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(0) }}
            />
          </div>
          <div className="w-44">
            <CustomDropdown
              options={typeOptions}
              value={typeFilter}
              onChange={e => { setTypeFilter(e); setPage(0) }}
              placeholder="Select Type"
            />
          </div>
          <div className="w-44">
            <CustomDropdown
              options={priorityOptions}
              value={priorityFilter}
              onChange={e => { setPriorityFilter(e); setPage(0) }}
              placeholder="Select Priority"
            />
          </div>
        </div>
      </div>

      {/* Table Card */}
      <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
        <div className="overflow-x-auto">
          {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading documents...</div> : (
            <table className="w-full text-sm">
              <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '4%', textAlign: 'center' }}>S.No</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '28%', textAlign: 'center' }}>Document Name</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Type</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Department</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Priority</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Date</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '6%', textAlign: 'center' }}>Status</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '11%', textAlign: 'center' }}>Downloads</th>
                <th className="py-3 px-4 font-medium text-white" style={{ width: '11%', textAlign: 'center' }}>Actions</th>
              </tr></thead>
              <tbody>{documents.length === 0 ? <tr><td colSpan="9" className="py-8 text-center text-gray-400 text-sm">No documents found.</td></tr> :
                documents.map((doc, idx) => (
                  <tr key={doc.id} className="border-b cursor-pointer hover:bg-blue-50 transition-colors" style={{ borderColor: '#E5E7EB' }} onClick={() => navigate(`/documents/${doc.id}`)}>
                    <td className="py-2.5 px-4 text-gray-600 text-xs font-medium text-center">{idx + 1}</td>
                    <td className="py-2.5 px-4 font-medium text-center" style={{ color: '#0B3C5D' }}><FaFileAlt className="inline mr-2 text-gray-400" />{doc.originalFileName || doc.fileName || 'Unnamed'}</td>
                    <td className="py-2.5 px-4 text-xs text-gray-600 text-center">{(doc.documentType || '-').replace(/_/g, ' ')}</td>
                    <td className="py-2.5 px-4 text-sm text-gray-600 text-center">{doc.departmentName || '-'}</td>
                    <td className="py-2.5 px-4 text-center"><span className={`px-2.5 py-1 rounded text-xs font-medium ${doc.priority === 'HIGH' ? 'bg-orange-100 text-orange-700' : doc.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' : 'bg-gray-100 text-gray-600'}`}>{doc.priority || 'N/A'}</span></td>
                    <td className="py-2.5 px-4 text-xs text-gray-600 text-center">{formatDate(doc.uploadDate || doc.createdAt)}</td>
                    <td className="py-2.5 px-4 text-center"><span className={`text-xs font-medium ${doc.status === 'ACTIVE' ? 'text-green-700' : doc.status === 'PENDING_REVIEW' ? 'text-orange-600' : 'text-gray-500'}`}>{doc.status || '-'}</span></td>
                    <td className="py-2.5 px-4 text-center">
                      <div className="flex gap-2 justify-center">
                        <button onClick={(e) => handleDownload(e, doc, 'original')} className="p-1.5 rounded hover:bg-blue-100 transition-colors" title="Download Original" style={{ color: '#0B3C5D' }}><FaDownload className="text-sm" /></button>
                        {doc.isTextExtracted && <button onClick={(e) => handleDownload(e, doc, 'extracted')} className="p-1.5 rounded hover:bg-green-100 transition-colors" title="Download Extracted" style={{ color: '#1E7E34' }}><FaFileAlt className="text-sm" /></button>}
                      </div>
                    </td>
                    <td className="py-2.5 px-4 text-center">
                      <div className="flex gap-2 justify-center">
                        <button onClick={(e) => { e.stopPropagation(); navigate(`/documents/${doc.id}`) }} className="p-1.5 rounded hover:bg-blue-100 transition-colors" title="View Details" style={{ color: '#0B3C5D' }}><FaEye className="text-sm" /></button>
                        {hasPermission('deleteDocuments') && <button onClick={(e) => { e.stopPropagation(); if (!deleting) setDeleteModal({ show: true, docId: doc.id, docName: doc.fileName || 'this document' }) }} disabled={deleting} className="p-1.5 rounded hover:bg-red-100 transition-colors text-red-500" style={{ opacity: deleting ? 0.5 : 1, cursor: deleting ? 'not-allowed' : 'pointer' }}><FaTrash className="text-sm" /></button>}
                      </div>
                    </td>
                  </tr>
                ))}</tbody>
            </table>)}
        </div>
      </div>
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-4">{Array.from({ length: totalPages }, (_, i) => (
          <button key={i} onClick={() => setPage(i)} className={`px-3 py-1 text-xs rounded border ${page === i ? 'text-white border-transparent' : 'text-gray-600 border-gray-200'}`} style={page === i ? { backgroundColor: '#0B3C5D' } : {}}>{i + 1}</button>
        ))}</div>
      )}

      {/* Delete Confirmation Modal - Notifications Style */}
      {deleteModal.show && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '500px' }}>
            <div style={{ padding: '20px' }}>
              <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Delete Document</h3>
              </div>

              <div className="space-y-0">
                <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                  <span className="text-sm font-semibold text-gray-600">Document Name</span>
                  <p className="text-sm font-semibold text-gray-800 mt-2 break-words">{deleteModal.docName}</p>
                </div>

                <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                  <span className="text-sm font-semibold text-gray-600">Action</span>
                  <p className="text-sm text-gray-800 mt-2">
                    Permanently delete this document from storage and database
                  </p>
                </div>

                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                  <span className="text-sm font-semibold text-gray-600">Note</span>
                  <p className="text-sm text-gray-800 mt-2">
                    All acknowledgements and history will be preserved for audit purposes. This action cannot be undone.
                  </p>
                </div>
              </div>

              <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                <button
                  onClick={() => setDeleteModal({ show: false, docId: null, docName: '' })}
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
                  onClick={confirmDelete}
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
export default DocumentsPage
