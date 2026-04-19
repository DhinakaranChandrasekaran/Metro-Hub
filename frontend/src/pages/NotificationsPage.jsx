import { useState, useEffect } from 'react'
import { FaCheck } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import notificationService from '../services/notificationService'

const NotificationsPage = () => {
    const { showToast } = useToast()
    const [filter, setFilter] = useState('ALL')
    const [notifications, setNotifications] = useState([])
    const [loading, setLoading] = useState(true)
    const [selectedNotif, setSelectedNotif] = useState(null)

    const typeStyles = {
        NEW_DOCUMENT: { label: 'Document', color: '#0B3C5D', bg: '#E8EEF3', priority: 'MEDIUM' },
        DEADLINE_APPROACHING: { label: 'Deadline', color: '#0B3C5D', bg: '#E8EEF3', priority: 'MEDIUM' },
        DEADLINE_TODAY: { label: 'Deadline', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        DEADLINE_OVERDUE: { label: 'Deadline', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        HIGH_PRIORITY_UPLOAD: { label: 'Document', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        ESCALATION_LEVEL_1: { label: 'Escalation', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        ESCALATION_LEVEL_2: { label: 'Escalation', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        ESCALATION_LEVEL_3: { label: 'Escalation', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
        ACKNOWLEDGEMENT_REQUIRED: { label: 'Acknowledgement', color: '#0B3C5D', bg: '#E8EEF3', priority: 'MEDIUM' },
        VIOLATION: { label: 'Violation', color: '#0B3C5D', bg: '#E8EEF3', priority: 'HIGH' },
    }
    const defaultStyle = { label: 'Notification', color: '#0B3C5D', bg: '#E8EEF3', priority: 'MEDIUM' }

    const filterTabs = [
        { key: 'ALL', label: 'All' },
        { key: 'NEW_DOCUMENT', label: 'Documents' },
        { key: 'DEADLINE', label: 'Deadlines' },
        { key: 'ESCALATION', label: 'Escalations' },
    ]

    useEffect(() => {
        fetchNotifications(true)
        // Auto-refresh every 3 seconds — per-user fresh data from backend
        const interval = setInterval(() => fetchNotifications(false), 3000)
        return () => clearInterval(interval)
    }, [])

    const fetchNotifications = async (showLoader = false) => {
        if (showLoader) setLoading(true)
        try {
            const res = await notificationService.getAll()
            const payload = res.data || res
            const list = payload.content || payload || []
            setNotifications(Array.isArray(list) ? list : [])
        } catch { showToast('Failed to load notifications.', 'error') }
        finally { if (showLoader) setLoading(false) }
    }

    const filtered = filter === 'ALL' ? notifications : notifications.filter(n => {
        const type = n.alertType || n.type || ''
        if (filter === 'NEW_DOCUMENT') {
            return type.includes('DOCUMENT') || type.includes('NEW_DOCUMENT') || type.includes('HIGH_PRIORITY')
        }
        if (filter === 'DEADLINE') return type.includes('DEADLINE')
        if (filter === 'ESCALATION') return type.includes('ESCALATION')
        return type.includes(filter)
    })
    const unreadCount = notifications.filter(n => !n.isRead && !n.read).length

    const markRead = async (id) => {
        try {
            await notificationService.markAsRead(id)
            // Refetch to ensure we get correct state from backend
            await fetchNotifications()
        } catch { showToast('Failed to mark as read.', 'error') }
    }

    const markAllRead = async () => {
        try {
            await notificationService.markAllRead()
            // Refetch to ensure all users see Super Admin's changes from backend
            await fetchNotifications()
            showToast('All marked as read.', 'success')
        } catch { showToast('Failed to mark all as read.', 'error') }
    }

    const openNotification = (notif) => {
        setSelectedNotif(notif)
        if (!notif.isRead && !notif.read) {
            markRead(notif.id)
        }
        // Auto-close after 10 seconds
        setTimeout(() => {
            setSelectedNotif(null)
        }, 10000)
    }

    const formatTime = (d) => {
        if (!d) return ''
        const diff = Date.now() - new Date(d).getTime()
        if (diff < 60000) return 'Just now'
        if (diff < 3600000) return `${Math.floor(diff / 60000)} min ago`
        if (diff < 86400000) return `${Math.floor(diff / 3600000)} hour${Math.floor(diff / 3600000) > 1 ? 's' : ''} ago`
        return `${Math.floor(diff / 86400000)} day${Math.floor(diff / 86400000) > 1 ? 's' : ''} ago`
    }

    const getNotificationTitle = (notif) => {
        // If message exists, extract first part as title (up to first : or -)
        if (notif.message) {
            const match = notif.message.match(/^([^:\-]*)/);
            if (match) return match[1].trim();
        }
        // Fallback to other fields
        return notif.title || notif.subject || notif.name || 'Notification'
    }

    const getNotificationMessage = (notif) => {
        // If message exists, extract part after : or - as actual message
        if (notif.message) {
            const colonIndex = notif.message.indexOf(':');
            const dashIndex = notif.message.indexOf('-');

            if (colonIndex > -1 && dashIndex > -1) {
                let msg = notif.message.substring(Math.min(colonIndex, dashIndex) + 1).trim();
                // Remove quotes and ensure spacing
                msg = msg.replace(/'/g, '').replace(/\s+/g, ' ').trim();
                return msg;
            } else if (colonIndex > -1) {
                let msg = notif.message.substring(colonIndex + 1).trim();
                msg = msg.replace(/'/g, '').replace(/\s+/g, ' ').trim();
                return msg;
            } else if (dashIndex > -1) {
                let msg = notif.message.substring(dashIndex + 1).trim();
                msg = msg.replace(/'/g, '').replace(/\s+/g, ' ').trim();
                return msg;
            }
            return notif.message.replace(/'/g, '').replace(/\s+/g, ' ').trim();
        }
        // Fallback to body or content
        return notif.body || notif.content || '-'
    }

    const getPriorityCircle = (priority) => {
        if (!priority) return null
        const priorityMap = {
            'HIGH': { text: 'H', label: 'High Priority' },
            'MEDIUM': { text: 'M', label: 'Medium Priority' },
            'LOW': { text: 'L', label: 'Low Priority' },
        }
        const info = priorityMap[priority]
        if (!info) return null
        return (
            <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold" style={{ backgroundColor: '#0B3C5D' }}>
                    {info.text}
                </div>
                <span className="text-xs font-semibold">{info.label}</span>
            </div>
        )
    }

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <div className="flex justify-between items-center">
                    <div>
                        <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Notifications</h1>
                        <p className="text-xs" style={{ color: 'rgba(255,255,255,0.7)', marginTop: '4px' }}>{unreadCount} unread</p>
                    </div>
                    {unreadCount > 0 && <button className="px-3 py-2 rounded text-xs font-medium text-white transition-colors" style={{ backgroundColor: 'rgba(255,255,255,0.2)', border: '1px solid rgba(255,255,255,0.3)' }} onClick={markAllRead}><FaCheck className="inline mr-1" /> Mark All</button>}
                </div>
            </div>

            <div className="card-metro mb-4">
                <div className="flex gap-2 flex-wrap">
                    {filterTabs.map(tab => (
                        <button key={tab.key} onClick={() => setFilter(tab.key)} className={`px-4 py-2 text-sm rounded border transition-colors ${filter === tab.key ? 'text-white border-transparent font-semibold' : 'text-gray-600 border-gray-200 hover:bg-gray-50'}`} style={filter === tab.key ? { backgroundColor: '#0B3C5D' } : {}}>{tab.label}</button>
                    ))}
                </div>
            </div>

            {loading ? (
                <div className="text-center py-12 text-gray-500"><p className="text-sm">Loading notifications...</p></div>
            ) : (
                <div className="space-y-3">
                    {filtered.length === 0 ? (
                        <div className="text-center py-10 text-gray-400 text-sm">No notifications found.</div>
                    ) : (
                        filtered.map((notif) => {
                            const type = notif.alertType || notif.type || ''
                            const style = typeStyles[type] || defaultStyle
                            const isRead = notif.isRead || notif.read
                            // Use notification.priority if exists, otherwise use type-based priority
                            const priorityLevel = notif.priority || style.priority

                            return (
                                <div
                                    key={notif.id}
                                    className={`p-4 rounded-lg border transition-all ${isRead ? 'border-gray-200 bg-white' : 'border-l-4 bg-blue-50'}`}
                                    style={!isRead ? { borderLeftColor: style.color } : { borderColor: '#E5E7EB' }}
                                >
                                    <div className="space-y-3">
                                        {/* First row: Priority Circle, Type Badge, Time */}
                                        <div className="flex items-center gap-3 justify-between flex-wrap">
                                            {priorityLevel && getPriorityCircle(priorityLevel)}
                                            <span className="text-xs font-semibold px-2.5 py-1 rounded" style={{ backgroundColor: style.bg, color: style.color }}>{style.label}</span>
                                            <span className={`text-xs ml-auto ${isRead ? 'text-gray-400' : 'text-gray-600 font-medium'}`}>{formatTime(notif.createdAt || notif.time)}</span>
                                        </div>

                                        {/* Second row: Title (bigger) */}
                                        <div>
                                            <p className={`text-sm ${isRead ? 'text-gray-700' : 'text-gray-900 font-semibold'}`}>{getNotificationTitle(notif)}</p>
                                        </div>

                                        {/* Third row: Message and Review Button on same line */}
                                        <div className="flex items-end gap-3">
                                            <div className="flex-1">
                                                {getNotificationMessage(notif) && <p className={`text-sm leading-relaxed ${isRead ? 'text-gray-600' : 'text-gray-800'}`}>{getNotificationMessage(notif)}</p>}
                                            </div>
                                            {!isRead && (
                                                <button
                                                    onClick={() => openNotification(notif)}
                                                    className="px-3 py-1.5 text-xs font-medium rounded transition-colors text-white whitespace-nowrap"
                                                    style={{ backgroundColor: '#0B3C5D' }}
                                                >
                                                    Review
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )
                        })
                    )}
                </div>
            )}

            {/* Notification Details Modal - Auto closes after 10 seconds */}
            {selectedNotif && (
                <div className="modal-overlay" onClick={() => setSelectedNotif(null)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Notification Details</h3>
                            </div>
                            <div className="space-y-0">
                                <div className="flex justify-between py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Type</span>
                                    <span className="text-xs font-semibold px-2.5 py-1 rounded" style={{ backgroundColor: typeStyles[selectedNotif.alertType || selectedNotif.type || '']?.bg || '#E8EEF3', color: typeStyles[selectedNotif.alertType || selectedNotif.type || '']?.color || '#0B3C5D' }}>{typeStyles[selectedNotif.alertType || selectedNotif.type || '']?.label || 'Notification'}</span>
                                </div>

                                <div className="flex justify-between py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Priority</span>
                                    <div>
                                        {(selectedNotif.priority || typeStyles[selectedNotif.alertType || selectedNotif.type || '']?.priority) ? getPriorityCircle(selectedNotif.priority || typeStyles[selectedNotif.alertType || selectedNotif.type || '']?.priority) : <span className="text-sm text-gray-800">-</span>}
                                    </div>
                                </div>

                                <div className="flex justify-between py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Time</span>
                                    <span className="text-sm font-semibold text-gray-800">{formatTime(selectedNotif.createdAt || selectedNotif.time)}</span>
                                </div>

                                <div className="flex justify-between py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Title</span>
                                    <span className="text-sm font-semibold text-gray-800">{getNotificationTitle(selectedNotif)}</span>
                                </div>

                                <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Message</span>
                                    <p className="text-sm text-gray-800 mt-2">{getNotificationMessage(selectedNotif)}</p>
                                </div>
                            </div>

                            <div className="mt-4 pt-3 border-t text-xs text-gray-400 text-center" style={{ borderColor: '#D0D7DE' }}>
                                This will close automatically
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
export default NotificationsPage
