import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const analyticsService = {
    getRiskSummary: async () => {
        try {
            const cacheKey = 'analytics_risk_summary'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/analytics/risk/summary')
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('analyticsService.getRiskSummary', error)
            throw handleApiError(error)
        }
    },
    getDepartmentRisk: async () => {
        try {
            // Don't cache - always fetch fresh data
            const response = await api.get('/analytics/risk/departments')
            return response.data
        } catch (error) {
            logError('analyticsService.getDepartmentRisk', error)
            throw handleApiError(error)
        }
    },
    getRiskHeatmap: async () => {
        try {
            const cacheKey = 'analytics_risk_heatmap'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/analytics/risk/heatmap')
            requestCache.set(cacheKey, response.data, 15 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('analyticsService.getRiskHeatmap', error)
            throw handleApiError(error)
        }
    },
    getRiskTrends: async (days = 30) => {
        try {
            const cacheKey = `analytics_risk_trends_${days}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/analytics/risk/trends?days=${days}`)
            requestCache.set(cacheKey, response.data, 30 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('analyticsService.getRiskTrends', error)
            throw handleApiError(error)
        }
    },
    getTopDefaulters: async (limit = 10) => {
        try {
            const cacheKey = `analytics_top_defaulters_${limit}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/analytics/risk/top-defaulters?limit=${limit}`)
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('analyticsService.getTopDefaulters', error)
            throw handleApiError(error)
        }
    },
}
export default analyticsService

