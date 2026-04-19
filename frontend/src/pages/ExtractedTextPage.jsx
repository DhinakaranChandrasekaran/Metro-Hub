import { useState, useEffect, useMemo, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { FaDownload, FaFileAlt } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import documentService from '../services/documentService'

// HIGHLIGHT PATTERNS
const DEADLINE_RE = /\b(deadline|due date|expir\w*|last date|final date|overdue)\b/gi
const DATE_RE = /\b(\d{1,2}[\/-]\d{1,2}[\/-]\d{2,4}|\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\w*\s+\d{4})\b/gi
const IMPORTANT_RE = /\b(important|urgent|critical|mandatory|required|compulsory|warning|notice|attention|safety|hazard|violation|penalty|fine)\b/gi

const ExtractedTextPage = () => {
    const { id } = useParams()
    const navigate = useNavigate()
    const { showToast } = useToast()
    const [doc, setDoc] = useState(null)
    const [extractedText, setExtractedText] = useState('')
    const [loading, setLoading] = useState(true)
    const reportRef = useRef(null)

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true)
            try {
                const docRes = await documentService.getDocumentById(id)
                const docData = docRes.data || docRes
                setDoc(docData)
                const blob = await documentService.downloadExtracted(id)
                const text = await blob.text()
                setExtractedText(text || '')
            } catch {
                showToast('Failed to load extracted text.', 'error')
                navigate(`/documents/${id}`)
            } finally { setLoading(false) }
        }
        fetchData()
    }, [id])

    // Parse text into structured blocks
    const parsedBlocks = useMemo(() => {
        if (!extractedText) return []
        const lines = extractedText.split(/\n+/).filter(l => l.trim())
        return lines.map(line => {
            const trimmed = line.trim()
            if (trimmed.length < 80 && trimmed === trimmed.toUpperCase() && /[A-Z]/.test(trimmed)) {
                return { type: 'heading', text: trimmed }
            }
            if (trimmed.endsWith(':') && trimmed.length < 80) {
                return { type: 'subheading', text: trimmed }
            }
            return { type: 'paragraph', text: trimmed }
        })
    }, [extractedText])

    // Highlight keywords in text
    const highlightText = (text) => {
        let parts = [{ text, type: 'normal' }]
        const applyPattern = (parts, regex, hlType) => {
            const result = []
            parts.forEach(part => {
                if (part.type !== 'normal') { result.push(part); return }
                let lastIdx = 0
                const matches = [...part.text.matchAll(regex)]
                matches.forEach(m => {
                    if (m.index > lastIdx) result.push({ text: part.text.slice(lastIdx, m.index), type: 'normal' })
                    result.push({ text: m[0], type: hlType })
                    lastIdx = m.index + m[0].length
                })
                if (lastIdx < part.text.length) result.push({ text: part.text.slice(lastIdx), type: 'normal' })
            })
            return result
        }
        parts = applyPattern(parts, DEADLINE_RE, 'deadline')
        parts = applyPattern(parts, DATE_RE, 'date')
        parts = applyPattern(parts, IMPORTANT_RE, 'important')
        return parts.map((p, i) => {
            if (p.type === 'deadline') return <span key={i} className="hl-deadline">{p.text}</span>
            if (p.type === 'date') return <span key={i} className="hl-date">{p.text}</span>
            if (p.type === 'important') return <span key={i} className="hl-important">{p.text}</span>
            return <span key={i}>{p.text}</span>
        })
    }

    // Print only the report section, not the full page
    const handleDownloadPdf = async () => {
        const reportEl = reportRef.current
        if (!reportEl) return

        try {
            const printWindow = window.open('', '_blank')
            const fullHtml = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${doc?.fileName || 'Document'} — Extracted Text</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background: #fff;
      color: #2c3e50;
      line-height: 1.6;
    }
    .extracted-viewer {
      background: #fff;
      border-radius: 10px;
      overflow: hidden;
    }
    .report-header {
      background: linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%);
      padding: 24px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: white;
    }
    .report-header h2 {
      margin: 0;
      font-size: 1.3em;
      font-weight: 600;
    }
    .report-header p {
      margin: 4px 0 0;
      opacity: 0.85;
      font-size: 0.85em;
    }
    .report-header .char-count {
      background: rgba(255,255,255,0.15);
      border-radius: 6px;
      padding: 6px 14px;
      border: 1px solid rgba(255,255,255,0.2);
      font-size: 0.78em;
      font-weight: 500;
    }
    .highlight-legend {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      padding: 12px 28px;
      background: #fafafa;
      border-bottom: 1px solid #f0f0f0;
      font-size: 0.78em;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .content-body {
      padding: 24px 28px;
    }
    .hl-deadline { background: #ffe0e0; color: #c0392b; font-weight: 700; padding: 2px 6px; border-radius: 3px; }
    .hl-date { background: #fff3e0; color: #e65100; font-weight: 600; padding: 2px 6px; border-radius: 3px; }
    .hl-important { background: #e3f0ff; color: #0B3C5D; font-weight: 700; padding: 2px 6px; border-radius: 3px; }
    h3 {
      color: #0B3C5D;
      font-weight: 700;
      font-size: 1.05em;
      margin: 1.4em 0 0.5em;
      border-bottom: 2px solid #0B3C5D;
      padding-bottom: 4px;
    }
    h4 {
      color: #1a4a6b;
      font-weight: 600;
      font-size: 0.95em;
      margin: 1.1em 0 0.3em;
    }
    p {
      margin: 0.4em 0;
      text-align: justify;
      line-height: 1.75;
      font-size: 0.92em;
      color: #34495e;
    }
    .report-footer {
      text-align: center;
      padding: 14px 28px;
      border-top: 1px solid #e0e0e0;
      font-size: 0.75em;
      color: #999;
    }
    @media print {
      @page {
        size: A4;
        margin: 10mm;
      }
      body { padding: 0; }
      .extracted-viewer { border-radius: 0; box-shadow: none; }
    }
  </style>
</head>
<body>
  <div class="extracted-viewer">
    ${reportEl.innerHTML}
  </div>
</body>
</html>`

            printWindow.document.write(fullHtml)
            printWindow.document.close()
            printWindow.focus()

            setTimeout(() => {
                printWindow.print()
                setTimeout(() => printWindow.close(), 500)
                showToast('PDF generated and opened for download.', 'success')
            }, 800)
        } catch {
            showToast('Failed to generate PDF.', 'error')
        }
    }

    const formatDate = () => new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })

    if (loading) return (
        <div className="animate-fade-in">
            <div className="text-center py-20 text-gray-500">Loading extracted text...</div>
        </div>
    )

    return (
        <>
            <style>{`
                .hl-deadline { background: #ffe0e0; color: #c0392b; font-weight: 700; padding: 1px 5px; border-radius: 3px; }
                .hl-date { background: #fff3e0; color: #e65100; font-weight: 600; padding: 1px 5px; border-radius: 3px; }
                .hl-important { background: #e3f0ff; color: #0B3C5D; font-weight: 700; padding: 1px 5px; border-radius: 3px; }
            `}</style>

            <div className="animate-fade-in">
                {/* ── Page Header ── matches all pages header structure */}
                <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                    <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Extracted Document</h1>
                    <p style={{ color: 'rgba(255,255,255,0.8)', fontSize: '0.9em', margin: '4px 0 0' }}>{doc?.fileName || 'Extracted Text'}</p>
                </div>

                {/* ── Action Buttons ── */}
                <div className="card-metro mb-4" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 20px' }}>
                    <p style={{ margin: 0, fontSize: '0.95em', color: '#1F2933', fontWeight: 500 }}>
                        Review key information extracted from the document — download as PDF for offline access.
                    </p>
                    <button
                        onClick={handleDownloadPdf}
                        className="flex items-center gap-2 px-4 py-2 text-sm rounded text-white font-semibold transition-colors hover:opacity-90"
                        style={{ backgroundColor: '#0B3C5D' }}
                    >
                        <FaDownload /> Save as PDF
                    </button>
                </div>

                {/* ── Report Section ── full width */}
                <div ref={reportRef} className="extracted-viewer" style={{
                    background: '#fff',
                    borderRadius: '10px',
                    boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
                    overflow: 'hidden',
                }}>
                    {/* Blue header bar - matches table header style */}
                    <div style={{
                        background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)',
                        padding: '20px 28px',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                    }}>
                        <div>
                            <h2 style={{ margin: 0, fontSize: '1.15em', fontWeight: 600, color: '#fff' }}>
                                {doc?.fileName || 'Document'}
                            </h2>
                            <p style={{ margin: '4px 0 0', opacity: 0.75, fontSize: '0.82em', color: '#fff' }}>
                                Extracted Text — MetroHub Document Management
                            </p>
                        </div>
                        <div style={{
                            backgroundColor: 'rgba(255,255,255,0.12)',
                            borderRadius: '6px',
                            padding: '6px 14px',
                            border: '1px solid rgba(255,255,255,0.2)',
                        }}>
                            <span style={{ color: '#fff', fontSize: '0.78em', fontWeight: 500 }}>
                                {extractedText.length.toLocaleString()} chars
                            </span>
                        </div>
                    </div>

                    {/* Highlight legend */}
                    <div style={{
                        display: 'flex', gap: '16px', flexWrap: 'wrap',
                        padding: '10px 28px', background: '#fafafa',
                        borderBottom: '1px solid #f0f0f0', fontSize: '0.78em',
                    }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <span className="hl-deadline">Deadline</span> Deadline / Due dates
                        </span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <span className="hl-date">01 Jan 2026</span> Dates
                        </span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <span className="hl-important">Important</span> Critical keywords
                        </span>
                    </div>

                    {/* Content body */}
                    <div style={{ padding: '24px 28px' }}>
                        {parsedBlocks.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#999', padding: '40px 0' }}>
                                No extracted text available for this document.
                            </p>
                        ) : (
                            parsedBlocks.map((block, i) => {
                                if (block.type === 'heading') return (
                                    <h3 key={i} style={{
                                        color: '#0B3C5D', fontWeight: 700, fontSize: '1.05em',
                                        margin: '1.4em 0 0.5em', borderBottom: '2px solid #0B3C5D',
                                        paddingBottom: '4px',
                                    }}>
                                        {highlightText(block.text)}
                                    </h3>
                                )
                                if (block.type === 'subheading') return (
                                    <h4 key={i} style={{
                                        color: '#1a4a6b', fontWeight: 600, fontSize: '0.95em',
                                        margin: '1.1em 0 0.3em',
                                    }}>
                                        {highlightText(block.text)}
                                    </h4>
                                )
                                return (
                                    <p key={i} style={{
                                        margin: '0.4em 0', textAlign: 'justify',
                                        lineHeight: 1.75, fontSize: '0.92em', color: '#34495e',
                                    }}>
                                        {highlightText(block.text)}
                                    </p>
                                )
                            })
                        )}
                    </div>

                    {/* Footer */}
                    <div style={{
                        textAlign: 'center', padding: '14px 28px',
                        borderTop: '1px solid #e0e0e0', fontSize: '0.75em', color: '#999',
                    }}>
                        Generated by MetroHub • {formatDate()}
                    </div>
                </div>
            </div>
        </>
    )
}

export default ExtractedTextPage
