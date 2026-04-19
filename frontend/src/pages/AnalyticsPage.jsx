import { useState, useEffect, useRef, useCallback } from 'react'
import { useToast } from '../context/ToastContext'
import { useAuth, ROLES } from '../context/AuthContext'
import ErrorModal from '../components/ErrorModal'
import analyticsService from '../services/analyticsService'

// ANALYTICS — Donut + Bar chart (all data from backend)
const AnalyticsPage = () => {
    const { showToast } = useToast()
    const { hasRole, user } = useAuth()
    const [deptData, setDeptData] = useState([])
    const [loading, setLoading] = useState(true)
    const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })
    const isFirstLoad = useRef(true)

    // Check user role
    const isSuperAdmin = hasRole(ROLES.SUPER_ADMIN)

    // Helper: extract departments array from any response shape
    const extractDepartments = (res) => {
        // res is what analyticsService.getDepartmentRisk() returns
        // analyticsService returns response.data (axios unwrap) = { success, data: [...], count }
        if (!res) return []

        // Case 1: res.data is the departments array (standard backend format)
        if (res.data && Array.isArray(res.data)) return res.data

        // Case 2: res itself is an array (direct)
        if (Array.isArray(res)) return res

        // Case 3: res.departments exists
        if (res.departments && Array.isArray(res.departments)) return res.departments

        // Case 4: res.content exists (paginated)
        if (res.content && Array.isArray(res.content)) return res.content

        // Fallback
        console.warn('Analytics: Unexpected response format:', res)
        return []
    }

    // Memoized fetch
    const fetchData = useCallback(async () => {
        try {
            const res = await analyticsService.getDepartmentRisk()
            const list = extractDepartments(res)
            console.log('Analytics data loaded:', list.length, 'departments')
            setDeptData(list)

            if (isFirstLoad.current) {
                setLoading(false)
                isFirstLoad.current = false
            }
        } catch (error) {
            console.error('Analytics fetch error:', error)
            if (isFirstLoad.current) {
                showToast('Failed to load analytics.', 'error')
                setLoading(false)
                isFirstLoad.current = false
            }
        }
    }, [showToast])

    useEffect(() => {
        fetchData()
        const interval = setInterval(() => { fetchData() }, 3000)
        return () => clearInterval(interval)
    }, [fetchData])

    // Helper: get fields from dept object (matching DepartmentRiskDTO fields)
    const getCompliance = (d) => d.complianceRate != null ? Math.round(d.complianceRate) : 0
    const getViolations = (d) => d.pendingViolationCount ?? 0
    const getDocs = (d) => d.totalDocuments ?? 0

    const totalDocs = deptData.reduce((s, d) => s + getDocs(d), 0)
    const totalViolations = deptData.reduce((s, d) => s + getViolations(d), 0)
    const totalResolved = deptData.reduce((s, d) => s + (d.resolvedViolationCount ?? 0), 0)
    const totalAllViolations = deptData.reduce((s, d) => s + (d.totalViolationCount ?? 0), 0)
    const totalLateAck = deptData.reduce((s, d) => s + (d.lateAcknowledgementCount ?? 0), 0)
    const totalEscAdmin = deptData.reduce((s, d) => s + (d.deptAdminEscalationCount ?? 0), 0)
    const totalEscSuper = deptData.reduce((s, d) => s + (d.superAdminEscalationCount ?? 0), 0)
    const totalUsers = deptData.reduce((s, d) => s + (d.totalUsers ?? 0), 0)
    const totalActiveUsers = deptData.reduce((s, d) => s + (d.activeUsers ?? 0), 0)
    const totalHighRiskUsers = deptData.reduce((s, d) => s + (d.highRiskUserCount ?? 0), 0)
    const totalRepeatOffenders = deptData.reduce((s, d) => s + (d.repeatOffenderCount ?? 0), 0)
    const totalSafetyViol = deptData.reduce((s, d) => s + (d.safetyViolationCount ?? 0), 0)
    const avgCompliance = deptData.length > 0 ? Math.round(deptData.reduce((s, d) => s + getCompliance(d), 0) / deptData.length) : 0
    const compliantPct = avgCompliance
    const violationPct = 100 - avgCompliance

    // SUPER ADMIN VIEW: All departments
    if (isSuperAdmin) {
        return (
            <div className="animate-fade-in">
                <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                    <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Analytics & Risk Assessment</h1>
                </div>
                {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading analytics...</div> : (
                    <>
                        {/* Row 1: Summary Cards */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                            <SummaryCard label="Total Documents" value={totalDocs} sub="Across all departments" />
                            <SummaryCard label="Avg Compliance" value={`${avgCompliance}%`} sub={avgCompliance >= 80 ? 'Good standing' : 'Needs improvement'} />
                            <SummaryCard label="Active Violations" value={totalViolations} sub="Require attention" />
                            <SummaryCard label="Departments" value={deptData.length} sub="Being monitored" />
                        </div>

                        {/* Row 2: Additional Stats */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                            <SummaryCard label="Total Violations" value={totalAllViolations} sub={`Resolved: ${totalResolved}`} />
                            <SummaryCard label="Late Acknowledgements" value={totalLateAck} sub="Across departments" />
                            <SummaryCard label="Safety Violations" value={totalSafetyViol} sub="Require priority" />
                            <SummaryCard label="High-Risk Users" value={totalHighRiskUsers} sub={`of ${totalUsers} total users`} />
                        </div>

                        {/* Row 3: Donut + Compliance Bars */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                            <div className="card-metro">
                                <h2 className="section-title mb-4">Overall Compliance Score</h2>
                                <div className="flex items-center justify-center gap-8">
                                    <div className="relative w-36 h-36">
                                        <svg viewBox="0 0 36 36" className="w-full h-full">
                                            <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="#E5E7EB" strokeWidth="3" />
                                            <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="#0B3C5D" strokeWidth="3" strokeDasharray={`${compliantPct}, 100`} strokeLinecap="round" />
                                        </svg>
                                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                                            <span className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{avgCompliance}%</span>
                                            <span className="text-xs text-gray-400">Compliant</span>
                                        </div>
                                    </div>
                                    <div className="space-y-3 text-sm">
                                        <div className="flex items-center gap-2"><span className="w-3 h-3 rounded" style={{ backgroundColor: '#0B3C5D' }}></span><span className="text-gray-600">Compliant: {compliantPct}%</span></div>
                                        <div className="flex items-center gap-2"><span className="w-3 h-3 rounded" style={{ backgroundColor: '#E5E7EB' }}></span><span className="text-gray-600">Non-compliant: {violationPct}%</span></div>
                                        <div className="pt-2 border-t text-xs text-gray-400" style={{ borderColor: '#D0D7DE' }}>Based on {totalDocs} documents<br />across {deptData.length} departments</div>
                                    </div>
                                </div>
                            </div>

                            <div className="card-metro">
                                <h2 className="section-title mb-4">Department Compliance Bars</h2>
                                <div className="space-y-3">
                                    {deptData.length === 0 ? <p className="text-gray-400 text-sm text-center py-4">No department data available</p> : deptData.map(dept => {
                                        const compliance = getCompliance(dept)
                                        return (
                                            <div key={dept.departmentId || dept.departmentName} className="flex items-center gap-3">
                                                <span className="text-xs text-gray-600 w-28 truncate">{dept.departmentName}</span>
                                                <div className="flex-1 h-4 bg-gray-100 rounded-full overflow-hidden">
                                                    <div className="h-full rounded-full transition-all" style={{ width: `${compliance}%`, backgroundColor: '#0B3C5D' }}></div>
                                                </div>
                                                <span className="text-xs font-medium text-gray-600 w-10 text-right">{compliance}%</span>
                                            </div>
                                        )
                                    })}
                                </div>
                            </div>
                        </div>

                        {/* Row 4: Escalations + User Overview */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                            <div className="card-metro">
                                <h2 className="section-title mb-4">Escalation Overview</h2>
                                <div className="grid grid-cols-2 gap-4 mb-4">
                                    <div className="text-center p-4 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{totalEscAdmin}</p>
                                        <p className="text-xs text-gray-500 mt-1">Dept Admin Escalations</p>
                                    </div>
                                    <div className="text-center p-4 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{totalEscSuper}</p>
                                        <p className="text-xs text-gray-500 mt-1">Super Admin Escalations</p>
                                    </div>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="text-center p-4 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{totalResolved}</p>
                                        <p className="text-xs text-gray-500 mt-1">Resolved Violations</p>
                                    </div>
                                    <div className="text-center p-4 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{totalRepeatOffenders}</p>
                                        <p className="text-xs text-gray-500 mt-1">Repeat Offenders</p>
                                    </div>
                                </div>
                            </div>

                            <div className="card-metro">
                                <h2 className="section-title mb-4">User & Workforce Overview</h2>
                                <div className="space-y-0">
                                    <DetailRow label="Total Users" value={totalUsers} />
                                    <DetailRow label="Active Users" value={totalActiveUsers} />
                                    <DetailRow label="High-Risk Users" value={totalHighRiskUsers} highlight={totalHighRiskUsers > 0} />
                                    <DetailRow label="Repeat Offenders" value={totalRepeatOffenders} highlight={totalRepeatOffenders > 0} />
                                    <DetailRow label="Late Acknowledgements" value={totalLateAck} highlight={totalLateAck > 0} />
                                    <DetailRow label="Safety Violations" value={totalSafetyViol} highlight={totalSafetyViol > 0} />
                                </div>
                            </div>
                        </div>

                        {/* Row 5: Department Performance Table */}
                        <div className="card-metro" style={{ overflow: 'hidden', padding: 0 }}>
                            <h2 className="section-title p-4 mb-0">Department Performance</h2>
                            <div className="overflow-x-auto">
                                <table className="w-full text-sm">
                                    <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                                        <th className="py-3 px-3 font-medium text-white text-center">S.No</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Department</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Docs</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Compliance</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Pending</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Resolved</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Late Ack</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Escalations</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Users</th>
                                        <th className="py-3 px-3 font-medium text-white text-center">Risk</th>
                                    </tr></thead>
                                    <tbody>{deptData.length === 0 ? <tr><td colSpan="10" className="py-6 text-center text-gray-400">No data available.</td></tr> :
                                        [...deptData].sort((a, b) => getCompliance(a) - getCompliance(b)).map((dept, idx) => {
                                            const c = getCompliance(dept)
                                            const pend = getViolations(dept)
                                            const resolved = dept.resolvedViolationCount ?? 0
                                            const late = dept.lateAcknowledgementCount ?? 0
                                            const esc = (dept.deptAdminEscalationCount ?? 0) + (dept.superAdminEscalationCount ?? 0)
                                            const users = dept.totalUsers ?? 0
                                            return (
                                                <tr key={dept.departmentId || dept.departmentName} className="border-b hover:bg-gray-50" style={{ borderColor: '#F0F0F0' }}>
                                                    <td className="py-3 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                                    <td className="py-3 px-3 font-medium text-gray-800 text-center">{dept.departmentName}</td>
                                                    <td className="py-3 px-3 text-center text-gray-600">{getDocs(dept)}</td>
                                                    <td className="py-3 px-3 text-center">
                                                        <div className="flex items-center gap-2">
                                                            <div className="flex-1 h-2 bg-gray-100 rounded-full overflow-hidden">
                                                                <div className="h-full rounded-full" style={{ width: `${c}%`, backgroundColor: '#0B3C5D' }}></div>
                                                            </div>
                                                            <span className="text-xs font-medium text-gray-600 w-10 text-right">{c}%</span>
                                                        </div>
                                                    </td>
                                                    <td className="py-3 px-3 text-center"><span className={pend > 0 ? 'font-semibold' : 'text-gray-600'} style={pend > 0 ? { color: '#0B3C5D' } : {}}>{pend}</span></td>
                                                    <td className="py-3 px-3 text-center"><span className={resolved > 0 ? 'font-semibold' : 'text-gray-600'} style={resolved > 0 ? { color: '#0B3C5D' } : {}}>{resolved}</span></td>
                                                    <td className="py-3 px-3 text-center"><span className={late > 0 ? 'font-semibold' : 'text-gray-600'} style={late > 0 ? { color: '#0B3C5D' } : {}}>{late}</span></td>
                                                    <td className="py-3 px-3 text-center"><span className={esc > 0 ? 'font-semibold' : 'text-gray-600'} style={esc > 0 ? { color: '#0B3C5D' } : {}}>{esc}</span></td>
                                                    <td className="py-3 px-3 text-center text-gray-600">{users}</td>
                                                    <td className="py-3 px-3 text-center">
                                                        <span className="px-2 py-0.5 rounded text-xs font-medium" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D' }}>{c >= 90 ? 'LOW' : c >= 75 ? 'MEDIUM' : 'HIGH'}</span>
                                                    </td>
                                                </tr>)
                                        })}</tbody>
                                </table>
                            </div>
                        </div>
                    </>)}

                <ErrorModal show={errorModal.show} title={errorModal.title} message={errorModal.message} onClose={() => setErrorModal({ show: false, title: '', message: '' })} />
            </div>
        )
    }

    // ============================================================
    // DEPARTMENT ADMIN VIEW: Only their department
    // ============================================================
    const deptInfo = deptData.length > 0 ? deptData[0] : null
    const deptCompliance = deptInfo ? getCompliance(deptInfo) : 0
    const deptViolations = deptInfo ? getViolations(deptInfo) : 0
    const deptDocs = deptInfo ? getDocs(deptInfo) : 0
    const deptRiskScore = deptInfo?.riskScore ?? 0
    const deptResolved = deptInfo?.resolvedViolationCount ?? 0
    const deptTotalViolations = deptInfo?.totalViolationCount ?? 0
    const deptLateAck = deptInfo?.lateAcknowledgementCount ?? 0
    const deptAcknowledged = deptInfo?.acknowledgedDocuments ?? 0
    const deptPendingAck = deptInfo?.pendingAcknowledgements ?? 0
    const deptAvgDelay = deptInfo?.avgDaysDelayed ?? 0
    const deptTotalUsers = deptInfo?.totalUsers ?? 0
    const deptActiveUsers = deptInfo?.activeUsers ?? 0
    const deptHighRiskUsers = deptInfo?.highRiskUserCount ?? 0
    const deptRepeatOffenders = deptInfo?.repeatOffenderCount ?? 0
    const deptEscAdmin = deptInfo?.deptAdminEscalationCount ?? 0
    const deptEscSuper = deptInfo?.superAdminEscalationCount ?? 0
    const deptSafetyViol = deptInfo?.safetyViolationCount ?? 0
    const deptTrendDir = deptInfo?.trendDirection || 'STABLE'
    const deptTrendPct = deptInfo?.violationsTrendPercentage ?? 0
    const deptViol7d = deptInfo?.violationsLast7Days ?? 0
    const deptViol30d = deptInfo?.violationsLast30Days ?? 0
    const riskLabel = deptRiskScore <= 20 ? 'LOW' : deptRiskScore <= 40 ? 'MODERATE' : deptRiskScore <= 70 ? 'HIGH' : 'CRITICAL'
    const onTimeRate = deptDocs > 0 ? Math.round((deptDocs - deptViolations) / deptDocs * 100) : 100

    return (
        <div className="animate-fade-in">
            {/* Header */}
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Analytics & Risk Assessment</h1>
                <p className="text-sm mt-2" style={{ color: 'rgba(255,255,255,0.8)' }}>{deptInfo?.departmentName || user?.department || 'Department'} — Your Department</p>
            </div>

            {loading ? (
                <div className="text-center py-10 text-gray-500 text-sm">Loading analytics...</div>
            ) : deptInfo ? (
                <>
                    {/* Row 1: Summary cards — same card-metro style */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                        <SummaryCard label="Risk Score" value={deptRiskScore} sub={riskLabel} />
                        <SummaryCard label="Compliance Score" value={`${deptCompliance}%`} sub={deptCompliance >= 80 ? 'Good standing' : 'Needs improvement'} />
                        <SummaryCard label="Total Documents" value={deptDocs} sub="In your department" />
                        <SummaryCard label="Active Violations" value={deptViolations} sub={deptViolations > 0 ? 'Require action' : 'All clear'} />
                    </div>

                    {/* Row 2: Compliance Donut + Violation Breakdown */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                        <div className="card-metro">
                            <h2 className="section-title mb-4">Department Compliance</h2>
                            <div className="flex items-center justify-center gap-8">
                                <div className="relative w-40 h-40">
                                    <svg viewBox="0 0 36 36" className="w-full h-full">
                                        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="#E5E7EB" strokeWidth="3" />
                                        <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="#0B3C5D" strokeWidth="3" strokeDasharray={`${deptCompliance}, 100`} strokeLinecap="round" />
                                    </svg>
                                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                                        <span className="text-3xl font-bold" style={{ color: '#0B3C5D' }}>{deptCompliance}%</span>
                                        <span className="text-xs text-gray-400">Compliant</span>
                                    </div>
                                </div>
                                <div className="space-y-3 text-sm">
                                    <div className="flex items-center gap-2"><span className="w-3 h-3 rounded" style={{ backgroundColor: '#0B3C5D' }}></span><span className="text-gray-600">Compliant: {deptCompliance}%</span></div>
                                    <div className="flex items-center gap-2"><span className="w-3 h-3 rounded" style={{ backgroundColor: '#E5E7EB' }}></span><span className="text-gray-600">Non-compliant: {100 - deptCompliance}%</span></div>
                                    <div className="pt-2 border-t text-xs text-gray-400" style={{ borderColor: '#D0D7DE' }}>
                                        Based on {deptDocs} documents<br />
                                        On-time rate: {onTimeRate}%
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="card-metro">
                            <h2 className="section-title mb-4">Violation Breakdown</h2>
                            <div className="space-y-4">
                                <ViolationBar label="Pending Violations" value={deptViolations} max={Math.max(deptTotalViolations, 1)} />
                                <ViolationBar label="Resolved" value={deptResolved} max={Math.max(deptTotalViolations, 1)} />
                                <ViolationBar label="Late Acknowledgements" value={deptLateAck} max={Math.max(deptTotalViolations, 1)} />
                                <ViolationBar label="Safety Violations" value={deptSafetyViol} max={Math.max(deptTotalViolations, 1)} />
                            </div>
                            <div className="pt-3 mt-3 border-t text-xs text-gray-400 flex justify-between" style={{ borderColor: '#D0D7DE' }}>
                                <span>Total: <strong className="text-gray-600">{deptTotalViolations}</strong></span>
                                <span>Resolution: <strong style={{ color: '#0B3C5D' }}>{deptTotalViolations > 0 ? Math.round(deptResolved / deptTotalViolations * 100) : 100}%</strong></span>
                            </div>
                        </div>
                    </div>

                    {/* Row 3: Activity & Trends + Department Details */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                        <div className="card-metro">
                            <h2 className="section-title mb-4">Activity & Trends</h2>
                            <div className="space-y-3">
                                <div className="flex justify-between items-center p-3 rounded" style={{ backgroundColor: '#F8FAFB', border: '1px solid #E5E7EB' }}>
                                    <span className="text-xs text-gray-600 font-medium">Last 7 Days</span>
                                    <span className="text-sm font-bold" style={{ color: '#0B3C5D' }}>{deptViol7d} violations</span>
                                </div>
                                <div className="flex justify-between items-center p-3 rounded" style={{ backgroundColor: '#F8FAFB', border: '1px solid #E5E7EB' }}>
                                    <span className="text-xs text-gray-600 font-medium">Last 30 Days</span>
                                    <span className="text-sm font-bold" style={{ color: '#0B3C5D' }}>{deptViol30d} violations</span>
                                </div>
                                <div className="flex justify-between items-center p-3 rounded" style={{ backgroundColor: '#F8FAFB', border: '1px solid #E5E7EB' }}>
                                    <span className="text-xs text-gray-600 font-medium">Trend</span>
                                    <span className="text-xs font-semibold px-3 py-1 rounded-full" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D' }}>
                                        {deptTrendDir === 'UP' ? '↑' : deptTrendDir === 'DOWN' ? '↓' : '→'} {Math.abs(deptTrendPct)}% {deptTrendDir}
                                    </span>
                                </div>
                            </div>
                            {/* Escalations */}
                            <div className="mt-4 pt-3 border-t" style={{ borderColor: '#D0D7DE' }}>
                                <p className="text-xs text-gray-500 font-medium mb-3">Escalations</p>
                                <div className="grid grid-cols-2 gap-3">
                                    <div className="text-center p-3 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-lg font-bold" style={{ color: '#0B3C5D' }}>{deptEscAdmin}</p>
                                        <p className="text-xs text-gray-500">To Dept Admin</p>
                                    </div>
                                    <div className="text-center p-3 rounded" style={{ backgroundColor: '#E8EEF3' }}>
                                        <p className="text-lg font-bold" style={{ color: '#0B3C5D' }}>{deptEscSuper}</p>
                                        <p className="text-xs text-gray-500">To Super Admin</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="card-metro">
                            <h2 className="section-title mb-4">Department Details</h2>
                            <div className="space-y-0">
                                <DetailRow label="Total Documents" value={deptDocs} />
                                <DetailRow label="Acknowledged" value={deptAcknowledged} />
                                <DetailRow label="Pending Acknowledgements" value={deptPendingAck} highlight={deptPendingAck > 0} />
                                <DetailRow label="Avg. Days Delayed" value={deptAvgDelay > 0 ? deptAvgDelay.toFixed(1) : '0'} highlight={deptAvgDelay > 2} />
                                <DetailRow label="Total Users" value={deptTotalUsers} />
                                <DetailRow label="Active Users" value={deptActiveUsers} />
                                <DetailRow label="High-Risk Users" value={deptHighRiskUsers} highlight={deptHighRiskUsers > 0} />
                                <DetailRow label="Repeat Offenders" value={deptRepeatOffenders} highlight={deptRepeatOffenders > 0} />
                            </div>
                        </div>
                    </div>

                    {/* Row 4: Risk Level Assessment */}
                    <div className="card-metro">
                        <h2 className="section-title mb-4">Risk Level Assessment</h2>
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-gray-600 text-sm mb-2">Current Risk Level</p>
                                <span className="px-4 py-2 rounded text-sm font-bold" style={{ backgroundColor: '#E8EEF3', color: '#0B3C5D' }}>
                                    {deptCompliance >= 90 ? 'LOW RISK' : deptCompliance >= 75 ? 'MEDIUM RISK' : 'HIGH RISK'}
                                </span>
                            </div>
                            <div className="text-right">
                                <p className="text-3xl font-bold" style={{ color: '#0B3C5D' }}>{onTimeRate}%</p>
                                <p className="text-xs text-gray-400">On-time rate</p>
                            </div>
                        </div>
                    </div>
                </>
            ) : (
                <div className="card-metro text-center py-10">
                    <p className="text-gray-500">No department data available</p>
                </div>
            )}

            <ErrorModal show={errorModal.show} title={errorModal.title} message={errorModal.message} onClose={() => setErrorModal({ show: false, title: '', message: '' })} />
        </div>
    )
}

// Shared SummaryCard — consistent card-metro style with #0B3C5D
const SummaryCard = ({ label, value, sub }) => (
    <div className="card-metro"><p className="text-xs text-gray-500 mb-1">{label}</p><p className="text-2xl font-bold" style={{ color: '#0B3C5D' }}>{value}</p>{sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}</div>
)

// Violation bar — uses #0B3C5D
const ViolationBar = ({ label, value, max }) => {
    const pct = max > 0 ? Math.round((value / max) * 100) : 0
    return (
        <div>
            <div className="flex justify-between mb-1">
                <span className="text-xs text-gray-600 font-medium">{label}</span>
                <span className="text-xs text-gray-500">{value} <span className="text-gray-400">({pct}%)</span></span>
            </div>
            <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, backgroundColor: '#0B3C5D' }}></div>
            </div>
        </div>
    )
}

// Detail row
const DetailRow = ({ label, value, highlight }) => (
    <div className="flex justify-between items-center py-2.5 border-b" style={{ borderColor: '#F0F0F0' }}>
        <span className="text-xs text-gray-500">{label}</span>
        <span className={`text-sm font-semibold ${highlight ? '' : 'text-gray-800'}`} style={highlight ? { color: '#0B3C5D' } : {}}>{value}</span>
    </div>
)

export default AnalyticsPage
