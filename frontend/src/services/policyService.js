import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const policyService = {
    getAll: async () => {
        try {
            const cacheKey = 'policies_all'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/policies')
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('policyService.getAll', error)
            throw handleApiError(error)
        }
    },
    create: async (data) => {
        try {
            let departmentId = data.departmentId || null
            const docId = data.documentId || null

            if (docId) {
                // Apply SLA to document FIRST - this will throw if SLA already exists or grace period expired
                await api.put(`/documents/${docId}/sla`, {
                    slaReminderHours: data.ackHours ?? data.reminderHours ?? 0,
                    slaDeptAdminEscalationHours: data.esc1 ?? data.deptAdminEscalationHours ?? 0,
                    slaSuperAdminEscalationHours: data.esc2 ?? data.superAdminEscalationHours ?? 0,
                    slaViolationHours: data.esc3 ?? data.violationHours ?? 0,
                })
            }

            const payload = {
                name: data.name || `SLA Rule - ${data.priority || 'MEDIUM'} Priority`,
                priority: data.priority || 'MEDIUM',
                departmentId: departmentId,
                reminderHours: data.ackHours ?? data.reminderHours ?? 0,
                deptAdminEscalationHours: data.esc1 ?? data.deptAdminEscalationHours ?? 0,
                superAdminEscalationHours: data.esc2 ?? data.superAdminEscalationHours ?? 0,
                violationHours: data.esc3 ?? data.violationHours ?? 0,
            }
            const response = await api.post('/policies', payload)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('policyService.create', error)
            throw handleApiError(error)
        }
    },
    update: async (id, data) => {
        try {
            const response = await api.put(`/policies/${id}`, data)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('policyService.update', error)
            throw handleApiError(error)
        }
    },
    toggle: async (id) => {
        try {
            const response = await api.post(`/policies/${id}/toggle`)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('policyService.toggle', error)
            throw handleApiError(error)
        }
    },
    delete: async (id) => {
        try {
            await api.delete(`/policies/${id}/permanent`)
            requestCache.clearAll()
        } catch (error) {
            logError('policyService.delete', error)
            throw handleApiError(error)
        }
    },
    getLegalHolds: async () => {
        try {
            const cacheKey = 'legal_holds'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/documents/legal-hold')
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('policyService.getLegalHolds', error)
            throw handleApiError(error)
        }
    },
    applyLegalHold: async (data) => {
        try {
            const response = await api.post(`/documents/${data.documentId}/legal-hold`, data)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('policyService.applyLegalHold', error)
            throw handleApiError(error)
        }
    },
    removeLegalHold: async (documentId) => {
        try {
            const response = await api.post(`/documents/${documentId}/legal-hold/remove`)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('policyService.removeLegalHold', error)
            throw handleApiError(error)
        }
    },
}
export default policyService

