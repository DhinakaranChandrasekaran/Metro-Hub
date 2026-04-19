import axios from 'axios'

// Axios instance configured for MetroHub backend

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Token refresh queue to prevent multiple simultaneous refresh attempts
let isRefreshing = false
let refreshSubscribers = []

const onRefreshed = (token) => {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

const addRefreshSubscriber = (callback) => {
  refreshSubscribers.push(callback)
}

// Request interceptor - Add auth token
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token')
    console.log('🔵 API Request:', config.url, 'Token:', token ? 'Present' : 'Missing')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - Handle errors & token refresh
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { config, response } = error

    // 401 Unauthorized - Try to refresh token
    if (response?.status === 401 && !config._retry) {
      if (isRefreshing) {
        // Queue the request to retry after refresh completes
        return new Promise(resolve => {
          addRefreshSubscriber(token => {
            config.headers.Authorization = `Bearer ${token}`
            resolve(api(config))
          })
        })
      }

      config._retry = true
      isRefreshing = true

      const refreshToken = sessionStorage.getItem('refreshToken')

      if (!refreshToken) {
        // No refresh token - redirect to login
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('metrohub_user')
        window.location.href = '/'
        return Promise.reject(error)
      }

      // Attempt to refresh token using a new axios instance to avoid interceptor loop
      return axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
        .then(res => {
          const newToken = res.data.accessToken || res.data.token
          const newRefreshToken = res.data.refreshToken

          sessionStorage.setItem('token', newToken)
          if (newRefreshToken) sessionStorage.setItem('refreshToken', newRefreshToken)

          api.defaults.headers.common.Authorization = `Bearer ${newToken}`
          config.headers.Authorization = `Bearer ${newToken}`

          isRefreshing = false
          onRefreshed(newToken)

          return api(config)
        })
        .catch(err => {
          // Refresh failed - redirect to login
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('refreshToken')
          sessionStorage.removeItem('metrohub_user')
          isRefreshing = false
          refreshSubscribers = []
          window.location.href = '/'
          return Promise.reject(err)
        })
    }

    return Promise.reject(error)
  }
)

export default api
