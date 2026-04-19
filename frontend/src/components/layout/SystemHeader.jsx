import { useState, useRef, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { FaBell, FaSignOutAlt, FaUser, FaChevronDown, FaSyncAlt } from 'react-icons/fa'
import { useAuth, ROLES } from '../../context/AuthContext'
import notificationService from '../../services/notificationService'

// SYSTEM HEADER — Navigation bar + User panel + Marquee
const SystemHeader = () => {
    const { user, logout, hasPermission } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const [showDropdown, setShowDropdown] = useState(false)
    const [isRefreshing, setIsRefreshing] = useState(false)
    const [unreadCount, setUnreadCount] = useState(0)
    const [liveTime, setLiveTime] = useState(new Date())
    const dropdownRef = useRef(null)

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setShowDropdown(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    // Live clock — updates every second
    useEffect(() => {
        const clockInterval = setInterval(() => setLiveTime(new Date()), 1000)
        return () => clearInterval(clockInterval)
    }, [])

    // Fetch unread notification count
    useEffect(() => {
        const fetchUnreadCount = async () => {
            try {
                const response = await notificationService.getUnreadCount()
                const count = response.data?.unreadCount || 0
                setUnreadCount(count)
            } catch (error) {
                console.log('Failed to fetch unread count')
            }
        }
        fetchUnreadCount()
        // Refresh count every 30 seconds
        const interval = setInterval(fetchUnreadCount, 30000)
        return () => clearInterval(interval)
    }, [])

    const handleLogout = () => { logout(); navigate('/') }

    const handleRefresh = () => {
        setIsRefreshing(true)
        window.location.reload()
    }

    if (!user) return null

    const roleLabel = {
        [ROLES.SUPER_ADMIN]: 'System Administrator',
        [ROLES.DEPARTMENT_ADMIN]: 'Department Admin',
        [ROLES.DEPARTMENT_UPLOAD_ADMIN]: 'Upload Admin',
        [ROLES.DEPARTMENT_USER]: 'Department User',
    }[user.role] || user.role

    // Build nav items based on role
    const navItems = [
        { path: '/dashboard', label: 'Home' },
        { path: '/documents', label: 'Documents' },
    ]
    if (hasPermission('upload')) navItems.push({ path: '/upload', label: 'Upload' })
    if (hasPermission('compliance')) navItems.push({ path: '/compliance', label: 'Compliance' })
    if (hasPermission('reports')) navItems.push({ path: '/reports', label: 'Reports' })
    if (hasPermission('analytics')) navItems.push({ path: '/analytics', label: 'Analytics' })
    if (hasPermission('createPolicy')) navItems.push({ path: '/policies', label: 'Policies' })
    if (hasPermission('deptUsers')) navItems.push({ path: '/users', label: 'Users' })

    const getInitials = (name) => {
        if (!name) return 'U'
        return name.split(' ').map(n => n.charAt(0)).join('').toUpperCase().slice(0, 2)
    }

    return (
        <div>
            {/* Navigation Bar */}
            <div className="flex items-center justify-between px-6 py-0 border-b border-t" style={{ backgroundColor: '#082F4A', borderColor: 'rgba(255,255,255,0.1)' }}>
                {/* Nav Links */}
                <nav className="flex items-center gap-0" role="navigation" aria-label="Main navigation">
                    {navItems.map(item => (
                        <button
                            key={item.path}
                            onClick={() => navigate(item.path)}
                            className="px-4 py-3 text-sm font-semibold transition-all border-b-2"
                            style={{
                                color: location.pathname === item.path ? '#FFFFFF' : 'rgba(255,255,255,0.7)',
                                borderBottomColor: location.pathname === item.path ? 'rgba(255,255,255,0.8)' : 'transparent',
                                backgroundColor: location.pathname === item.path ? 'rgba(255,255,255,0.12)' : 'transparent',
                            }}
                            onMouseOver={e => { if (location.pathname !== item.path) e.target.style.backgroundColor = 'rgba(255,255,255,0.08)' }}
                            onMouseOut={e => { if (location.pathname !== item.path) e.target.style.backgroundColor = 'transparent' }}
                        >
                            {item.label}
                        </button>
                    ))}
                </nav>

                {/* Right: Notification + User */}
                <div className="flex items-center gap-4">
                    <button
                        className="relative p-2.5 text-white/75 hover:text-white hover:bg-white/20 rounded-lg transition-all"
                        onClick={() => navigate('/notifications')}
                        aria-label="Notifications"
                        title="Notifications"
                    >
                        <FaBell className="text-base" />
                        {unreadCount > 0 && <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full text-white text-xs flex items-center justify-center font-bold" style={{ backgroundColor: '#0B3C5D', fontSize: '10px' }}>{unreadCount > 99 ? '99+' : unreadCount}</span>}
                    </button>

                    {/* User Dropdown */}
                    <div className="relative" ref={dropdownRef}>
                        <button
                            className="flex items-center gap-2.5 px-4 py-2 rounded-lg transition-all text-white hover:bg-white/15"
                            onClick={() => setShowDropdown(!showDropdown)}
                            aria-expanded={showDropdown}
                            title="Profile menu"
                        >
                            <div className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold" style={{ backgroundColor: 'rgba(255,255,255,0.25)', color: '#FFF' }}>
                                {getInitials(user.name)}
                            </div>
                            <span className="text-sm font-medium hidden md:inline">{user.name || 'User'}</span>
                            <FaChevronDown className="text-xs text-white/50 transition-transform" style={{ transform: showDropdown ? 'rotate(180deg)' : 'rotate(0)' }} />
                        </button>

                        {showDropdown && (
                            <div className="absolute right-0 top-full mt-2 bg-white border rounded-lg shadow-lg py-1.5 w-72 z-50 overflow-hidden" style={{ borderColor: '#E8EEF3' }}>
                                <div className="px-4 py-4 border-b" style={{ borderColor: '#E8EEF3', background: 'linear-gradient(135deg, #F8FAFC 0%, #F0F4F8 100%)' }}>
                                    <p className="text-sm font-bold text-gray-900">{user.name || 'User'}</p>
                                    <p className="text-xs text-gray-500 mt-1">{user.email || 'no-email@metro.gov'}</p>
                                    <div className="mt-2 pt-2 border-t" style={{ borderColor: '#E0E7F1' }}>
                                        <p className="text-xs font-semibold text-blue-900" style={{ color: '#0B3C5D' }}>{roleLabel}</p>
                                        <p className="text-xs text-gray-500 mt-0.5">{user.department || 'Department'}</p>
                                    </div>
                                </div>
                                <button className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-blue-50 flex items-center gap-3 transition-colors" onClick={() => { navigate('/settings'); setShowDropdown(false) }}>
                                    <FaUser className="text-sm text-blue-600" style={{ color: '#0B3C5D' }} /> Profile & Settings
                                </button>
                                <button className="w-full text-left px-4 py-2.5 text-sm text-gray-700 hover:bg-blue-50 flex items-center gap-3 transition-colors" onClick={() => { navigate('/notifications'); setShowDropdown(false) }}>
                                    <FaBell className="text-sm text-blue-600" style={{ color: '#0B3C5D' }} /> Notifications
                                </button>
                                <div className="border-t" style={{ borderColor: '#E8EEF3' }}></div>
                                <button className="w-full text-left px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 flex items-center gap-3 transition-colors" onClick={handleLogout}>
                                    <FaSignOutAlt className="text-sm" /> Sign Out
                                </button>
                            </div>
                        )}
                    </div>

                    {/* Refresh Button */}
                    <button
                        className="relative p-2.5 text-white/75 hover:text-white hover:bg-white/20 rounded-lg transition-all"
                        onClick={handleRefresh}
                        aria-label="Refresh"
                        title="Refresh page"
                        disabled={isRefreshing}
                    >
                        <FaSyncAlt className={`text-base ${isRefreshing ? 'animate-spin' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Latest Updates Ticker — Professional Design */}
            <div className="overflow-hidden border-b" style={{ background: 'linear-gradient(135deg, #F0F5FB 0%, #E8F0F7 100%)', borderColor: '#D4E1ED', height: '36px' }}>
                <div className="flex items-center h-full px-6 gap-3">
                    <span className="text-xs font-bold flex-shrink-0 px-3 py-1.5 rounded-full whitespace-nowrap" style={{ backgroundColor: '#0B3C5D', color: '#FFF', letterSpacing: '0.5px' }}>Latest Updates</span>
                    <div className="overflow-hidden flex-1">
                        <p className="text-xs whitespace-nowrap" style={{ color: '#1E3A52', animation: 'scroll-left 40s linear infinite', fontWeight: '500' }}>
                            📌 All departments must complete document acknowledgements within SLA deadlines. Pending violations will be escalated automatically.
                            &nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                            📌 System maintenance scheduled: 2nd Sunday of every month (02:00 - 06:00 hrs IST).
                            &nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                            📌 New compliance policy effective from 01 March 2026 — contact IT helpdesk for details.
                            &nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;
                            📌 Metro Document Portal upgraded to v1.0 — All users must re-verify their profiles.
                        </p>
                    </div>
                    {/* Live + Timestamp on right side */}
                    <div className="flex items-center gap-2 flex-shrink-0 pl-3" style={{ borderLeft: '1px solid #D4E1ED' }}>
                        <span style={{ width: '7px', height: '7px', borderRadius: '50%', backgroundColor: '#22c55e', display: 'inline-block', boxShadow: '0 0 6px rgba(34,197,94,0.5)', animation: 'liveBlink 1.5s ease-in-out infinite' }}></span>
                        <span className="text-xs font-bold whitespace-nowrap" style={{ color: '#0B3C5D' }}>Live</span>
                        <span className="text-xs whitespace-nowrap" style={{ color: '#5A7A94' }}>{liveTime.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
                    </div>
                </div>
                <style>{`
                    @keyframes scroll-left {
                        0% { transform: translateX(100%); }
                        100% { transform: translateX(-200%); }
                    }
                    @keyframes liveBlink {
                        0%, 100% { opacity: 1; }
                        50% { opacity: 0.4; }
                    }
                `}</style>
            </div>
        </div>
    )
}

export default SystemHeader
