import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Eye, EyeOff, Sparkles, Loader2, AlertCircle } from 'lucide-react';
import { login as loginApi, register as registerApi } from '../api/auth';
import { useAuth } from '../context/AuthContext';

export default function AuthPage() {
  const { login } = useAuth();
  const [mode,    setMode]    = useState('login'); // 'login' | 'register'
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');
  const [showPw,  setShowPw]  = useState(false);

  const [form, setForm] = useState({
    fullName: '', username: '', email: '', password: '',
  });

  const handle = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    setError('');
  };

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const payload = mode === 'login'
        ? { email: form.email, password: form.password }
        : { fullName: form.fullName, username: form.username, email: form.email, password: form.password };

      const res = mode === 'login'
        ? await loginApi(payload)
        : await registerApi(payload);

      login(res.data);
    } catch (err) {
      const msg = err.response?.data?.message
        || err.response?.data?.fieldErrors
            ? Object.values(err.response.data.fieldErrors || {}).join(', ')
            : 'Something went wrong. Please try again.';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* Background orbs */}
      <div className="orb orb-1" />
      <div className="orb orb-2" />

      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden items-center justify-center p-16">
        <div className="absolute inset-0 bg-gradient-to-br from-violet-900/30 via-dark-800 to-dark-900" />
        <div className="absolute inset-0"
          style={{
            backgroundImage: 'radial-gradient(circle at 30% 40%, rgba(124,58,237,0.15) 0%, transparent 60%), radial-gradient(circle at 70% 80%, rgba(168,85,247,0.1) 0%, transparent 50%)',
          }}
        />

        {/* Grid lines */}
        <div className="absolute inset-0 opacity-5"
          style={{
            backgroundImage: 'linear-gradient(rgba(124,58,237,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(124,58,237,0.5) 1px, transparent 1px)',
            backgroundSize: '60px 60px',
          }}
        />

        <div className="relative z-10 text-center">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            <div className="flex items-center justify-center mb-6">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-violet-600 to-purple-600 flex items-center justify-center shadow-glow-lg animate-float">
                <Sparkles className="w-10 h-10 text-white" />
              </div>
            </div>
            <h1 className="text-5xl font-black mb-4">
              <span className="glow-text">VisionForge</span>
              <span className="text-white"> AI</span>
            </h1>
            <p className="text-slate-400 text-lg max-w-sm mx-auto leading-relaxed">
              Transform your imagination into stunning visuals with the power of AI
            </p>

            <div className="mt-12 grid grid-cols-3 gap-4 max-w-xs mx-auto">
              {['Ultra HD', 'Fast AI', 'Secure'].map((feat, i) => (
                <motion.div
                  key={feat}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.3 + i * 0.1 }}
                  className="glass-card p-3 text-center"
                >
                  <p className="text-xs font-semibold text-violet-400">{feat}</p>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 relative z-10">
        <motion.div
          initial={{ opacity: 0, x: 40 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6 }}
          className="w-full max-w-md"
        >
          {/* Logo (mobile only) */}
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-600 to-purple-600 flex items-center justify-center shadow-glow-sm">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <span className="text-xl font-bold glow-text">VisionForge AI</span>
          </div>

          <div className="glass-card p-8">
            {/* Tab switcher */}
            <div className="flex bg-dark-800 rounded-xl p-1 mb-8">
              {['login', 'register'].map((m) => (
                <button
                  key={m}
                  onClick={() => { setMode(m); setError(''); }}
                  className={`flex-1 py-2.5 rounded-lg text-sm font-semibold capitalize transition-all duration-200 ${
                    mode === m
                      ? 'bg-gradient-to-r from-violet-600 to-purple-600 text-white shadow-glow-sm'
                      : 'text-slate-500 hover:text-slate-300'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>

            <AnimatePresence mode="wait">
              <motion.form
                key={mode}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                transition={{ duration: 0.2 }}
                onSubmit={submit}
                className="space-y-4"
              >
                {mode === 'register' && (
                  <>
                    <div>
                      <label className="label">Full Name</label>
                      <input name="fullName" value={form.fullName} onChange={handle}
                        className="input-field" placeholder="Pawan Yadav" required />
                    </div>
                    <div>
                      <label className="label">Username</label>
                      <input name="username" value={form.username} onChange={handle}
                        className="input-field" placeholder="pawan" required />
                    </div>
                  </>
                )}

                <div>
                  <label className="label">Email</label>
                  <input name="email" type="email" value={form.email} onChange={handle}
                    className="input-field" placeholder="pawan@example.com" required />
                </div>

                <div>
                  <label className="label">Password</label>
                  <div className="relative">
                    <input name="password" type={showPw ? 'text' : 'password'}
                      value={form.password} onChange={handle}
                      className="input-field pr-12" placeholder="••••••••" required />
                    <button type="button" onClick={() => setShowPw(s => !s)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors">
                      {showPw ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                {error && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    className="flex items-start gap-2 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-sm"
                  >
                    <AlertCircle size={16} className="mt-0.5 shrink-0" />
                    <span>{error}</span>
                  </motion.div>
                )}

                <button type="submit" disabled={loading} className="btn-primary w-full mt-2 flex items-center justify-center gap-2">
                  {loading
                    ? <><Loader2 size={18} className="animate-spin" /> {mode === 'login' ? 'Signing in...' : 'Creating account...'}</>
                    : <>{mode === 'login' ? 'Sign In' : 'Create Account'} <Sparkles size={16} /></>
                  }
                </button>

                <p className="text-center text-sm text-slate-500 mt-4">
                  {mode === 'login' ? "Don't have an account? " : 'Already have an account? '}
                  <button type="button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }}
                    className="text-violet-400 hover:text-violet-300 font-medium transition-colors">
                    {mode === 'login' ? 'Sign up free' : 'Sign in'}
                  </button>
                </p>
              </motion.form>
            </AnimatePresence>
          </div>

          {mode === 'register' && (
            <p className="text-center text-xs text-slate-600 mt-4">
              New accounts receive <span className="text-violet-400 font-semibold">30 free credits</span> to generate images
            </p>
          )}
        </motion.div>
      </div>
    </div>
  );
}
