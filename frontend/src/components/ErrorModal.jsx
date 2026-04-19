// Reusable Error Modal Component - Follows Website UI Design
import React from 'react'

const ErrorModal = ({ show, title, message, onClose }) => {
    if (!show) return null

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                <div style={{ padding: '20px' }}>
                    <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                        <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>
                            {title}
                        </h3>
                    </div>

                    <div className="space-y-0">
                        <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                            <span className="text-sm font-semibold text-gray-600">Error Details</span>
                            <p className="text-sm text-gray-800 mt-2 leading-relaxed">
                                {message}
                            </p>
                        </div>
                    </div>

                    <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                        <button
                            onClick={onClose}
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
                            OK
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default ErrorModal
