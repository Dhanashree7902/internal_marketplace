import React from 'react';
import {
  Tag,
  Clock,
  Sparkles,
  CheckCircle2,
  XCircle,
  AlertCircle,
  FolderPlus,
  Loader2,
  ChevronRight,
  TrendingUp,
  Layers,
  Inbox,
  X,
} from 'lucide-react';

const VARIANT_CLASSES = {
  primary:
    'bg-gradient-to-r from-indigo-600 to-indigo-700 text-white hover:from-indigo-500 hover:to-indigo-600 focus-visible:outline-indigo-600 shadow-md shadow-indigo-600/20 hover:shadow-lg hover:shadow-indigo-600/30 active:scale-[0.98]',
  secondary:
    'bg-white text-slate-700 border border-slate-200 hover:bg-slate-50 hover:border-slate-300 focus-visible:outline-indigo-600 shadow-sm active:scale-[0.98]',
  danger:
    'bg-white text-rose-600 border border-rose-200 hover:bg-rose-50 hover:border-rose-300 focus-visible:outline-rose-600 active:scale-[0.98]',
  ghost:
    'text-indigo-600 hover:bg-indigo-50/80 hover:text-indigo-700 focus-visible:outline-indigo-600 active:scale-[0.98]',
  accent:
    'bg-gradient-to-r from-violet-600 to-indigo-600 text-white hover:from-violet-500 hover:to-indigo-500 shadow-md shadow-violet-600/20 active:scale-[0.98]',
};

const SIZE_CLASSES = {
  md: 'px-4 py-2 text-sm font-medium',
  sm: 'px-3 py-1.5 text-xs font-medium',
  lg: 'px-5 py-2.5 text-base font-medium',
};

export function Button({ variant = 'primary', size = 'md', className = '', icon: Icon, loading = false, children, disabled, ...props }) {
  return (
    <button
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-xl transition-all duration-200
        focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 cursor-pointer
        disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100 ${SIZE_CLASSES[size]} ${VARIANT_CLASSES[variant]} ${className}`}
      {...props}
    >
      {loading ? (
        <Loader2 className="h-4 w-4 animate-spin shrink-0" />
      ) : Icon ? (
        <Icon className="h-4 w-4 shrink-0" />
      ) : null}
      {children}
    </button>
  );
}

export function Card({ className = '', hover = false, ...props }) {
  return (
    <div
      className={`rounded-2xl border border-slate-200/80 bg-white shadow-sm transition-all duration-200 ${
        hover ? 'hover:border-indigo-200 hover:shadow-md hover:-translate-y-0.5' : ''
      } ${className}`}
      {...props}
    />
  );
}

export function Input({ className = '', icon: Icon, error, ...props }) {
  return (
    <div className="relative w-full">
      {Icon && (
        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
          <Icon className="h-4 w-4" />
        </div>
      )}
      <input
        className={`w-full rounded-xl border bg-white py-2 text-sm text-slate-900 placeholder:text-slate-400
          transition-all duration-200 focus:outline-none focus:ring-4 ${
            Icon ? 'pl-9 pr-3' : 'px-3.5'
          } ${
            error
              ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/10'
              : 'border-slate-200 focus:border-indigo-500 focus:ring-indigo-500/15'
          } ${className}`}
        {...props}
      />
    </div>
  );
}

export function Textarea({ className = '', error, ...props }) {
  return (
    <textarea
      className={`w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400
        transition-all duration-200 focus:outline-none focus:ring-4 ${
          error
            ? 'border-rose-300 focus:border-rose-500 focus:ring-rose-500/10'
            : 'border-slate-200 focus:border-indigo-500 focus:ring-indigo-500/15'
        } ${className}`}
      rows={4}
      {...props}
    />
  );
}

export function Select({ className = '', children, ...props }) {
  return (
    <select
      className={`w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-sm text-slate-900
        transition-all duration-200 focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15 ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}

export function Field({ label, hint, error, children, required }) {
  return (
    <label className="block">
      <div className="mb-1.5 flex items-center justify-between">
        <span className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
          {label} {required && <span className="text-rose-500">*</span>}
        </span>
        {hint && <span className="text-xs text-slate-400">{hint}</span>}
      </div>
      {children}
      {error && <p className="mt-1 text-xs font-medium text-rose-600">{error}</p>}
    </label>
  );
}

const BADGE_CONFIG = {
  ACTIVE: { bg: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', icon: CheckCircle2 },
  CLOSED: { bg: 'bg-slate-100 text-slate-600 ring-slate-500/20', icon: Clock },
  SOLD: { bg: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20', icon: Tag },
  REMOVED: { bg: 'bg-rose-50 text-rose-700 ring-rose-600/20', icon: XCircle },
  REJECTED: { bg: 'bg-rose-50 text-rose-700 ring-rose-600/20', icon: XCircle },
  ARCHIVED: { bg: 'bg-slate-100 text-slate-500 ring-slate-500/20', icon: Layers },
  PENDING: { bg: 'bg-amber-50 text-amber-700 ring-amber-600/20', icon: AlertCircle },
  APPROVED: { bg: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', icon: CheckCircle2 },
  SELL: { bg: 'bg-emerald-100/80 text-emerald-800 ring-emerald-600/20', icon: Tag },
  RENT: { bg: 'bg-sky-100/80 text-sky-800 ring-sky-600/20', icon: Clock },
  DEFAULT: { bg: 'bg-indigo-50 text-indigo-700 ring-indigo-600/20', icon: Sparkles },
};

export function Badge({ children, tone, showIcon = true }) {
  const config = BADGE_CONFIG[tone] ?? BADGE_CONFIG.DEFAULT;
  const Icon = config.icon;
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset ${config.bg}`}>
      {showIcon && Icon && <Icon className="h-3 w-3 shrink-0" />}
      <span>{children}</span>
    </span>
  );
}

export function PageHeader({ eyebrow, title, description, action }) {
  return (
    <div className="mb-8 flex flex-col gap-4 border-b border-slate-200/80 pb-6 sm:flex-row sm:items-center sm:justify-between">
      <div>
        {eyebrow && (
          <div className="mb-1 flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest text-indigo-600">
            <Sparkles className="h-3.5 w-3.5" />
            <span>{eyebrow}</span>
          </div>
        )}
        <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">{title}</h1>
        {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export function SectionHeading({ eyebrow, title, description, action }) {
  return (
    <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow && (
          <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">{eyebrow}</p>
        )}
        {title && <h2 className="text-base font-bold text-slate-900">{title}</h2>}
        {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export function Panel({ title, description, action, children, className = '' }) {
  return (
    <Card className={`overflow-hidden ${className}`}>
      {(title || action) && (
        <div className="flex flex-col gap-2 border-b border-slate-100 bg-slate-50/50 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            {title && <h3 className="text-sm font-bold text-slate-900">{title}</h3>}
            {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
          </div>
          {action}
        </div>
      )}
      <div className="divide-y divide-slate-100">{children}</div>
    </Card>
  );
}

export function StatTile({ label, value, icon: Icon = TrendingUp, trend }) {
  return (
    <Card className="p-5 relative overflow-hidden">
      <div className="flex items-center justify-between">
        <p className="text-xs font-bold uppercase tracking-wider text-slate-400">{label}</p>
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-50 text-indigo-600">
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <p className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900">{value}</p>
      {trend && <p className="mt-1 text-xs font-medium text-emerald-600">{trend}</p>}
    </Card>
  );
}

export function formatDate(value) {
  if (!value) return '';
  let date;
  if (typeof value === 'object' && typeof value._seconds === 'number') {
    date = new Date(value._seconds * 1000);
  } else {
    date = new Date(value);
  }
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

export function EmptyState({ title, description, action, icon: Icon = Inbox }) {
  return (
    <Card className="flex flex-col items-center justify-center px-6 py-16 text-center">
      <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 ring-8 ring-indigo-50/50">
        <Icon className="h-7 w-7" />
      </div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {description && <p className="mt-1.5 max-w-sm text-sm text-slate-500">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </Card>
  );
}

export function Spinner() {
  return (
    <div className="flex justify-center py-16">
      <div className="flex flex-col items-center gap-3">
        <Loader2 className="h-8 w-8 animate-spin text-indigo-600" />
        <span className="text-xs font-medium text-slate-400">Loading marketplace...</span>
      </div>
    </div>
  );
}

export function SkeletonCard() {
  return (
    <div className="animate-pulse rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <div className="h-4 w-20 rounded bg-slate-200" />
        <div className="h-5 w-14 rounded-full bg-slate-200" />
      </div>
      <div className="mt-3 h-5 w-3/4 rounded bg-slate-200" />
      <div className="mt-2 h-4 w-full rounded bg-slate-200" />
      <div className="mt-2 h-4 w-2/3 rounded bg-slate-200" />
      <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-3">
        <div className="flex items-center gap-2">
          <div className="h-7 w-7 rounded-full bg-slate-200" />
          <div className="h-3 w-16 rounded bg-slate-200" />
        </div>
        <div className="h-5 w-12 rounded bg-slate-200" />
      </div>
    </div>
  );
}

export function SkeletonList() {
  return (
    <div className="divide-y divide-slate-100 rounded-2xl border border-slate-200/80 bg-white shadow-sm overflow-hidden">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="flex animate-pulse items-center justify-between p-4">
          <div className="space-y-2">
            <div className="h-4 w-48 rounded bg-slate-200" />
            <div className="h-3 w-64 rounded bg-slate-200" />
          </div>
          <div className="h-6 w-16 rounded-full bg-slate-200" />
        </div>
      ))}
    </div>
  );
}

export function Modal({ isOpen, onClose, title, children }) {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in">
      <div className="w-full max-w-lg overflow-hidden rounded-2xl bg-white shadow-2xl border border-slate-200 animate-in zoom-in-95">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h3 className="text-base font-bold text-slate-900">{title}</h3>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}
