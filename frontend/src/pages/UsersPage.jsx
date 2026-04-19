import { useState, useEffect } from 'react'
import { FaEdit, FaTrash, FaToggleOn, FaToggleOff, FaSave, FaTimes, FaUserPlus, FaUser } from 'react-icons/fa'
import { useToast } from '../context/ToastContext'
import { useAuth, ROLES } from '../context/AuthContext'
import CustomDropdown from '../components/CustomDropdown'
import ErrorModal from '../components/ErrorModal'
import userService from '../services/userService'

const UsersPage = () => {
    const { showToast } = useToast()
    const { user: currentUser } = useAuth()
    const isSuperAdmin = currentUser?.role === ROLES.SUPER_ADMIN
    const isDeptAdmin = currentUser?.role === ROLES.DEPARTMENT_ADMIN
    const [deptFilter, setDeptFilter] = useState('ALL')
    const [roleFilter, setRoleFilter] = useState('ALL')
    const [showAddUser, setShowAddUser] = useState(false)
    const [showEditModal, setShowEditModal] = useState(false)
    const [editTarget, setEditTarget] = useState(null)
    const [deleteModal, setDeleteModal] = useState({ show: false, userId: null, userName: '' })
    const [deleting, setDeleting] = useState(false)
    const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })
    const [userForm, setUserForm] = useState({ name: '', email: '', password: '', phoneNumber: '', employeeId: '', designation: '', role: isSuperAdmin ? 'DEPARTMENT_ADMIN' : 'DEPARTMENT_USER', department: isSuperAdmin ? '' : (currentUser?.departmentName || ''), departmentId: isSuperAdmin ? '' : (currentUser?.departmentId || '') })
    const [users, setUsers] = useState([])
    const [loading, setLoading] = useState(true)
    const departments = ['Engineering', 'Safety & Quality', 'Finance & Accounts', 'Operations', 'Human Resources', 'Legal & Compliance', 'IT & Systems', 'Procurement', 'Maintenance', 'Administration']
    const roleLabels = { SUPER_ADMIN: 'Super Admin', DEPARTMENT_ADMIN: 'Dept Admin', DEPARTMENT_UPLOAD_ADMIN: 'Upload Admin', DEPARTMENT_USER: 'User' }

    // Custom Dropdown Options
    const deptFilterOptions = [{ value: 'ALL', label: 'All Departments' }, ...departments.map(d => ({ value: d, label: d }))]
    const roleOptions = isSuperAdmin ? [{ value: 'DEPARTMENT_ADMIN', label: 'Dept Admin' }, { value: 'DEPARTMENT_UPLOAD_ADMIN', label: 'Upload Admin' }, { value: 'DEPARTMENT_USER', label: 'User' }] : [{ value: 'DEPARTMENT_UPLOAD_ADMIN', label: 'Upload Admin' }, { value: 'DEPARTMENT_USER', label: 'User' }]
    const deptOptions = isSuperAdmin ? [{ value: '', label: 'Select' }, ...departments.map(d => ({ value: d, label: d }))] : [{ value: currentUser?.departmentName || '', label: currentUser?.departmentName || '' }]

    useEffect(() => { fetchUsers() }, [])

    const fetchUsers = async () => {
        setLoading(true)
        try {
            let data
            if (isSuperAdmin) {
                // Super Admin: Get ALL users
                data = await userService.getAll()
                const list = data.content || data || []
                data = Array.isArray(list) ? list : []
            } else if (isDeptAdmin && currentUser?.departmentId) {
                // Department Admin: Get only users from their department (Upload Admin & Department User)
                data = await userService.getByDepartment(currentUser.departmentId)
                const list = data.content || data || []
                data = Array.isArray(list) ? list.filter(u => u.role !== 'SUPER_ADMIN' && u.role !== 'DEPARTMENT_ADMIN') : []
            } else {
                data = []
            }
            setUsers(Array.isArray(data) ? data : [])
        } catch { showToast('Failed to load users.', 'error') }
        finally { setLoading(false) }
    }

    let filtered = users
    if (deptFilter !== 'ALL') filtered = filtered.filter(u => (u.departmentName || u.department || '') === deptFilter)
    if (roleFilter !== 'ALL') filtered = filtered.filter(u => u.role === roleFilter)

    const toggleActive = async (id) => {
        const targetUser = users.find(u => u.id === id)
        try {
            await userService.toggleStatus(id, targetUser?.isActive !== false)
            await fetchUsers()
            showToast('User status updated.', 'success')
        } catch { showToast('Failed to update status.', 'error') }
    }

    const deleteUser = async (id) => {
        const u = users.find(x => x.id === id)
        setDeleteModal({ show: true, userId: id, userName: u?.name || 'this user' })
    }

    const confirmDelete = async () => {
        setDeleting(true)
        try {
            await userService.delete(deleteModal.userId)
            await fetchUsers()
            showToast(`${deleteModal.userName} removed.`, 'success')
        } catch { showToast('Failed to delete user.', 'error') }
        finally { setDeleting(false); setDeleteModal({ show: false, userId: null, userName: '' }) }
    }

    const startEdit = u => {
        setEditTarget(u)
        setUserForm({
            name: u.name || '', email: u.email || '', password: '',
            phoneNumber: u.phoneNumber || '', employeeId: u.employeeId || '',
            designation: u.designation || '', role: u.role || 'DEPARTMENT_USER',
            department: u.departmentName || u.department || '',
            departmentId: u.departmentId || '',
        })
        setShowEditModal(true)
    }

    const saveEdit = async () => {
        if (!editTarget) return

        // Validation for Department Admin
        if (isDeptAdmin) {
            // Check 1: Cannot change role to Dept Admin
            if (userForm.role === 'DEPARTMENT_ADMIN') {
                setErrorModal({
                    show: true,
                    title: 'Permission Denied',
                    message: `You cannot assign the Department Admin role. You can only manage Upload Admin or regular User roles within your department.`
                })
                return
            }
        }

        try {
            await userService.update(editTarget.id, {
                name: userForm.name || undefined,
                email: userForm.email || undefined,
                phoneNumber: userForm.phoneNumber || undefined,
                designation: userForm.designation || undefined,
                role: userForm.role || undefined,
            })
            setShowEditModal(false)
            setEditTarget(null)
            await fetchUsers()
            showToast('User updated.', 'success')
        } catch (e) { showToast(e.response?.data?.message || 'Failed to update user.', 'error') }
    }

    const addUser = async () => {
        if (!userForm.name || !userForm.email || !userForm.password || !userForm.department) return

        // Validation for Department Admin
        if (isDeptAdmin) {
            // Check 1: Department Admin cannot add users from other departments
            if (userForm.department !== currentUser?.departmentName) {
                setErrorModal({
                    show: true,
                    title: 'Department Mismatch',
                    message: `You can only add users to your department (${currentUser?.departmentName}). You cannot add users from other departments.`
                })
                return
            }

            // Check 2: Department Admin cannot add Dept Admin users
            if (userForm.role === 'DEPARTMENT_ADMIN') {
                setErrorModal({
                    show: true,
                    title: 'Insufficient Permissions',
                    message: `You don't have permission to create Department Admin users. You can only create Upload Admin or regular Users within your department.`
                })
                return
            }
        }

        try {
            await userService.create(userForm)
            setShowAddUser(false)
            setUserForm({ name: '', email: '', password: '', phoneNumber: '', employeeId: '', designation: '', role: 'DEPARTMENT_USER', department: '', departmentId: '' })
            await fetchUsers()
            showToast('User created.', 'success')
        } catch (e) { showToast(e.response?.data?.message || 'Failed to create user.', 'error') }
    }

    return (
        <div className="animate-fade-in">
            <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
                <h1 className="page-title" style={{ color: 'white', margin: 0 }}>User Management</h1>
            </div>
            <div className="card-metro mb-4">
                <div className="flex gap-3 flex-wrap items-center justify-between">
                    <div className="flex gap-3 flex-wrap items-center">
                        {/* Role Filter Buttons - Both Super Admin & Dept Admin */}
                        <div className="flex gap-2 flex-wrap">
                            {[{ key: 'ALL', label: 'All Roles' }, { key: 'DEPARTMENT_ADMIN', label: 'Dept Admin' }, { key: 'DEPARTMENT_UPLOAD_ADMIN', label: 'Upload Admin' }, { key: 'DEPARTMENT_USER', label: 'Users' }]
                                .filter(r => isSuperAdmin || (r.key !== 'DEPARTMENT_ADMIN'))
                                .map(r => (
                                    <button key={r.key} onClick={() => setRoleFilter(r.key)} className={`flex items-center gap-2 px-3 py-2 text-sm rounded border transition-colors ${roleFilter === r.key ? 'text-white border-transparent' : 'text-gray-600 border-gray-200 hover:bg-gray-50'}`} style={roleFilter === r.key ? { backgroundColor: '#0B3C5D' } : {}}>{r.label}</button>
                            ))}
                        </div>

                        {/* Department Filter Dropdown - Super Admin Only */}
                        {isSuperAdmin && (
                            <div className="w-56">
                                <CustomDropdown options={deptFilterOptions} value={deptFilter} onChange={setDeptFilter} />
                            </div>
                        )}
                    </div>
                    <button className="btn-metro-primary text-sm" onClick={() => {
                        const defaultRole = isSuperAdmin ? 'DEPARTMENT_ADMIN' : 'DEPARTMENT_USER'
                        const defaultDept = isSuperAdmin ? '' : (currentUser?.departmentName || '')
                        const defaultDeptId = isSuperAdmin ? '' : (currentUser?.departmentId || '')
                        setUserForm({ name: '', email: '', password: '', phoneNumber: '', employeeId: '', designation: '', role: defaultRole, department: defaultDept, departmentId: defaultDeptId })
                        setShowAddUser(true)
                    }}><FaUserPlus /> Add User</button>
                </div>
            </div>
            <div className="card-metro mb-4" style={{ overflow: 'hidden', padding: 0 }}>
                <div className="overflow-x-auto">
                {loading ? <div className="text-center py-10 text-gray-500 text-sm">Loading users...</div> : filtered.length === 0 ? <div className="text-center py-10 text-gray-400 text-sm">No users found.</div> : (
                    <table className="w-full text-sm">
                        <thead><tr style={{ backgroundColor: '#0B3C5D', color: 'white' }}>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '4%', textAlign: 'center' }}>S.No</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '16%', textAlign: 'center' }}>Name</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '18%', textAlign: 'center' }}>Email</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '10%', textAlign: 'center' }}>Role</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '13%', textAlign: 'center' }}>Department</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '11%', textAlign: 'center' }}>Designation</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '8%', textAlign: 'center' }}>Status</th>
                            <th className="py-3 px-3 font-medium text-white" style={{ width: '12%', textAlign: 'center' }}>Actions</th>
                        </tr></thead>
                        <tbody>{filtered.map((u, idx) => (
                            <tr key={u.id} className={`border-b ${!(u.isActive !== false) ? 'opacity-50' : ''}`} style={{ borderColor: '#F0F0F0' }}>
                                <td className="py-2.5 px-3 text-gray-500 text-xs font-medium text-center">{idx + 1}</td>
                                <td className="py-2.5 px-3 font-medium text-gray-800 text-center">{u.name}</td>
                                <td className="py-2.5 px-3 text-gray-500 text-xs text-center">{u.email}</td>
                                <td className="py-2.5 px-3 text-center"><span className={`px-1.5 py-0.5 rounded text-xs font-medium ${u.role === 'SUPER_ADMIN' ? 'bg-purple-50 text-purple-800' : u.role === 'DEPARTMENT_ADMIN' ? 'bg-blue-50 text-blue-800' : u.role === 'DEPARTMENT_UPLOAD_ADMIN' ? 'bg-green-50 text-green-800' : 'bg-gray-50 text-gray-600'}`}>{roleLabels[u.role] || u.role}</span></td>
                                <td className="py-2.5 px-3 text-gray-600 text-xs text-center">{u.departmentName || u.department || '-'}</td>
                                <td className="py-2.5 px-3 text-gray-500 text-xs text-center">{u.designation || '-'}</td>
                                <td className="py-2.5 px-3 text-center">{u.isActive !== false ? <span className="text-xs text-green-700">Active</span> : <span className="text-xs text-gray-400">Disabled</span>}</td>
                                <td className="py-2.5 px-3 text-center">
                                    <button onClick={() => startEdit(u)} className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded" title="Edit"><FaEdit /></button>
                                    <button onClick={() => toggleActive(u.id)} className="p-1.5 text-gray-400 hover:bg-gray-100 rounded" title="Toggle Status">{u.isActive !== false ? <FaToggleOn className="text-green-500" /> : <FaToggleOff />}</button>
                                    <button onClick={() => { if (!deleting) deleteUser(u.id) }} disabled={deleting} className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded" title="Delete" style={{ opacity: deleting ? 0.5 : 1, cursor: deleting ? 'not-allowed' : 'pointer' }}><FaTrash /></button>
                                </td>
                            </tr>
                        ))}</tbody>
                    </table>)}
                </div>
            </div>

            {/* ADD USER MODAL */}
            {showAddUser && (
                <div className="modal-overlay" onClick={() => setShowAddUser(false)}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Add New User</h3>
                            </div>
                            <div className="space-y-3">
                                <div><label className="label-metro">Full Name *</label><input className="input-metro" value={userForm.name} onChange={e => setUserForm({ ...userForm, name: e.target.value })} /></div>
                                <div><label className="label-metro">Email *</label><input className="input-metro" type="email" value={userForm.email} onChange={e => setUserForm({ ...userForm, email: e.target.value })} /></div>
                                <div><label className="label-metro">Password *</label><input className="input-metro" type="password" value={userForm.password} onChange={e => setUserForm({ ...userForm, password: e.target.value })} /></div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div><label className="label-metro">Phone</label><input className="input-metro" value={userForm.phoneNumber} onChange={e => setUserForm({ ...userForm, phoneNumber: e.target.value })} placeholder="+91 XXXXX XXXXX" /></div>
                                    <div><label className="label-metro">Employee ID</label><input className="input-metro" value={userForm.employeeId} onChange={e => setUserForm({ ...userForm, employeeId: e.target.value })} /></div>
                                </div>
                                <div><label className="label-metro">Designation</label><input className="input-metro" value={userForm.designation} onChange={e => setUserForm({ ...userForm, designation: e.target.value })} placeholder="e.g., Senior Engineer" /></div>
                                <div><label className="label-metro">Role</label><CustomDropdown options={roleOptions} value={userForm.role} onChange={val => setUserForm({ ...userForm, role: val })} /></div>
                                <div><label className="label-metro">Department *</label><CustomDropdown options={deptOptions} value={userForm.department} onChange={val => setUserForm({ ...userForm, department: val })} /></div>
                            </div>
                            <div className="flex gap-3 justify-end mt-5 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}>
                                <button className="btn-metro-reset" onClick={() => setShowAddUser(false)}>Cancel</button>
                                <button className="btn-metro-primary" onClick={addUser} disabled={!userForm.name || !userForm.email || !userForm.password || !userForm.department}>Add User</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* EDIT USER MODAL — Centered with all fields */}
            {showEditModal && editTarget && (
                <div className="modal-overlay" onClick={() => { setShowEditModal(false); setEditTarget(null) }}>
                    <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Edit User — {editTarget.name}</h3>
                            </div>
                            <div className="space-y-3">
                                <div><label className="label-metro">Full Name</label><input className="input-metro" value={userForm.name} onChange={e => setUserForm({ ...userForm, name: e.target.value })} /></div>
                                <div><label className="label-metro">Email</label><input className="input-metro" value={userForm.email} onChange={e => setUserForm({ ...userForm, email: e.target.value })} /></div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div><label className="label-metro">Phone</label><input className="input-metro" value={userForm.phoneNumber} onChange={e => setUserForm({ ...userForm, phoneNumber: e.target.value })} placeholder="+91 XXXXX XXXXX" /></div>
                                    <div><label className="label-metro">Employee ID</label><input className="input-metro bg-gray-50" value={userForm.employeeId} disabled /></div>
                                </div>
                                <div><label className="label-metro">Designation</label><input className="input-metro" value={userForm.designation} onChange={e => setUserForm({ ...userForm, designation: e.target.value })} placeholder="e.g., Senior Engineer" /></div>
                                <div className="grid grid-cols-2 gap-3">
                                    <div><label className="label-metro">Role</label><CustomDropdown options={roleOptions} value={userForm.role} onChange={val => setUserForm({ ...userForm, role: val })} /></div>
                                    <div><label className="label-metro">Department</label><input className="input-metro bg-gray-50" value={userForm.department} disabled /></div>
                                </div>
                                <div><label className="label-metro">Status</label><p className="text-sm mt-1">{editTarget.isActive !== false ? <span className="text-green-700 font-medium">✓ Active</span> : <span className="text-gray-400">✗ Disabled</span>}</p></div>
                            </div>
                            <div className="flex gap-3 justify-end mt-5 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}>
                                <button className="btn-metro-reset" onClick={() => { setShowEditModal(false); setEditTarget(null) }}>Cancel</button>
                                <button className="btn-metro-primary" onClick={saveEdit}><FaSave /> Save Changes</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete User Modal - Notifications Style */}
            {deleteModal.show && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ maxWidth: '500px' }}>
                        <div style={{ padding: '20px' }}>
                            <div style={{ paddingBottom: '12px', marginBottom: '20px' }}>
                                <h3 className="section-title" style={{ margin: 0, color: '#0B3C5D' }}>Delete User</h3>
                            </div>

                            <div className="space-y-0">
                                <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">User Name</span>
                                    <p className="text-sm font-semibold text-gray-800 mt-2 break-words">{deleteModal.userName}</p>
                                </div>

                                <div className="py-3 border-b" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Action</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        Permanently remove this user from the system
                                    </p>
                                </div>

                                <div className="py-3" style={{ borderColor: '#F0F0F0' }}>
                                    <span className="text-sm font-semibold text-gray-600">Note</span>
                                    <p className="text-sm text-gray-800 mt-2">
                                        User records and activity history will be preserved for audit purposes. This action cannot be undone.
                                    </p>
                                </div>
                            </div>

                            <div className="mt-5 pt-4 border-t flex gap-3 justify-end" style={{ borderColor: '#D0D7DE' }}>
                            <button
                                onClick={() => setDeleteModal({ show: false, userId: null, userName: '' })}
                                disabled={deleting}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #D0D7DE',
                                    borderRadius: '3px',
                                    backgroundColor: '#FFFFFF',
                                    color: '#0B3C5D',
                                    cursor: deleting ? 'not-allowed' : 'pointer',
                                    opacity: deleting ? 0.6 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deleting && (e.target.style.backgroundColor = '#E8EEF3')}
                                onMouseLeave={(e) => !deleting && (e.target.style.backgroundColor = '#FFFFFF')}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDelete}
                                disabled={deleting}
                                style={{
                                    padding: '8px 16px',
                                    fontSize: '14px',
                                    fontWeight: '500',
                                    border: '1px solid #0B3C5D',
                                    borderRadius: '3px',
                                    backgroundColor: deleting ? '#7FA3BC' : '#0B3C5D',
                                    color: 'white',
                                    cursor: deleting ? 'not-allowed' : 'pointer',
                                    opacity: deleting ? 0.7 : 1,
                                    transition: 'all 0.15s ease'
                                }}
                                onMouseEnter={(e) => !deleting && (e.target.style.backgroundColor = '#0E4D78')}
                                onMouseLeave={(e) => !deleting && (e.target.style.backgroundColor = '#0B3C5D')}
                            >
                                {deleting ? 'Deleting...' : 'Delete'}
                            </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Error Modal - Reusable Component */}
            <ErrorModal
                show={errorModal.show}
                title={errorModal.title}
                message={errorModal.message}
                onClose={() => setErrorModal({ show: false, title: '', message: '' })}
            />
        </div>
    )
}
export default UsersPage
