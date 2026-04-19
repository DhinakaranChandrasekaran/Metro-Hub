import { FaSubway } from 'react-icons/fa'

// GOVERNMENT HEADER — Professional gradient design
const GovHeader = () => {
    return (
        <header role="banner">
            {/* Band 1: Government Band */}
            <div className="text-xs text-center py-2 border-b" style={{ backgroundColor: '#082F4A', color: 'rgba(255,255,255,0.85)', borderColor: 'rgba(255,255,255,0.1)' }}>
                🇮🇳 भारत सरकार &nbsp;|&nbsp; Government of India &nbsp;|&nbsp; Ministry of Housing and Urban Affairs
            </div>

            {/* Band 2: MetroHub Header */}
            <div className="px-6 py-4 flex items-center justify-between" style={{ backgroundColor: '#082F4A' }}>
                <div className="flex items-center gap-5">
                    <div className="w-14 h-14 bg-white/15 rounded-full flex items-center justify-center border border-white/20 shadow-lg" style={{ backdropFilter: 'blur(10px)' }}>
                        <FaSubway className="text-white text-2xl" />
                    </div>
                    <div className="text-white">
                        <h1 className="text-2xl font-bold tracking-wide" style={{ letterSpacing: '0.12em' }}>
                            METROHUB
                        </h1>
                        <p className="text-xs text-white/75 leading-tight mt-0.5">
                            Metro Document Management &amp; Compliance System
                        </p>
                    </div>
                </div>
                <div className="text-right text-white hidden md:block">
                    <p className="text-xs text-white/70 font-medium">Toll Free Helpline</p>
                    <p className="text-lg font-bold text-white mt-1">1800-XXX-XXXX</p>
                </div>
            </div>
        </header>
    )
}

export default GovHeader
