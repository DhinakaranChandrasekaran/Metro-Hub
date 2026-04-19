import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const documentService = {
  uploadDocument: async (formData, onProgress) => {
    try {
      const response = await api.post('/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onProgress && progressEvent.total) {
            const pct = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(pct)
          }
        }
      })
      requestCache.clearAll()
      return response.data
    } catch (error) {
      logError('uploadDocument', error)
      throw handleApiError(error)
    }
  },

  getAllDocuments: async (page = 0, size = 20) => {
    try {
      const cacheKey = `docs_all_${page}_${size}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/documents?page=${page}&size=${size}`)
      const data = response.data
      requestCache.set(cacheKey, data, 5 * 60 * 1000)
      return data
    } catch (error) {
      logError('getAllDocuments', error)
      throw handleApiError(error)
    }
  },

  getDocumentById: async (id) => {
    try {
      const cacheKey = `doc_${id}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/documents/${id}`)
      const data = response.data
      requestCache.set(cacheKey, data, 10 * 60 * 1000)
      return data
    } catch (error) {
      logError('getDocumentById', error)
      throw handleApiError(error)
    }
  },

  searchDocuments: async (searchParams) => {
    try {
      const response = await api.get('/search/advanced', { params: searchParams })
      return response.data.data || response.data
    } catch (error) {
      logError('searchDocuments', error)
      throw handleApiError(error)
    }
  },

  deleteDocument: async (id) => {
    try {
      await api.delete(`/documents/${id}`)
      requestCache.clearAll()
    } catch (error) {
      logError('deleteDocument', error)
      throw handleApiError(error)
    }
  },

  downloadOriginal: async (id) => {
    try {
      const response = await api.get(`/documents/${id}/download`, { responseType: 'blob' })
      return response.data
    } catch (error) {
      logError('downloadOriginal', error)
      throw handleApiError(error)
    }
  },

  downloadExtracted: async (id) => {
    try {
      const response = await api.get(`/documents/${id}/download-extracted`, { responseType: 'blob' })
      return response.data
    } catch (error) {
      logError('downloadExtracted', error)
      throw handleApiError(error)
    }
  },

  acknowledge: async (id, notes) => {
    try {
      const response = await api.post(`/documents/${id}/acknowledge`, { notes })
      requestCache.clearAll()
      return response.data
    } catch (error) {
      logError('acknowledge', error)
      throw handleApiError(error)
    }
  },

  getPendingAcknowledgements: async (page = 0, size = 20) => {
    try {
      const cacheKey = `pending_acks_${page}_${size}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/documents/pending-acknowledgement?page=${page}&size=${size}`)
      const data = response.data
      requestCache.set(cacheKey, data, 2 * 60 * 1000)
      return data
    } catch (error) {
      logError('getPendingAcknowledgements', error)
      throw handleApiError(error)
    }
  },

  getAcknowledgementHistory: async (id) => {
    try {
      if (id) {
        const cacheKey = `ack_history_${id}`
        if (requestCache.has(cacheKey)) {
          return requestCache.get(cacheKey)
        }
        const response = await api.get(`/documents/${id}/acknowledgements`)
        const data = response.data
        requestCache.set(cacheKey, data, 5 * 60 * 1000)
        return data
      }
      return { content: [] }
    } catch (error) {
      logError('getAcknowledgementHistory', error)
      throw handleApiError(error)
    }
  },

  getByDepartment: async (deptId, page = 0, size = 20) => {
    try {
      const cacheKey = `docs_dept_${deptId}_${page}_${size}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/documents/department/${deptId}?page=${page}&size=${size}`)
      const data = response.data
      requestCache.set(cacheKey, data, 5 * 60 * 1000)
      return data
    } catch (error) {
      logError('getByDepartment', error)
      throw handleApiError(error)
    }
  },

  applySla: async (id, slaConfig) => {
    try {
      const response = await api.put(`/documents/${id}/sla`, slaConfig)
      requestCache.clearAll()
      return response.data
    } catch (error) {
      logError('applySla', error)
      throw handleApiError(error)
    }
  },

  getLegalHolds: async () => {
    try {
      const cacheKey = 'legal_holds'
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get('/documents/legal-hold')
      const data = response.data
      requestCache.set(cacheKey, data, 10 * 60 * 1000)
      return data
    } catch (error) {
      logError('getLegalHolds', error)
      throw handleApiError(error)
    }
  },

  getAcknowledgementsForDoc: async (id) => {
    try {
      const cacheKey = `ack_for_doc_${id}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/documents/${id}/acknowledgements`)
      const data = response.data
      requestCache.set(cacheKey, data, 5 * 60 * 1000)
      return data
    } catch (error) {
      logError('getAcknowledgementsForDoc', error)
      throw handleApiError(error)
    }
  },

  getDepartmentUsers: async (departmentId) => {
    try {
      const cacheKey = `dept_users_${departmentId}`
      if (requestCache.has(cacheKey)) {
        return requestCache.get(cacheKey)
      }
      const response = await api.get(`/users/department/${departmentId}/list`)
      const data = response.data
      requestCache.set(cacheKey, data, 15 * 60 * 1000)
      return data
    } catch (error) {
      logError('getDepartmentUsers', error)
      throw handleApiError(error)
    }
  },

  hasUserAcknowledged: async (documentId) => {
    try {
      const response = await api.get(`/documents/${documentId}/acknowledged`)
      return response.data
    } catch (error) {
      logError('hasUserAcknowledged', error)
      return { acknowledged: false }
    }
  },

  getSlaConfig: async (documentId) => {
    try {
      const response = await api.get(`/documents/${documentId}/sla`)
      return response.data
    } catch (error) {
      logError('getSlaConfig', error)
      throw handleApiError(error)
    }
  },
}

export default documentService

