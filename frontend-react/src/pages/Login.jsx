import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { signInWithGoogle } from '../firebase.js';
import { useState } from 'react';
import { Store, ShieldCheck, Zap, Tag, Lock, ArrowRight } from 'lucide-react';

export default function Login() {
  const { firebaseUser } = useAuth();
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  if (firebaseUser) return <Navigate to="/" replace />;

  async function handleSignIn() {
    setError(null);
    setLoading(true);
    try {
      await signInWithGoogle();
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }

  return (
    <div className="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-slate-900 px-4 py-12">
      {/* Soft Background Mesh / Glow */}
      <div className="absolute -left-32 -top-32 h-96 w-96 rounded-full bg-indigo-600/20 blur-3xl" />
      <div className="absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-violet-600/20 blur-3xl" />

      <div className="relative z-10 w-full max-w-md overflow-hidden rounded-3xl border border-white/10 bg-white/95 p-8 shadow-2xl backdrop-blur-xl sm:p-10">
        <div className="flex flex-col items-center text-center">
          {/* Logo Emblem */}
          <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-violet-600 text-white shadow-lg shadow-indigo-500/30">
            <Store className="h-7 w-7" />
          </div>

          <h1 className="text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl">
            Internal Marketplace
          </h1>
          <p className="mt-2 text-sm text-slate-500 leading-relaxed">
            The private corporate platform for buying, selling, and renting items with your teammates.
          </p>
        </div>

        {/* Feature Points */}
        <div className="my-8 space-y-3 rounded-2xl bg-slate-50 p-4 border border-slate-100 text-xs font-semibold text-slate-600">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>Verified company employees only</span>
          </div>
          <div className="flex items-center gap-2.5">
            <Zap className="h-4 w-4 text-indigo-600 shrink-0" />
            <span>Direct peer-to-peer team exchange</span>
          </div>
          <div className="flex items-center gap-2.5">
            <Tag className="h-4 w-4 text-violet-600 shrink-0" />
            <span>Sell or rent office gear & tech equipment</span>
          </div>
        </div>

        {/* Sign In Button */}
        <button
          onClick={handleSignIn}
          disabled={loading}
          className="flex w-full items-center justify-center gap-3 rounded-2xl border border-slate-300/80 bg-white
            px-5 py-3.5 text-sm font-bold text-slate-700 shadow-md transition-all duration-200 hover:bg-slate-50
            hover:border-slate-400 active:scale-[0.98] disabled:opacity-60 cursor-pointer"
        >
          <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
            <path
              fill="#4285F4"
              d="M23.52 12.27c0-.85-.08-1.66-.22-2.45H12v4.63h6.47c-.28 1.5-1.13 2.77-2.4 3.62v3.01h3.88c2.27-2.09 3.57-5.17 3.57-8.81z"
            />
            <path
              fill="#34A853"
              d="M12 24c3.24 0 5.96-1.07 7.95-2.92l-3.88-3.01c-1.08.72-2.45 1.15-4.07 1.15-3.13 0-5.78-2.11-6.73-4.95H1.27v3.11C3.25 21.3 7.31 24 12 24z"
            />
            <path fill="#FBBC05" d="M5.27 14.27a7.2 7.2 0 010-4.54V6.62H1.27a12 12 0 000 10.76l4-3.11z" />
            <path
              fill="#EA4335"
              d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.44-3.44C17.95 1.19 15.24 0 12 0 7.31 0 3.25 2.7 1.27 6.62l4 3.11C6.22 6.86 8.87 4.75 12 4.75z"
            />
          </svg>
          <span>{loading ? 'Signing in…' : 'Sign in with Google Workspace'}</span>
        </button>

        {error && (
          <p className="mt-4 rounded-xl bg-rose-50 px-4 py-2.5 text-center text-xs font-semibold text-rose-700" role="alert">
            {error}
          </p>
        )}
      </div>

      <p className="mt-8 text-center text-xs text-slate-400">
        Internal Marketplace · Company Security Policy Applies
      </p>
    </div>
  );
}
