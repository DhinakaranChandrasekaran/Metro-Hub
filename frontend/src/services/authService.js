import api from './api'
import { handleApiError, logError } from '../utils/errorHandler'

// AUTH SERVICE — Calls backend /api/auth endpoints
const authService = {
    login: async (email, password) => {
        try {
            const response = await api.post('/auth/login', { email, password })
            return response.data
        } catch (error) {
            logError('authService.login', error)
            throw handleApiError(error)
        }
    },

    logout: async () => {
        try {
            await api.post('/auth/logout')
        } catch (error) {
            logError('authService.logout', error)
            // Logout errors are non-critical, continue anyway
        }
    },

    getCurrentUser: async () => {
        try {
            const response = await api.get('/auth/me')
            return response.data
        } catch (error) {
            logError('authService.getCurrentUser', error)
            throw handleApiError(error)
        }
    },

    changePassword: async (oldPassword, newPassword) => {
        try {
            const response = await api.post('/auth/change-password', { oldPassword, newPassword })
            return response.data
        } catch (error) {
            logError('authService.changePassword', error)
            throw handleApiError(error)
        }
    },

    register: async (data) => {
        try {
            const response = await api.post('/auth/register', data)
            return response.data
        } catch (error) {
            logError('authService.register', error)
            throw handleApiError(error)
        }
    },

    refreshToken: async (refreshToken) => {
        try {
            const response = await api.post('/auth/refresh', { refreshToken })
            return response.data
        } catch (error) {
            logError('authService.refreshToken', error)
            throw handleApiError(error)
        }
    },
}

export default authService
