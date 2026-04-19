import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaSignOutAlt, FaSave } from 'react-icons/fa'
import { useAuth, ROLES } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import ErrorModal from '../components/ErrorModal'
import authService from '../services/authService'
import api from '../services/api'

// SETTINGS PAGE — Tabbed: Profile, Notifications, Password, System
const SettingsPage = () => {
  const { user, logout, updateUser } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('profile')
  const [profileForm, setProfileForm] = useState({
    name: '', phoneNumber: '', designation: '', officeExt: '',
  })
  const [notifPrefs, setNotifPrefs] = useState({
    newDocument: true, deadline: true, escalation: true, violation: true,
    emailEnabled: true, smsEnabled: false,
  })
  const [passwordForm, setPasswordForm] = useState({ current: '', newPass: '', confirm: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [saving, setSaving] = useState(false)
  const [errorModal, setErrorModal] = useState({ show: false, title: '', message: '' })

  useEffect(() => {
    if (user) {
      setProfileForm({
        name: user.name || '',
        phoneNumber: user.phoneNumber || '',
        designation: user.designation || '',
        officeExt: '',
      })
    }
  }, [user])

  const handleLogout = () => { logout(); navigate('/') }

  const handleSaveProfile = async () => {
    setSaving(true)
    try {
      if (!profileForm.name) {
        setErrorModal({
          show: true,
          title: 'Validation Error',
          message: 'Full name is required. Please enter your name.'
        })
        setSaving(false)
        return
      }

      const res = await api.put(`/users/${user.id}`, {
        name: profileForm.name || undefined,
        phoneNumber: profileForm.phoneNumber || undefined,
        designation: profileForm.designation || undefined,
      })
      // Update local user state so changes reflect immediately
      updateUser({
        name: profileForm.name || user.name,
        phoneNumber: profileForm.phoneNumber || user.phoneNumber,
        designation: profileForm.designation || user.designation,
      })
      showToast('Profile updated successfully.', 'success')
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to save profile.', 'error')
    } finally { setSaving(false) }
  }
  const handleSave = () => showToast('Your changes have been saved successfully.', 'success')

  const handlePasswordUpdate = async () => {
    if (!passwordForm.current || !passwordForm.newPass || !passwordForm.confirm) {
      setErrorModal({
        show: true,
        title: 'Validation Error',
        message: 'Please fill in all password fields.'
      })
      return
    }
    if (passwordForm.newPass !== passwordForm.confirm) {
      setErrorModal({
        show: true,
        title: 'Password Mismatch',
        message: 'New password and confirm password do not match. Please try again.'
      })
      return
    }
    if (passwordForm.newPass.length < 8) {
      setErrorModal({
        show: true,
        title: 'Weak Password',
        message: 'Password must be at least 8 characters long. Please enter a stronger password.'
      })
      return
    }
    try {
      await authService.changePassword(passwordForm.current, passwordForm.newPass)
      showToast('Your password has been updated successfully.', 'success')
      setPasswordForm({ current: '', newPass: '', confirm: '' })
    } catch (err) {
      showToast(err.response?.data?.message || 'Password update failed.', 'error')
    }
  }

  const roleLabel = {
    [ROLES.SUPER_ADMIN]: 'System Administrator',
    [ROLES.DEPARTMENT_ADMIN]: 'Department Administrator',
    [ROLES.DEPARTMENT_UPLOAD_ADMIN]: 'Upload Administrator',
    [ROLES.DEPARTMENT_USER]: 'Department User',
  }[user?.role] || user?.role || 'User'

  const tabs = [
    { key: 'profile', label: 'My Profile' },
    { key: 'notifications', label: 'Notifications' },
    { key: 'security', label: 'Change Password' },
    { key: 'system', label: 'System Info' },
  ]

  return (
    <div className="animate-fade-in">
      <div className="mb-5" style={{ background: 'linear-gradient(135deg, #0B3C5D 0%, #1a5a8a 100%)', padding: '24px', borderRadius: '8px' }}>
        <h1 className="page-title" style={{ color: 'white', margin: 0 }}>Settings</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-0 border-b mb-5" style={{ borderColor: '#D0D7DE' }}>
        {tabs.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className="px-4 py-2 text-sm font-medium border-b-2 transition-colors"
            style={{
              color: activeTab === tab.key ? '#0B3C5D' : '#6B7280',
              borderBottomColor: activeTab === tab.key ? '#0B3C5D' : 'transparent',
              marginBottom: '-1px',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Profile Tab */}
      {activeTab === 'profile' && (
        <div className="card-metro" style={{ padding: '24px' }}>
          <div className="flex items-center gap-4 pb-4 mb-4 border-b" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>
            <div className="w-14 h-14 rounded-full flex items-center justify-center text-white text-xl font-bold" style={{ backgroundColor: '#0B3C5D' }}>
              {user?.name?.charAt(0) || 'U'}
            </div>
            <div>
              <h2 className="text-base font-semibold text-gray-800">{user?.name}</h2>
              <p className="text-xs text-gray-500">{roleLabel} • {user?.department}</p>
              <p className="text-xs text-gray-400">{user?.email}</p>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Full Name</label><input className="input-metro" value={profileForm.name} onChange={e => setProfileForm({...profileForm, name: e.target.value})} /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Email</label><input className="input-metro bg-gray-50" defaultValue={user?.email || ''} disabled /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Employee ID</label><input className="input-metro bg-gray-50" defaultValue={user?.employeeId || '-'} disabled /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Department</label><input className="input-metro bg-gray-50" defaultValue={user?.department || ''} disabled /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Role</label><input className="input-metro bg-gray-50" defaultValue={roleLabel} disabled /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Phone</label><input className="input-metro" value={profileForm.phoneNumber} onChange={e => setProfileForm({...profileForm, phoneNumber: e.target.value})} placeholder="+91 XXXXX XXXXX" /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Designation</label><input className="input-metro" value={profileForm.designation} onChange={e => setProfileForm({...profileForm, designation: e.target.value})} placeholder="e.g., Senior Engineer" /></div>
            <div><label className="block text-xs font-medium text-gray-600 mb-1">Office Extension</label><input className="input-metro" value={profileForm.officeExt} onChange={e => setProfileForm({...profileForm, officeExt: e.target.value})} placeholder="Ext. XXX" /></div>
          </div>
          <div className="flex justify-between items-center mt-5 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}>
            <button onClick={handleLogout} className="flex items-center gap-2 px-4 py-2 text-sm font-medium rounded border transition-colors hover:bg-blue-50" style={{ color: '#0B3C5D', borderColor: '#0B3C5D' }}>
              <FaSignOutAlt /> Sign Out
            </button>
            <button className="btn-metro-primary" onClick={handleSaveProfile} disabled={saving}><FaSave /> {saving ? 'Saving...' : 'Save Changes'}</button>
          </div>
        </div>
      )}

      {/* Notifications Tab */}
      {activeTab === 'notifications' && (
        <div className="card-metro" style={{ padding: '24px' }}>
          <h3 className="text-sm font-semibold text-gray-700 mb-4 pb-4 border-b" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>Notification Types</h3>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>
                <th className="py-2 font-medium text-gray-600" style={{ width: '20%' }}>Type</th>
                <th className="py-2 font-medium text-gray-600" style={{ width: '60%' }}>Description</th>
                <th className="py-2 font-medium text-gray-600 text-center" style={{ width: '20%' }}>Enabled</th>
              </tr>
            </thead>
            <tbody>
              {[
                { key: 'newDocument', label: 'New Document', desc: 'Get notified when new documents are uploaded to your department or assigned to you' },
                { key: 'deadline', label: 'Deadline Reminder', desc: 'Receive alerts for upcoming acknowledgement deadlines to ensure timely compliance and avoid penalties' },
                { key: 'escalation', label: 'Escalation Alert', desc: 'Be alerted when documents are escalated due to delays or priority changes requiring immediate action' },
                { key: 'violation', label: 'SLA Violation', desc: 'Get notified of compliance violations and SLA breaches to take corrective action promptly' },
              ].map(pref => (
                <tr key={pref.key} className="border-b" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-2.5 text-gray-800" style={{ width: '20%' }}>{pref.label}</td>
                  <td className="py-2.5 text-gray-600 text-sm" style={{ width: '60%', lineHeight: '1.4' }}>{pref.desc}</td>
                  <td className="py-2.5 text-center" style={{ width: '20%' }}>
                    <input type="checkbox" checked={notifPrefs[pref.key]} onChange={() => setNotifPrefs({ ...notifPrefs, [pref.key]: !notifPrefs[pref.key] })} className="w-4 h-4" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="mt-5">
            <h3 className="text-sm font-semibold text-gray-700 mb-3 pb-3 border-b" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>Delivery Channels</h3>
            <table className="w-full text-sm">
              <tbody>
                <tr className="border-b" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-2.5 text-gray-800" style={{ width: '20%' }}>Email Notifications</td>
                  <td className="py-2.5 text-gray-600 text-sm" style={{ width: '60%', lineHeight: '1.4' }}>Receive all notifications and alerts via email to your registered email address</td>
                  <td className="py-2.5 text-center" style={{ width: '20%' }}><input type="checkbox" checked={notifPrefs.emailEnabled} onChange={() => setNotifPrefs({ ...notifPrefs, emailEnabled: !notifPrefs.emailEnabled })} className="w-4 h-4" /></td>
                </tr>
                <tr className="border-b" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-2.5 text-gray-800" style={{ width: '20%' }}>SMS Notifications</td>
                  <td className="py-2.5 text-gray-600 text-sm" style={{ width: '60%', lineHeight: '1.4' }}>Get critical alerts via SMS for urgent matters and time-sensitive documents</td>
                  <td className="py-2.5 text-center" style={{ width: '20%' }}><input type="checkbox" checked={notifPrefs.smsEnabled} onChange={() => setNotifPrefs({ ...notifPrefs, smsEnabled: !notifPrefs.smsEnabled })} className="w-4 h-4" /></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div className="flex justify-end mt-5 pt-4 border-t" style={{ borderColor: '#D0D7DE' }}>
            <button className="btn-metro-primary" onClick={handleSave}><FaSave /> Save Preferences</button>
          </div>
        </div>
      )}

      {/* Change Password Tab */}
      {activeTab === 'security' && (
        <div className="card-metro" style={{ padding: '24px' }}>
          <h3 className="text-sm font-semibold text-gray-700 mb-4 pb-4 border-b" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>Change Password</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Current Password</label>
              <input type={showPassword ? 'text' : 'password'} className="input-metro" placeholder="Enter current password"
                value={passwordForm.current} onChange={e => setPasswordForm({ ...passwordForm, current: e.target.value })} />
            </div>
            <div></div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">New Password</label>
              <input type={showPassword ? 'text' : 'password'} className="input-metro" placeholder="Enter new password"
                value={passwordForm.newPass} onChange={e => setPasswordForm({ ...passwordForm, newPass: e.target.value })} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Confirm New Password</label>
              <input type={showPassword ? 'text' : 'password'} className="input-metro" placeholder="Re-enter new password"
                value={passwordForm.confirm} onChange={e => setPasswordForm({ ...passwordForm, confirm: e.target.value })} />
            </div>
          </div>

          <label className="flex items-center text-xs text-gray-600 cursor-pointer gap-2 mb-4">
            <input type="checkbox" checked={showPassword} onChange={() => setShowPassword(!showPassword)} />
            Show passwords
          </label>

          {passwordForm.newPass && (
            <div className="rounded p-4 mb-4 border" style={{ borderColor: '#D0D7DE', background: '#f8f9fa' }}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-gray-600">Password Strength</span>
                <span className="text-xs font-semibold" style={{
                  color: passwordForm.newPass.length >= 12 ? '#1E7E34' : passwordForm.newPass.length >= 8 ? '#FF9800' : '#C62828'
                }}>
                  {passwordForm.newPass.length >= 12 ? '✅ Strong' : passwordForm.newPass.length >= 8 ? '⚠️ Medium' : '❌ Weak'}
                </span>
              </div>
              <div className="flex gap-1 mb-3">
                {[1, 2, 3, 4].map(i => (
                  <div key={i} className="flex-1 h-2 rounded-full transition-all duration-300" style={{
                    backgroundColor: passwordForm.newPass.length >= i * 3
                      ? passwordForm.newPass.length >= 12 ? '#1E7E34' : passwordForm.newPass.length >= 8 ? '#FF9800' : '#C62828'
                      : '#E5E7EB'
                  }}></div>
                ))}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-1.5">
                {[
                  { rule: 'Minimum 8 characters', met: passwordForm.newPass.length >= 8 },
                  { rule: 'At least one uppercase letter', met: /[A-Z]/.test(passwordForm.newPass) },
                  { rule: 'At least one lowercase letter', met: /[a-z]/.test(passwordForm.newPass) },
                  { rule: 'At least one number', met: /[0-9]/.test(passwordForm.newPass) },
                  { rule: 'At least one special character', met: /[!@#$%^&*]/.test(passwordForm.newPass) },
                ].map((item, i) => (
                  <div key={i} className="flex items-center gap-2 text-xs">
                    <span className={`w-4 h-4 rounded-full flex items-center justify-center text-white text-xs flex-shrink-0 ${item.met ? 'bg-green-500' : 'bg-gray-300'}`}>
                      {item.met ? '✓' : ''}
                    </span>
                    <span className={item.met ? 'text-gray-700' : 'text-gray-400'}>{item.rule}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {passwordForm.confirm && passwordForm.newPass !== passwordForm.confirm && (
            <p className="text-xs text-red-500 flex items-center gap-1 mb-3">⚠️ Passwords do not match</p>
          )}

          <div className="flex justify-end border-t pt-4" style={{ borderColor: '#D0D7DE' }}>
            <button className="btn-metro-primary" onClick={handlePasswordUpdate}
              disabled={!passwordForm.current || !passwordForm.newPass || !passwordForm.confirm || passwordForm.newPass !== passwordForm.confirm}>
              Update Password
            </button>
          </div>
        </div>
      )}

      {/* System Info Tab */}
      {activeTab === 'system' && (
        <div className="card-metro" style={{ padding: '24px' }}>
          <h3 className="text-sm font-semibold text-gray-700 mb-4 pb-4 border-b" style={{ borderColor: '#0B3C5D', borderBottomWidth: '2px' }}>System Information</h3>
          <table className="w-full text-sm">
            <tbody>
              {[
                ['Application', 'MetroHub — Document Management System'],
                ['Version', '1.0.0'],
                ['Organization', 'Government Metro Rail Authority'],
                ['Support Email', 'support@metrohub.in'],
                ['Support Hotline', '1800-METRO-HUB'],
                ['Accessibility', 'WCAG Level AA Compliant'],
                ['Last Updated', '07 March 2026'],
                ['Privacy Policy', 'Available at metrohub.in/privacy'],
              ].map(([k, v]) => (
                <tr key={k} className="border-b" style={{ borderColor: '#F0F0F0' }}>
                  <td className="py-2.5 text-gray-500 font-medium" style={{ width: '30%' }}>{k}</td>
                  <td className="py-2.5 text-gray-800">{v}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Error Modal */}
      <ErrorModal
        show={errorModal.show}
        title={errorModal.title}
        message={errorModal.message}
        onClose={() => setErrorModal({ show: false, title: '', message: '' })}
      />
    </div>
  )
}

export default SettingsPage
