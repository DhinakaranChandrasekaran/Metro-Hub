import api from './api'
import { handleApiError, logError } from '../utils/errorHandler'

const notificationService = {
    getAll: async (page = 0, size = 50) => {
        try {
            // Don't cache notifications - each user has different notifications
            // Caching causes one user's notifications to appear in another user's session
            const response = await api.get(`/alerts/my?page=${page}&size=${size}`)
            return response.data
        } catch (error) {
            logError('notificationService.getAll', error)
            throw handleApiError(error)
        }
    },
    getUnread: async () => {
        try {
            // Don't cache - must be fresh per user
            const response = await api.get('/alerts/my/unread')
            return response.data
        } catch (error) {
            logError('notificationService.getUnread', error)
            throw handleApiError(error)
        }
    },
    getUnreadCount: async () => {
        try {
            // Don't cache - must be fresh per user
            const response = await api.get('/alerts/my/unread-count')
            return response.data
        } catch (error) {
            logError('notificationService.getUnreadCount', error)
            throw handleApiError(error)
        }
    },
    markAsRead: async (id) => {
        try {
            const response = await api.post(`/alerts/my/mark-read/${id}`)
            return response.data
        } catch (error) {
            logError('notificationService.markAsRead', error)
            throw handleApiError(error)
        }
    },
    markAllRead: async () => {
        try {
            const response = await api.post('/alerts/my/mark-all-read')
            return response.data
        } catch (error) {
            logError('notificationService.markAllRead', error)
            throw handleApiError(error)
        }
    },
}
export default notificationService

