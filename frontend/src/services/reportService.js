import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const reportService = {
    getComplianceSummary: async () => {
        try {
            const cacheKey = 'report_compliance_summary'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/reports/compliance/summary')
            requestCache.set(cacheKey, response.data, 15 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('reportService.getComplianceSummary', error)
            throw handleApiError(error)
        }
    },
    getDepartmentCompliance: async () => {
        try {
            const cacheKey = 'report_dept_compliance'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/reports/compliance/department')
            requestCache.set(cacheKey, response.data, 15 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('reportService.getDepartmentCompliance', error)
            throw handleApiError(error)
        }
    },
    getDefaulters: async () => {
        try {
            const cacheKey = 'report_defaulters'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/reports/compliance/user')
            requestCache.set(cacheKey, response.data, 15 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('reportService.getDefaulters', error)
            throw handleApiError(error)
        }
    },
    getAuditTrail: async (documentId) => {
        try {
            if (documentId) {
                const cacheKey = `report_audit_${documentId}`
                if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
                const response = await api.get(`/reports/audit/document/${documentId}`)
                requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
                return response.data
            }
            return { content: [] }
        } catch (error) {
            logError('reportService.getAuditTrail', error)
            throw handleApiError(error)
        }
    },
    getViolationTrends: async (days = 7) => {
        try {
            const cacheKey = `report_violation_trends_${days}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/reports/violations/trends?days=${days}`)
            requestCache.set(cacheKey, response.data, 30 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('reportService.getViolationTrends', error)
            throw handleApiError(error)
        }
    },
    exportPdf: async (reportType) => {
        try {
            const typeMap = { compliance: 'compliance-summary', department: 'department-compliance', defaulters: 'user-defaulter' }
            const endpoint = typeMap[reportType] || 'compliance-summary'
            const response = await api.get(`/reports/export/pdf/${endpoint}`, { responseType: 'blob' })
            return response.data
        } catch (error) {
            logError('reportService.exportPdf', error)
            throw handleApiError(error)
        }
    },
    exportExcel: async (reportType) => {
        try {
            const typeMap = { compliance: 'compliance-summary', department: 'department-compliance', defaulters: 'user-defaulter' }
            const endpoint = typeMap[reportType] || 'compliance-summary'
            const response = await api.get(`/reports/export/excel/${endpoint}`, { responseType: 'blob' })
            return response.data
        } catch (error) {
            logError('reportService.exportExcel', error)
            throw handleApiError(error)
        }
    },
}
export default reportService

