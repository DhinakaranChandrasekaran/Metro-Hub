import { Link } from 'react-router-dom'

// UNAUTHORIZED PAGE
const UnauthorizedPage = () => {
    return (
        <div className="animate-fade-in text-center py-16">
            <div
                className="w-16 h-16 rounded-full mx-auto mb-5 flex items-center justify-center border"
                style={{ backgroundColor: '#FFEBEE', borderColor: '#EF9A9A' }}
            >
                <span className="text-2xl">🔒</span>
            </div>
            <h1 className="text-xl font-bold text-gray-800 mb-2">Access Restricted</h1>
            <p className="text-sm text-gray-500 mb-6 max-w-md mx-auto">
                You do not have the required permissions to access this page.
                If you believe this is an error, contact your system administrator.
            </p>
            <Link to="/dashboard" className="btn-metro-secondary inline-flex">
                ← Return to Dashboard
            </Link>
        </div>
    )
}

export default UnauthorizedPage
