import React from 'react'
import { FaExclamationTriangle } from 'react-icons/fa'

/**
 * ERROR BOUNDARY — Catches render errors in child components
 * Prevents entire app crash from single component error
 */
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null, errorInfo: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true }
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo)
    this.setState({
      error,
      errorInfo
    })
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center p-4" style={{ backgroundColor: '#F5F7FA' }}>
          <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-6">
            <div className="flex items-center gap-3 mb-4">
              <FaExclamationTriangle className="text-red-600 text-2xl" />
              <h1 className="text-lg font-bold text-gray-800">Something went wrong</h1>
            </div>
            <p className="text-sm text-gray-600 mb-4">
              An unexpected error occurred. Please try refreshing the page or contact your administrator if the problem persists.
            </p>
            {process.env.NODE_ENV === 'development' && this.state.error && (
              <details className="mt-4 mb-4">
                <summary className="text-xs text-gray-500 cursor-pointer mb-2">Error details (development only)</summary>
                <div className="text-xs bg-gray-100 p-2 rounded overflow-auto max-h-40" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                  {String(this.state.error)}
                </div>
              </details>
            )}
            <button
              onClick={() => window.location.href = '/'}
              className="btn-metro-primary w-full justify-center"
            >
              Go to Home
            </button>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary
