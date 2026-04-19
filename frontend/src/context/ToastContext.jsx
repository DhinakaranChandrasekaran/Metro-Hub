import { createContext, useContext, useState, useCallback, useEffect } from 'react'

// TOAST CONTEXT — Simple centered popup (no border, no icons)
const ToastContext = createContext(null)

export const useToast = () => {
    const context = useContext(ToastContext)
    if (!context) throw new Error('useToast must be used within ToastProvider')
    return context
}

export const ToastProvider = ({ children }) => {
    const [toast, setToast] = useState(null)

    const showToast = useCallback((message, type = 'success', duration = 5000, position = 'center') => {
        setToast({ message, type, id: Date.now(), position })
        setTimeout(() => setToast(null), duration)
    }, [])

    const hideToast = () => setToast(null)

    return (
        <ToastContext.Provider value={{ showToast }}>
            {children}
            {toast && <ToastPopup toast={toast} onClose={hideToast} />}
        </ToastContext.Provider>
    )
}

const ToastPopup = ({ toast, onClose }) => {
    const [visible, setVisible] = useState(false)

    useEffect(() => {
        setTimeout(() => setVisible(true), 50)
    }, [])

    const config = {
        success: { bg: '#E8F5E9', color: '#1E7E34', title: 'Success' },
        error: { bg: '#FFEBEE', color: '#C62828', title: 'Error' },
        warning: { bg: '#FFF3E0', color: '#E65100', title: 'Attention' },
        info: { bg: '#E8EEF3', color: '#0B3C5D', title: 'Information' },
    }[toast.type] || { bg: '#E8F5E9', color: '#1E7E34', title: 'Success' }

    // Center for success, bottom-right for errors
    const isCenter = toast.position === 'center'
    const isBottomRight = toast.position === 'bottom-right'

    if (isBottomRight) {
        return (
            <div className="fixed bottom-6 right-6 z-[9999] max-w-md" style={{ display: 'block' }}>
                <div
                    className="bg-white rounded shadow-xl p-5 transition-all duration-300"
                    style={{
                        transform: visible ? 'translateX(0)' : 'translateX(450px)',
                        opacity: visible ? 1 : 0,
                        width: '100%'
                    }}
                >
                    <div className="flex items-start justify-between mb-2">
                        <p className="text-sm font-semibold" style={{ color: config.color }}>{config.title}</p>
                        <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-lg leading-none ml-3">×</button>
                    </div>
                    <p className="text-sm text-gray-600 leading-relaxed">{toast.message}</p>
                    <div className="mt-3 h-0.5 rounded-full overflow-hidden bg-gray-100">
                        <div className="h-full rounded-full" style={{ backgroundColor: config.color, animation: 'toast-progress 5s linear forwards' }}></div>
                    </div>
                    <style>{`@keyframes toast-progress { from { width: 100%; } to { width: 0%; } }`}</style>
                </div>
            </div>
        )
    }

    // Center position (success)
    return (
        <div className="fixed inset-0 flex items-center justify-center z-[9999]" style={{ backgroundColor: 'rgba(0,0,0,0.12)' }}>
            <div
                className="bg-white rounded shadow-xl p-5 max-w-sm w-full mx-4 transition-all duration-300"
                style={{
                    transform: visible ? 'scale(1) translateY(0)' : 'scale(0.9) translateY(-10px)',
                    opacity: visible ? 1 : 0,
                }}
            >
                <div className="flex items-start justify-between mb-2">
                    <p className="text-sm font-semibold" style={{ color: config.color }}>{config.title}</p>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-lg leading-none ml-3">×</button>
                </div>
                <p className="text-sm text-gray-600 leading-relaxed">{toast.message}</p>
                <div className="mt-3 h-0.5 rounded-full overflow-hidden bg-gray-100">
                    <div className="h-full rounded-full" style={{ backgroundColor: config.color, animation: 'toast-progress 5s linear forwards' }}></div>
                </div>
                <style>{`@keyframes toast-progress { from { width: 100%; } to { width: 0%; } }`}</style>
            </div>
        </div>
    )
}

export default ToastContext
