import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaSubway, FaSignInAlt } from 'react-icons/fa'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

// WELCOME PAGE — Full-screen blue welcome → login form slides in after 5s
const WelcomePage = () => {
    const navigate = useNavigate()
    const { login } = useAuth()
    const { showToast } = useToast()
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [showLogin, setShowLogin] = useState(false)

    // Show login form after 5 seconds
    useEffect(() => {
        const timer = setTimeout(() => setShowLogin(true), 5000)
        return () => clearTimeout(timer)
    }, [])

    const handleLogin = async (e) => {
        e.preventDefault()
        if (!email || !password) {
            showToast('Email and password are required.', 'error', 5000, 'bottom-right')
            return
        }
        setLoading(true)
        try {
            await login(email, password)
            showToast('Login successful. Welcome to MetroHub.', 'success')
            navigate('/dashboard')
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data?.error || 'Invalid credentials. Please try again.'
            showToast(msg, 'error', 5000, 'bottom-right')
        } finally {
            setLoading(false)
        }
    }

    return (
        <>
            <style>{`
                @keyframes welcomeFadeIn {
                    0% { opacity: 0; transform: translateY(30px); }
                    100% { opacity: 1; transform: translateY(0); }
                }
                @keyframes loginSlideIn {
                    0% { opacity: 0; transform: translateX(60px); }
                    100% { opacity: 1; transform: translateX(0); }
                }
                @keyframes pulseGlow {
                    0%, 100% { box-shadow: 0 0 0 0 rgba(255, 152, 0, 0.4); }
                    50% { box-shadow: 0 0 0 12px rgba(255, 152, 0, 0); }
                }
                @keyframes metroLine {
                    0% { width: 0; }
                    100% { width: 100%; }
                }
                @keyframes scroll-left {
                    0% { transform: translateX(100%); }
                    100% { transform: translateX(-200%); }
                }
                @keyframes trafficBlink {
                    0%, 33%, 100% { opacity: 0.3; }
                    50%, 83% { opacity: 1; }
                }
                @keyframes waveRipple {
                    0% {
                        box-shadow: 0 0 0 0 currentColor;
                    }
                    100% {
                        box-shadow: 0 0 0 20px rgba(255, 255, 255, 0);
                    }
                }
                @keyframes scroll-left-seamless {
                    0% { transform: translateX(0); }
                    100% { transform: translateX(-50%); }
                }
                .welcome-text { animation: welcomeFadeIn 1.2s ease-out forwards; }
                .welcome-text-delay { animation: welcomeFadeIn 1.2s ease-out 0.4s forwards; opacity: 0; }
                .welcome-text-delay2 { animation: welcomeFadeIn 1.2s ease-out 0.8s forwards; opacity: 0; }
                .login-slide { animation: loginSlideIn 0.8s ease-out forwards; }
                .metro-dot { animation: pulseGlow 2s ease-in-out infinite; }
                .metro-line-anim { animation: metroLine 2s ease-out 0.5s forwards; width: 0; }
                .marquee-scroll { animation: scroll-left-seamless 40s linear infinite; }
                .traffic-red { animation: trafficBlink 2.4s infinite, waveRipple 1.5s infinite; }
                .traffic-yellow { animation: trafficBlink 2.4s infinite 0.8s, waveRipple 1.5s infinite 0.5s; }
                .traffic-green { animation: trafficBlink 2.4s infinite 1.6s, waveRipple 1.5s infinite 1s; }
            `}</style>

            <div className="min-h-screen flex flex-col" style={{ backgroundColor: '#0B3C5D' }}>

                {/* Top Gov Bar with Marquee */}
                <div className="flex items-center justify-between px-6 py-2" style={{ backgroundColor: '#1A1A2E', color: 'rgba(255,255,255,0.7)', fontSize: '11px' }}>
                    <span className="font-medium text-white/80">🇮🇳 भारत सरकार | Government of India</span>
                    <div className="flex-1 ml-6 overflow-hidden">
                        <div className="text-xs whitespace-nowrap marquee-scroll flex" style={{ color: 'rgba(255,255,255,0.9)', fontSize: '11px', fontWeight: '500' }}>
                            <span className="inline-block">
                                Welcome to MetroHub — Official Document Management &amp; Compliance System for Government Metro Rail Authority | Streamlined workflows | Compliance tracking | Secure access | Digital transformation for metro operations
                            </span>
                            <span className="inline-block ml-4">
                                Welcome to MetroHub — Official Document Management &amp; Compliance System for Government Metro Rail Authority | Streamlined workflows | Compliance tracking | Secure access | Digital transformation for metro operations
                            </span>
                        </div>
                    </div>
                </div>

                {/* Main Content */}
                <div className="flex-1 relative flex overflow-hidden">

                    {/* Background grid pattern — covers entire area */}
                    <div className="absolute inset-0" style={{
                        backgroundImage: `
                            linear-gradient(90deg, rgba(255,255,255,0.015) 1px, transparent 1px),
                            linear-gradient(rgba(255,255,255,0.015) 1px, transparent 1px)`,
                        backgroundSize: '60px 60px'
                    }}></div>

                    {/* Blue metro line at bottom */}
                    <div className="absolute bottom-0 left-0 right-0 h-1 metro-line-anim" style={{ backgroundColor: '#0B3C5D', zIndex: 5 }}></div>

                    {/* LEFT HALF — Welcome text centered */}
                    <div className="relative z-10 flex items-center justify-center" style={{
                        width: showLogin ? '50%' : '100%',
                        transition: 'width 0.8s ease',
                    }}>
                        <div className="text-center px-8 w-full flex flex-col items-center" style={{ maxWidth: '100%' }}>
                            {/* Metro line decoration */}
                            <div className="flex items-center gap-2 mb-8 welcome-text justify-center">
                                <div className="w-3.5 h-3.5 rounded-full traffic-red" style={{ backgroundColor: '#FF6B6B' }}></div>
                                <div className="w-20 h-0.5 bg-white/20"></div>
                                <div className="w-3 h-3 rounded-full traffic-yellow" style={{ backgroundColor: '#FFD93D' }}></div>
                                <div className="w-20 h-0.5 bg-white/20"></div>
                                <div className="w-3.5 h-3.5 rounded-full traffic-green" style={{ backgroundColor: '#6BCB77' }}></div>
                            </div>

                            {/* Logo */}
                            <div className="flex items-center gap-4 mb-6 welcome-text justify-center">
                                <div className="w-16 h-16 bg-white/10 rounded-full flex items-center justify-center border border-white/20">
                                    <FaSubway className="text-white text-3xl" />
                                </div>
                                <div className="text-white text-center">
                                    <h1 className="text-3xl font-bold tracking-wider" style={{ letterSpacing: '0.1em' }}>METROHUB</h1>
                                    <p className="text-xs text-white/50 mt-0.5">Government Metro Rail Authority</p>
                                </div>
                            </div>

                            <h2 className="text-3xl md:text-5xl font-bold text-white mb-4 welcome-text" style={{ letterSpacing: '0.04em', lineHeight: 1.2, whiteSpace: 'nowrap' }}>
                                Welcome to MetroHub
                            </h2>
                            <p className="text-lg text-white/65 mb-2 welcome-text-delay text-center" style={{ maxWidth: '600px' }}>
                                Official Document Management &amp; Compliance System
                            </p>
                            <p className="text-base text-white/40 welcome-text-delay2 text-center" style={{ maxWidth: '600px' }}>
                                Ensuring transparent governance through digital document management
                            </p>
                        </div>
                    </div>

                    {/* RIGHT HALF — Login form centered (appears after 5s) */}
                    {showLogin && (
                        <div className="relative z-10 flex items-center justify-center login-slide" style={{
                            width: '50%',
                            borderLeft: '1px solid rgba(255,255,255,0.08)',
                        }}>
                            <div className="w-full px-10" style={{ maxWidth: '480px' }}>
                                <div className="bg-white rounded-lg shadow-2xl p-8" style={{ border: 'none !important', borderRadius: '8px', overflow: 'hidden' }}>
                                    <h3 className="text-lg font-semibold text-gray-800 mb-1 text-center">Sign In</h3>
                                    <p className="text-sm text-gray-500 text-center mb-5 pb-3 border-b" style={{ borderColor: '#D0D7DE' }}>
                                        Enter your official credentials
                                    </p>

                                    <form onSubmit={handleLogin}>
                                        <div className="mb-4">
                                            <label htmlFor="welcome-email" className="block text-sm font-medium text-gray-700 mb-1.5">
                                                Email Address <span style={{ color: '#C62828' }}>*</span>
                                            </label>
                                            <input type="email" id="welcome-email" className="input-metro" style={{ padding: '10px 12px', fontSize: '14px' }} placeholder="your.name@metrohub.in"
                                                value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
                                        </div>

                                        <div className="mb-5">
                                            <label htmlFor="welcome-password" className="block text-sm font-medium text-gray-700 mb-1.5">
                                                Password <span style={{ color: '#C62828' }}>*</span>
                                            </label>
                                            <input type="password" id="welcome-password" className="input-metro" style={{ padding: '10px 12px', fontSize: '14px' }} placeholder="Enter password"
                                                value={password} onChange={(e) => setPassword(e.target.value)} required />
                                        </div>

                                        <button type="submit" className="btn-metro-primary w-full justify-center" style={{ padding: '10px', fontSize: '14px' }} disabled={loading}>
                                            {loading ? 'Signing In...' : <><FaSignInAlt /> Sign In</>}
                                        </button>
                                    </form>

                                    <p className="text-center text-sm text-gray-400 mt-4">
                                        Need access? Contact your <a href="#admin" className="hover:underline" style={{ color: '#0B3C5D' }}>IT Admin</a>
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div style={{ backgroundColor: '#092e47' }}>
                    <div className="px-6 py-3 text-center text-xs" style={{ color: 'rgba(255,255,255,0.5)' }}>
                        <p>© {new Date().getFullYear()} MetroHub — Government Metro Rail Authority. All Rights Reserved.</p>
                        <p className="mt-1" style={{ color: 'rgba(255,255,255,0.35)' }}>MetroHub Official Portal | Digital Document Management &amp; Compliance System | For support, contact your IT Department</p>
                    </div>
                </div>
            </div>
        </>
    )
}

export default WelcomePage
