// APPLICATION CONSTANTS

// Departments for dropdown
export const DEPARTMENTS = [
  { id: 1, name: 'Maintenance', code: 'MAINT' },
  { id: 2, name: 'Safety & Quality', code: 'SAFE' },
  { id: 3, name: 'Human Resources', code: 'HR' },
  { id: 4, name: 'Finance & Accounts', code: 'FIN' },
  { id: 5, name: 'Legal & Compliance', code: 'LEGAL' },
  { id: 6, name: 'Engineering', code: 'ENG' },
  { id: 7, name: 'Operations', code: 'OPS' },
  { id: 8, name: 'IT & Systems', code: 'IT' },
  { id: 9, name: 'Procurement', code: 'PROC' },
  { id: 10, name: 'Administration', code: 'ADMIN' },
]

// Document types
export const DOCUMENT_TYPES = [
  { value: 'JOB_CARD', label: 'Job Card' },
  { value: 'INVOICE', label: 'Invoice' },
  { value: 'POLICY', label: 'Policy Document' },
  { value: 'SAFETY_CIRCULAR', label: 'Safety Circular' },
  { value: 'LEGAL_NOTICE', label: 'Legal Notice' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'MANUAL', label: 'Equipment Manual' },
  { value: 'REPORT', label: 'Report' },
  { value: 'MEMO', label: 'Memo' },
  { value: 'CERTIFICATE', label: 'Certificate' },
  { value: 'OTHER', label: 'Other' },
]

// Priority levels
export const PRIORITIES = [
  { value: 'HIGH', label: 'High', color: 'red' },
  { value: 'MEDIUM', label: 'Medium', color: 'yellow' },
  { value: 'LOW', label: 'Low', color: 'green' },
]

// Allowed file types
export const ALLOWED_FILE_TYPES = {
  'application/pdf': ['.pdf'],
  'application/msword': ['.doc'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
}

// Max file size (10MB)
export const MAX_FILE_SIZE = 10 * 1024 * 1024 