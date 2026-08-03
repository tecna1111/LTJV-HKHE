import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BarChart3,
  Bot,
  ChevronRight,
  Eye,
  EyeOff,
  KanbanSquare,
  LockKeyhole,
  Mail,
  MessageCircle,
  ShieldCheck,
  Sparkles,
  Video,
  Wifi,
  X,
} from 'lucide-react';
import BrandLogo from '../../../components/BrandLogo';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { login } from '../authService';
import './LoginPage.css';

const roles = [
  { id: 'STUDENT', label: 'Student', placeholder: 'Student username' },
  { id: 'LECTURER', label: 'Lecturer', placeholder: 'Lecturer username' },
  { id: 'HEAD_DEPT', label: 'Head Dept', placeholder: 'Department head username' },
  { id: 'STAFF', label: 'Staff', placeholder: 'Staff username' },
  { id: 'ADMIN', label: 'Admin', placeholder: 'Admin username' },
];

const features = [
  { icon: KanbanSquare, tone: 'amber', title: 'Integrated Team Workspace & Sprint Kanban', text: 'Agile project boards with real-time sync across all members.' },
  { icon: Video, tone: 'violet', title: 'Interactive Whiteboard & Video Calls', text: 'Collaborate visually with embedded media and live sessions.' },
  { icon: Sparkles, tone: 'green', title: 'AI-Powered Milestone Assistant', text: 'Smart planning with COSRE AI accelerating project delivery.' },
  { icon: BarChart3, tone: 'blue', title: 'Peer Review & Rubric Evaluation', text: 'Structured assessment with transparent analytics and feedback.' },
];

function LoginPage() {
  const [activeRole, setActiveRole] = useState('STUDENT');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [aiOpen, setAiOpen] = useState(true);
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();
  const role = useMemo(() => roles.find((item) => item.id === activeRole), [activeRole]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const result = await login({ username: username.trim(), password });
      if (!result.success) {
        setError(result.message || 'Không thể đăng nhập.');
        return;
      }
      setAuth(result.data.token, result.data.username, result.data.fullName, result.data.role, rememberMe);
      navigate('/dashboard', { replace: true });
    } catch (requestError) {
      setError(getApiError(requestError, 'Đăng nhập thất bại. Vui lòng kiểm tra thông tin.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <div className="auth-ambient auth-ambient-one" />
      <div className="auth-ambient auth-ambient-two" />

      <section className="auth-card" aria-label="COSRE authentication">
        <aside className="brand-panel">
          <div className="brand-grid" />
          <header className="brand-header">
            <BrandLogo inverse />
            <span className="version-pill">PBL Platform v2.4</span>
          </header>

          <div className="brand-copy">
            <h2>Empowering Project-Based Learning &amp; Real-Time Collaboration</h2>
            <p>Unified workspace for universities — connecting students, lecturers, departments and staff in one seamless environment.</p>
          </div>

          <div className="feature-list">
            {features.map(({ icon: Icon, tone, title, text }) => (
              <article className="feature-item" key={title}>
                <span className={`feature-icon ${tone}`}><Icon size={16} /></span>
                <div><h3>{title}</h3><p>{text}</p></div>
              </article>
            ))}
          </div>

          <div className="technology-pill"><span /> Powered by Java Spring Boot &amp; React Ecosystem</div>
        </aside>

        <section className="login-panel">
          <div className="login-content">
            <div className="mobile-brand"><BrandLogo compact /></div>

            <div className="role-section">
              <span className="eyebrow">Select role context</span>
              <div className="role-tabs" role="tablist" aria-label="Choose account role">
                {roles.map((item) => (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={item.id === activeRole}
                    className={item.id === activeRole ? 'active' : ''}
                    key={item.id}
                    onClick={() => { setActiveRole(item.id); setError(''); }}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="login-heading">
              <h1>Sign In to Your Workspace</h1>
              <p>Select your role and enter your institutional credentials below.</p>
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
              <label className="field-shell">
                <span>{role.label} username</span>
                <Mail size={17} />
                <input
                  autoComplete="username"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder={role.placeholder}
                  required
                />
              </label>

              <label className="field-shell">
                <span>Password</span>
                <LockKeyhole size={17} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Enter your password"
                  required
                />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Hide password' : 'Show password'}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </label>

              <div className="form-options">
                <label className="remember-control">
                  <input type="checkbox" checked={rememberMe} onChange={(event) => setRememberMe(event.target.checked)} />
                  <span className="custom-check">✓</span>
                  Remember me on this device
                </label>
                <button type="button" className="text-button" onClick={() => setError('Vui lòng liên hệ Staff hoặc Admin để đặt lại mật khẩu.')}>Forgot Password?</button>
              </div>

              {error && <div className="auth-error" role="alert">{error}</div>}

              <button className="submit-button" type="submit" disabled={submitting}>
                {submitting ? <><span className="spinner" /> Authenticating…</> : <>Sign In to Workspace <ChevronRight size={17} /></>}
              </button>
            </form>

          </div>

          <footer className="access-notice"><ShieldCheck size={14} /><span><strong>Access restricted</strong> to authorized university accounts. Accounts are imported and managed by Staff.</span></footer>
        </section>
      </section>

      <div className="system-status"><i /> System Operational <span>|</span><Wifi size={12} /> <strong>Spring Boot Security JWT Active</strong></div>

      {aiOpen ? (
        <aside className="ai-widget">
          <header><span><Bot size={15} /></span><div><strong>COSRE AI Login Helper</strong><small><i /> Online</small></div><button onClick={() => setAiOpen(false)} aria-label="Close assistant"><X size={14} /></button></header>
          <div className="ai-body"><p><MessageCircle size={15} />Need help accessing your class? <strong>Ask AI</strong> can guide you to the right support channel.</p><button onClick={() => setError('COSRE AI đang được phát triển. Vui lòng liên hệ Staff để được hỗ trợ.')}>Ask COSRE AI →</button></div>
        </aside>
      ) : (
        <button className="ai-launcher" onClick={() => setAiOpen(true)} aria-label="Open COSRE AI"><Bot size={21} /></button>
      )}
    </main>
  );
}

export default LoginPage;
