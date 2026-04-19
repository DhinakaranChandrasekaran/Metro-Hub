import api from './api'
import { requestCache } from './cacheService'
import { handleApiError, logError } from '../utils/errorHandler'

const userService = {
    getAll: async (page = 0, size = 50) => {
        try {
            const cacheKey = `users_all_${page}_${size}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/users/all?page=${page}&size=${size}`)
            requestCache.set(cacheKey, response.data, 5 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('userService.getAll', error)
            throw handleApiError(error)
        }
    },
    getById: async (id) => {
        try {
            const cacheKey = `user_${id}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/users/${id}`)
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('userService.getById', error)
            throw handleApiError(error)
        }
    },
    create: async (data) => {
        try {
            const response = await api.post('/auth/register', data)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('userService.create', error)
            throw handleApiError(error)
        }
    },
    update: async (id, data) => {
        try {
            const response = await api.put(`/users/${id}`, data)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('userService.update', error)
            throw handleApiError(error)
        }
    },
    delete: async (id) => {
        try {
            await api.post(`/users/${id}/deactivate`)
            requestCache.clearAll()
        } catch (error) {
            logError('userService.delete', error)
            throw handleApiError(error)
        }
    },
    toggleStatus: async (id, isActive) => {
        try {
            const endpoint = isActive ? `/users/${id}/deactivate` : `/users/${id}/activate`
            const response = await api.post(endpoint)
            requestCache.clearAll()
            return response.data
        } catch (error) {
            logError('userService.toggleStatus', error)
            throw handleApiError(error)
        }
    },
    getByDepartment: async (deptId) => {
        try {
            const cacheKey = `users_dept_${deptId}`
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get(`/users/department/${deptId}`)
            requestCache.set(cacheKey, response.data, 10 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('userService.getByDepartment', error)
            throw handleApiError(error)
        }
    },
    getAdmins: async () => {
        try {
            const cacheKey = 'users_admins'
            if (requestCache.has(cacheKey)) return requestCache.get(cacheKey)
            const response = await api.get('/users/admins')
            requestCache.set(cacheKey, response.data, 15 * 60 * 1000)
            return response.data
        } catch (error) {
            logError('userService.getAdmins', error)
            throw handleApiError(error)
        }
    },
}
export default userService

