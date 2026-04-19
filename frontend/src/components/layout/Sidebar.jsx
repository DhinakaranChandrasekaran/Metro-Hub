import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import {
  FaTachometerAlt,
  FaFileAlt,
  FaCloudUploadAlt,
  FaFolderOpen,
  FaBell,
  FaCheckCircle,
  FaExclamationTriangle,
  FaChartBar,
  FaChartLine,
  FaGavel,
  FaUsers,
  FaCog,
  FaChevronDown,
  FaChevronRight,
  FaAngleDoubleLeft,
  FaAngleDoubleRight,
  FaHeadset,
  FaEnvelope,
  FaPhone,
} from 'react-icons/fa'
import { useAuth } from '../../context/AuthContext'

// SIDEBAR — Strict Backend-Aligned RBAC
const Sidebar = () => {
  const { hasPermission } = useAuth()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [docsExpanded, setDocsExpanded] = useState(
    location.pathname === '/upload' || location.pathname === '/documents'
  )

  /* ── Build nav items — only include items user has access to ── */
  const buildNavItems = () => {
    const items = []

    // Dashboard — everyone
    items.push({ path: '/dashboard', icon: FaTachometerAlt, label: 'Dashboard' })

    // Documents section (expandable)
    const docChildren = []
    if (hasPermission('upload')) {
      docChildren.push({ path: '/upload', icon: FaCloudUploadAlt, label: 'Upload' })
    }
    docChildren.push({ path: '/documents', icon: FaFolderOpen, label: 'View Documents' })
    items.push({
      type: 'group',
      title: 'Documents',
      icon: FaFileAlt,
      children: docChildren,
      expanded: docsExpanded,
      onToggle: () => setDocsExpanded(!docsExpanded),
    })

    // Notifications — everyone
    items.push({ path: '/notifications', icon: FaBell, label: 'Notifications' })

    // Acknowledgements — view acknowledgement tracking (all roles)
    if (hasPermission('viewAckList')) {
      items.push({ path: '/acknowledgements', icon: FaCheckCircle, label: 'Acknowledgements' })
    }

    // Compliance — super admin, dept admin
    if (hasPermission('compliance')) {
      items.push({ path: '/compliance', icon: FaExclamationTriangle, label: 'Compliance' })
    }

    // Policies — view policies is available to all, but page-level write control is handled there
    if (hasPermission('createPolicy')) {
      items.push({ path: '/policies', icon: FaGavel, label: 'Policies' })
    }

    // Reports — super admin, dept admin
    if (hasPermission('reports')) {
      items.push({ path: '/reports', icon: FaChartBar, label: 'Reports' })
    }

    // Analytics — super admin, dept admin
    if (hasPermission('analytics')) {
      items.push({ path: '/analytics', icon: FaChartLine, label: 'Analytics' })
    }

    // Users — super admin (global), dept admin (dept level)
    if (hasPermission('deptUsers')) {
      items.push({ path: '/users', icon: FaUsers, label: 'Users' })
    }

    // Settings — everyone
    items.push({ path: '/settings', icon: FaCog, label: 'Settings' })

    return items
  }

  const navItems = buildNavItems()

  return (
    <aside
      className={`bg-white border-r flex-shrink-0 flex flex-col transition-all duration-200 ${collapsed ? 'w-16' : 'w-60'}`}
      style={{ borderColor: '#D0D7DE', minHeight: 'calc(100vh - 140px)' }}
      role="navigation"
      aria-label="Main navigation"
    >
      {/* Collapse Toggle */}
      <div className="flex justify-end p-2 border-b" style={{ borderColor: '#D0D7DE' }}>
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded transition-colors"
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <FaAngleDoubleRight /> : <FaAngleDoubleLeft />}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-2 overflow-y-auto">
        {navItems.map((item, idx) => {
          if (item.type === 'group') {
            return (
              <div key={idx} className="mb-1">
                <button
                  onClick={item.onToggle}
                  className={`w-full sidebar-nav-item mx-2 ${collapsed ? 'justify-center px-0' : ''}`}
                  style={{ width: collapsed ? '44px' : 'calc(100% - 16px)' }}
                  aria-expanded={item.expanded}
                >
                  <span className="nav-icon"><item.icon /></span>
                  {!collapsed && (
                    <>
                      <span className="flex-1 text-left">{item.title}</span>
                      {item.expanded ? <FaChevronDown className="text-xs" /> : <FaChevronRight className="text-xs" />}
                    </>
                  )}
                </button>
                {(item.expanded || collapsed) && item.children.map(child => (
                  <NavItem key={child.path} item={child} collapsed={collapsed} indent={!collapsed} />
                ))}
              </div>
            )
          }
          return <NavItem key={item.path} item={item} collapsed={collapsed} />
        })}
      </nav>

      {/* IT Helpdesk — styled card matching app design */}
      {!collapsed && (
        <div className="p-3 border-t" style={{ borderColor: '#D0D7DE' }}>
          <div className="rounded-lg p-4" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', boxShadow: '0 2px 8px rgba(11,60,93,0.15)' }}>
            <div className="flex items-center gap-2 mb-3">
              <FaHeadset className="text-white text-lg" />
              <p className="text-xs font-bold text-white tracking-wide">IT HELPDESK</p>
            </div>
            <div className="space-y-2">
              <p className="text-xs flex items-center gap-2">
                <FaEnvelope className="text-white/80 flex-shrink-0" />
                <a href="mailto:support@metrohub.in" className="text-white/90 hover:text-white hover:underline transition-colors">
                  support@metrohub.in
                </a>
              </p>
              <p className="text-xs flex items-center gap-2 text-white/70">
                <FaPhone className="text-white/80 flex-shrink-0" />
                1800-XXX-XXXX
              </p>
            </div>
          </div>
        </div>
      )}
    </aside>
  )
}

const NavItem = ({ item, collapsed, indent = false }) => (
  <NavLink
    to={item.path}
    className={({ isActive }) =>
      `sidebar-nav-item mx-2 ${isActive ? 'active' : ''} ${collapsed ? 'justify-center px-0' : ''} ${indent ? 'ml-8' : ''}`
    }
    style={{ width: collapsed ? '44px' : indent ? 'calc(100% - 48px)' : 'calc(100% - 16px)' }}
    title={collapsed ? item.label : ''}
  >
    <span className="nav-icon"><item.icon /></span>
    {!collapsed && <span>{item.label}</span>}
  </NavLink>
)

export default Sidebar
