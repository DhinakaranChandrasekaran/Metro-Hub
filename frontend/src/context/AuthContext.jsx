import { createContext, useContext, useState, useEffect } from 'react'
import authService from '../services/authService'

// AUTH CONTEXT — Real Backend JWT Authentication
const AuthContext = createContext(null)

export const ROLES = {
    SUPER_ADMIN: 'SUPER_ADMIN',
    DEPARTMENT_UPLOAD_ADMIN: 'DEPARTMENT_UPLOAD_ADMIN',
    DEPARTMENT_ADMIN: 'DEPARTMENT_ADMIN',
    DEPARTMENT_USER: 'DEPARTMENT_USER',
}

export const PERMISSIONS = {
    upload: [ROLES.DEPARTMENT_UPLOAD_ADMIN],
    viewDocuments: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    deleteDocuments: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    acknowledge: [ROLES.DEPARTMENT_USER],
    viewAckList: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    setSla: [ROLES.DEPARTMENT_UPLOAD_ADMIN],
    applyLegalHold: [ROLES.DEPARTMENT_UPLOAD_ADMIN],
    removeLegalHold: [ROLES.SUPER_ADMIN],
    viewLegalHolds: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN],
    createPolicy: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN],
    deletePolicy: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    viewPolicies: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    reports: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    analytics: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    triggerRisk: [ROLES.SUPER_ADMIN],
    viewViolations: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    resolveViolation: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN],
    globalViolation: [ROLES.SUPER_ADMIN],
    globalUsers: [ROLES.SUPER_ADMIN],
    deptUsers: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    viewDeptUsers: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN],
    adminDashboard: [ROLES.SUPER_ADMIN],
    deptDashboard: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    alerts: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    createReminder: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN],
    viewReminders: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    notifications: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    settings: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_USER],
    compliance: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN],
    viewOriginalDoc: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN, ROLES.DEPARTMENT_USER],
    viewSla: [ROLES.SUPER_ADMIN, ROLES.DEPARTMENT_ADMIN, ROLES.DEPARTMENT_UPLOAD_ADMIN],
}

export const useAuth = () => {
    const context = useContext(AuthContext)
    if (!context) throw new Error('useAuth must be used within an AuthProvider')
    return context
}

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(() => {
        try {
            const saved = sessionStorage.getItem('metrohub_user')
            return saved ? JSON.parse(saved) : null
        } catch { return null }
    })
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        const token = sessionStorage.getItem('token')
        if (token) {
            authService.getCurrentUser()
                .then(data => {
                    const userData = {
                        id: data.id,
                        name: data.name,
                        email: data.email,
                        role: data.role,
                        department: data.departmentName || data.department || '',
                        departmentId: data.departmentId,
                        employeeId: data.employeeId,
                        phoneNumber: data.phoneNumber,
                        designation: data.designation || '',
                        isActive: data.isActive,
                    }
                    sessionStorage.setItem('metrohub_user', JSON.stringify(userData))
                    setUser(userData)
                })
                .catch((err) => {
                    // Only clear session on 401 (token truly expired)
                    // For other errors (network, 500), keep existing session
                    if (err?.response?.status === 401) {
                        sessionStorage.removeItem('token')
                        sessionStorage.removeItem('metrohub_user')
                        setUser(null)
                    }
                })
                .finally(() => setLoading(false))
        } else {
            setLoading(false)
        }
    }, [])

    const login = async (email, password) => {
        const response = await authService.login(email, password)
        // Backend LoginResponseDTO returns flat fields: accessToken, refreshToken, name, email, role, etc.
        const token = response.accessToken || response.token
        sessionStorage.setItem('token', token)
        if (response.refreshToken) sessionStorage.setItem('refreshToken', response.refreshToken)
        console.log('✅ Login successful. Token stored:', token ? 'Yes' : 'No')

        const userData = {
            id: response.userId || response.id,
            name: response.name,
            email: response.email,
            role: response.role,
            department: response.departmentName || response.department || '',
            departmentId: response.departmentId,
            employeeId: response.employeeId,
            phoneNumber: response.phoneNumber,
            designation: response.designation || '',
            isActive: response.isActive !== false,
        }
        sessionStorage.setItem('metrohub_user', JSON.stringify(userData))
        setUser(userData)
        return userData
    }

    const logout = async () => {
        try { await authService.logout() } catch (e) { /* ignore */ }
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('refreshToken')
        sessionStorage.removeItem('metrohub_user')
        setUser(null)
    }

    const hasRole = (...roles) => user ? roles.includes(user.role) : false
    const hasPermission = (key) => user ? (PERMISSIONS[key] || []).includes(user.role) : false

    // Allow components to update user state after profile changes
    const updateUser = (updates) => {
        setUser(prev => {
            const updated = { ...prev, ...updates }
            sessionStorage.setItem('metrohub_user', JSON.stringify(updated))
            return updated
        })
    }

    return (
        <AuthContext.Provider value={{ user, loading, login, logout, hasRole, hasPermission, updateUser, isAuthenticated: !!user }}>
            {children}
        </AuthContext.Provider>
    )
}

export default AuthContext
