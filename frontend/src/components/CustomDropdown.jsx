import { useState, useRef, useEffect } from 'react'
import { FaChevronDown } from 'react-icons/fa'

const CustomDropdown = ({ options, value, onChange, placeholder = 'Select option', disabled = false }) => {
    const [isOpen, setIsOpen] = useState(false)
    const dropdownRef = useRef(null)

    const selectedLabel = options.find(opt => opt.value === value)?.label || placeholder

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setIsOpen(false)
            }
        }
        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    return (
        <div className="relative" ref={dropdownRef}>
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                disabled={disabled}
                className="w-full px-3 py-2 text-left text-sm border rounded-sm bg-white flex items-center justify-between hover:border-gray-300 transition-colors"
                style={{ borderColor: '#D0D7DE', color: value ? '#1F2933' : '#6B7280' }}
            >
                <span>{selectedLabel}</span>
                <FaChevronDown className="text-xs" style={{ transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }} />
            </button>

            {isOpen && (
                <div
                    className="absolute top-full left-0 right-0 mt-1 bg-white border rounded-sm shadow-lg"
                    style={{ borderColor: '#D0D7DE', zIndex: 9999 }}
                >
                    <div className="max-h-48 overflow-y-auto">
                        {options.map((opt) => (
                            <button
                                key={opt.value}
                                type="button"
                                onClick={() => {
                                    onChange(opt.value)
                                    setIsOpen(false)
                                }}
                                className="w-full px-3 py-2.5 text-left text-sm transition-colors hover:bg-blue-50"
                                style={{
                                    backgroundColor: value === opt.value ? '#E8EEF3' : '#FFFFFF',
                                    color: value === opt.value ? '#0B3C5D' : '#1F2933',
                                    fontWeight: value === opt.value ? '600' : '400',
                                    borderBottom: '1px solid #F0F0F0'
                                }}
                            >
                                {opt.label}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}

export default CustomDropdown
