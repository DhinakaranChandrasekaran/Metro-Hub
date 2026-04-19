import { FaSubway, FaPhone, FaEnvelope, FaMapMarkerAlt, FaExternalLinkAlt } from 'react-icons/fa'

// FOOTER — Modeled after real Indian metro rail websites
const Footer = () => {
  const year = new Date().getFullYear()

  return (
    <footer style={{ backgroundColor: '#0B3C5D' }} className="text-white" role="contentinfo">

      {/* Main Footer */}
      <div className="px-6 py-10">
        <div className="flex items-stretch gap-8">

          {/* LEFT (50%): Logo + About */}
          <div className="flex gap-4" style={{ width: '50%', flexShrink: 0 }}>
            {/* Logo */}
            <div className="flex-shrink-0">
              <div className="w-48 h-48 rounded flex items-center justify-center bg-white/10 border-2 border-white/20">
                <FaSubway className="text-white text-4xl" />
              </div>
            </div>

            {/* About Section */}
            <div className="flex-1 flex flex-col justify-between">
              <div>
                <h3 className="font-bold text-2xl text-white mb-1">METROHUB</h3>
                <p className="text-base text-white/80 mb-3">Government Metro Rail Authority</p>
                <div className="border-b border-white/10 mb-3 pb-3"></div>
                <p className="text-xs text-white/50 leading-relaxed">
                  MetroHub is the official Document Management & Compliance System for Government Metro Rail Authority. It streamlines document workflows, ensures compliance tracking, and provides secure access for authorized personnel.
                </p>
              </div>
              <div className="flex items-center gap-2 text-xs text-white/40">
                <span>Visitors: <strong className="text-white/70">12,847</strong></span>
                <span>|</span>
                <span>Last Updated: <strong className="text-white/70">07 Mar 2026</strong></span>
              </div>
            </div>
          </div>

          {/* RIGHT (50%): Quick Links + Important Links + Contact Us */}
          <div className="flex gap-8" style={{ width: '50%' }}>
            {/* Quick Links */}
            <div className="flex-1 flex flex-col">
              <h4 className="font-semibold text-xs mb-3 text-white/90 pb-2 border-b border-white/10">Quick Links</h4>
              <ul className="space-y-2 text-xs text-white/50">
                <li><a href="/dashboard" className="hover:text-white transition-colors">› Dashboard</a></li>
                <li><a href="/documents" className="hover:text-white transition-colors">› Documents</a></li>
                <li><a href="/notifications" className="hover:text-white transition-colors">› Notifications</a></li>
                <li><a href="/compliance" className="hover:text-white transition-colors">› Compliance</a></li>
                <li><a href="/reports" className="hover:text-white transition-colors">› Reports</a></li>
              </ul>
            </div>

            {/* Important Links */}
            <div className="flex-1 flex flex-col">
              <h4 className="font-semibold text-xs mb-3 text-white/90 pb-2 border-b border-white/10">Important Links</h4>
              <ul className="space-y-2 text-xs text-white/50">
                <li><a href="https://www.india.gov.in" target="_blank" rel="noreferrer" className="hover:text-white transition-colors">india.gov.in <FaExternalLinkAlt className="text-white/30 inline" style={{ fontSize: '8px' }} /></a></li>
                <li><a href="https://www.delhimetrorail.com" target="_blank" rel="noreferrer" className="hover:text-white transition-colors">DMRC Official <FaExternalLinkAlt className="text-white/30 inline" style={{ fontSize: '8px' }} /></a></li>
                <li><a href="https://mohua.gov.in" target="_blank" rel="noreferrer" className="hover:text-white transition-colors">MoHUA <FaExternalLinkAlt className="text-white/30 inline" style={{ fontSize: '8px' }} /></a></li>
                <li><a href="#rti" className="hover:text-white transition-colors">› RTI Info</a></li>
              </ul>
            </div>

            {/* Contact Us */}
            <div className="flex-1 flex flex-col">
              <h4 className="font-semibold text-xs mb-3 text-white/90 pb-2 border-b border-white/10">Contact Us</h4>
              <ul className="space-y-3 text-xs text-white/50">
                <li className="flex items-start gap-2">
                  <FaMapMarkerAlt className="text-white/40 mt-0.5 flex-shrink-0" />
                  <span className="leading-tight">Metro Bhawan,<br />Fire Brigade Lane,<br />New Delhi — 110001</span>
                </li>
                <li className="flex items-start gap-2">
                  <FaPhone className="text-white/40 mt-0.5 flex-shrink-0" />
                  <span className="leading-tight">1800-XXX-XXXX<br />(Toll Free)</span>
                </li>
                <li className="flex items-start gap-2">
                  <FaEnvelope className="text-white/40 mt-0.5 flex-shrink-0" />
                  <span className="leading-tight">support@<br />metrohub.in</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="border-t border-white/10 px-6 py-3">
        <div className="flex flex-col md:flex-row justify-between items-center text-xs text-white/40 gap-2">
          <p>© {year} MetroHub — Government Metro Rail Authority. All Rights Reserved.</p>
          <div className="flex gap-3">
            <a href="/privacy" className="hover:text-white transition-colors">Privacy Policy</a>
            <span className="text-white/20">|</span>
            <a href="/terms" className="hover:text-white transition-colors">Terms of Use</a>
            <span className="text-white/20">|</span>
            <a href="/disclaimer" className="hover:text-white transition-colors">Disclaimer</a>
            <span className="text-white/20">|</span>
            <a href="/accessibility" className="hover:text-white transition-colors">Accessibility</a>
            <span className="text-white/20">|</span>
            <a href="/sitemap" className="hover:text-white transition-colors">Sitemap</a>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
