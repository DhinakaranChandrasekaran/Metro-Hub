/**
 * ERROR HANDLING UTILITIES — Standardized error handling for all API calls
 */

export const handleApiError = (error) => {
  // Network error (no response from server)
  if (!error.response) {
    return {
      type: 'NETWORK_ERROR',
      message: error.message || 'Network error. Please check your connection.',
      status: null,
      details: error
    }
  }

  const { status, data } = error.response

  // 401 Unauthorized - Handled by API interceptor, but capture for logging
  if (status === 401) {
    return {
      type: 'AUTH_ERROR',
      message: 'Session expired. Please login again.',
      status,
      details: data
    }
  }

  // 403 Forbidden - User lacks permission
  if (status === 403) {
    return {
      type: 'PERMISSION_ERROR',
      message: data?.message || 'You do not have permission to perform this action.',
      status,
      details: data
    }
  }

  // 400 Bad Request - Validation error
  if (status === 400) {
    return {
      type: 'VALIDATION_ERROR',
      message: data?.message || 'Invalid request. Please check your input.',
      status,
      details: data
    }
  }

  // 404 Not Found
  if (status === 404) {
    return {
      type: 'NOT_FOUND',
      message: data?.message || 'The requested resource was not found.',
      status,
      details: data
    }
  }

  // 409 Conflict
  if (status === 409) {
    return {
      type: 'CONFLICT_ERROR',
      message: data?.message || 'This action conflicts with existing data.',
      status,
      details: data
    }
  }

  // 500+ Server errors
  if (status >= 500) {
    return {
      type: 'SERVER_ERROR',
      message: data?.message || 'Server error. Please try again later.',
      status,
      details: data
    }
  }

  // Generic error
  return {
    type: 'UNKNOWN_ERROR',
    message: data?.message || error.message || 'An unexpected error occurred.',
    status,
    details: data || error
  }
}

/**
 * Logs error for debugging (only in development or with explicit logging)
 */
export const logError = (context, error, apiError) => {
  if (process.env.NODE_ENV === 'development') {
    console.error(`[${context}]`, apiError || error)
  }
}

/**
 * Checks if error is retryable (transient errors)
 */
export const isRetryableError = (apiError) => {
  if (!apiError) return false
  const retryableStatus = [408, 429, 500, 502, 503, 504]
  return retryableStatus.includes(apiError.status) || apiError.type === 'NETWORK_ERROR'
}
