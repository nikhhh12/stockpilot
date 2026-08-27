import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { LogIn, ShieldCheck, Bot, Sparkles, AlertCircle, ArrowRight, UserCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('admin@stockpilot.com');
  const [password, setPassword] = useState('password123');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setIsLoading(true);

    try {
      await login(email, password);
      navigate('/');
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Invalid credentials or backend unreachable.');
    } finally {
      setIsLoading(false);
    }
  };

  const setDemoCredentials = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('password123');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 p-4 text-slate-100 relative overflow-hidden">
      {/* Ambient background glows */}
      <div className="absolute top-1/4 -left-48 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -right-48 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md bg-slate-900/90 border border-slate-800/80 rounded-3xl p-8 shadow-2xl backdrop-blur-2xl relative z-10 space-y-6">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-500 to-indigo-600 border border-sky-400/30 text-white font-black text-xl shadow-lg shadow-sky-500/20 mb-2">
            SP
          </div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">StockPilot AI</h1>
          <p className="text-xs text-slate-400">Inventory Intelligence &amp; Supply Chain Engine</p>
        </div>

        {errorMessage && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/30 rounded-xl text-rose-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-300 font-medium mb-1.5">Email Address</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          <div>
            <label className="block text-slate-300 font-medium mb-1.5">Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-semibold text-xs shadow-lg shadow-sky-500/20 flex items-center justify-center gap-2 transition-all hover:scale-[1.02] disabled:opacity-50"
          >
            {isLoading ? (
              <span>Authenticating...</span>
            ) : (
              <>
                <LogIn className="w-4 h-4" />
                <span>Sign In to StockPilot</span>
              </>
            )}
          </button>
        </form>

        {/* Quick Demo Accounts */}
        <div className="pt-4 border-t border-slate-800/80 space-y-2.5">
          <p className="text-[11px] text-slate-400 font-medium text-center">Quick Demo Account Fill:</p>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => setDemoCredentials('admin@stockpilot.com')}
              className="p-2 rounded-xl bg-slate-950 hover:bg-slate-800/80 text-[10px] text-sky-300 border border-slate-800 font-medium transition-colors text-center"
            >
              System Admin
            </button>
            <button
              type="button"
              onClick={() => setDemoCredentials('manager@stockpilot.com')}
              className="p-2 rounded-xl bg-slate-950 hover:bg-slate-800/80 text-[10px] text-indigo-300 border border-slate-800 font-medium transition-colors text-center"
            >
              Inv. Manager
            </button>
            <button
              type="button"
              onClick={() => setDemoCredentials('staff@stockpilot.com')}
              className="p-2 rounded-xl bg-slate-950 hover:bg-slate-800/80 text-[10px] text-emerald-300 border border-slate-800 font-medium transition-colors text-center"
            >
              Warehouse Staff
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center text-[11px] text-slate-400">
          Need an account?{' '}
          <Link to="/register" className="text-sky-400 font-semibold hover:underline">
            Register New Organization User
          </Link>
        </div>
      </div>
    </div>
  );
};
