import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const dashboardService = {
    getSummary: async () => {
        try {
            const cacheKey = 'dashboard_summary'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/dashboard/summary')
            requestCache.set(cacheKey, response.data, 3 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('dashboardService.getSummary', error)
            throw handleApiError(error)
        }
    },
    getPendingActions: async (page = 0, size = 20) => {
        try {
            const cacheKey = `dashboard_pending_${page}_${size}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/dashboard/pending-actions?page=${page}&size=${size}`)
            requestCache.set(cacheKey, response.data, 2 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('dashboardService.getPendingActions', error)
            throw handleApiError(error)
        }
    },
    getRecentDocuments: async (days = 7, page = 0, size = 10) => {
        try {
            const cacheKey = `dashboard_recent_${days}_${page}_${size}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/dashboard/recent?days=${days}&page=${page}&size=${size}`)
            requestCache.set(cacheKey, response.data, 5 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('dashboardService.getRecentDocuments', error)
            throw handleApiError(error)
        }
    },
    getDepartmentDashboard: async () => {
        try {
            const cacheKey = 'dashboard_department'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/dashboard/department')
            requestCache.set(cacheKey, response.data, 3 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('dashboardService.getDepartmentDashboard', error)
            throw handleApiError(error)
        }
    },
    getDeadlineTracking: async () => {
        try {
            const cacheKey = 'dashboard_deadlines'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/dashboard/deadlines')
            requestCache.set(cacheKey, response.data, 5 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('dashboardService.getDeadlineTracking', error)
            throw handleApiError(error)
        }
    },
}
export default dashboardService

