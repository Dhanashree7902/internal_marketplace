import { useState, useEffect } from 'react';
import { Navigate, Route, Routes, Link, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import { signOutUser } from './firebase.js';
import { api } from './api/client.js';
import { Button, Spinner } from './components/ui.jsx';
import {
  Home as HomeIcon,
  Package,
  PlusCircle,
  FolderPlus,
  Bell,
  ShieldCheck,
  LogOut,
  Menu,
  X,
  Store,
  User,
  Sparkles,
} from 'lucide-react';

import Login from './pages/Login.jsx';
import Home from './pages/Home.jsx';
import CategoryPage from './pages/CategoryPage.jsx';
import PostDetail from './pages/PostDetail.jsx';
import CreateEditPost from './pages/CreateEditPost.jsx';
import MyPosts from './pages/MyPosts.jsx';
import RequestCategory from './pages/RequestCategory.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import Notifications from './pages/Notifications.jsx';

function RequireAuth({ children }) {
  const { firebaseUser, loading } = useAuth();
  if (loading) return <FullPageSpinner />;
  if (!firebaseUser) return <Navigate to="/login" replace />;
  return children;
}

function RequireAdmin({ children }) {
  const { isAdmin, loading } = useAuth();
  if (loading) return <FullPageSpinner />;
  if (!isAdmin) return <Navigate to="/" replace />;
  return children;
}

function FullPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="flex flex-col items-center gap-3">
        <div className="h-10 w-10 animate-spin rounded-full border-3 border-indigo-200 border-t-indigo-600" />
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Loading Marketplace</span>
      </div>
    </div>
  );
}

const NAV_ITEMS = [
  { to: '/', label: 'Home', icon: HomeIcon },
  { to: '/my-posts', label: 'My Posts', icon: Package },
  { to: '/request-category', label: 'Request Category', icon: FolderPlus },
  { to: '/notifications', label: 'Notifications', icon: Bell, badge: true },
];

function NavLink({ to, children, icon: Icon, unreadCount }) {
  const location = useLocation();
  const isActive = location.pathname === to;
  return (
    <Link
      to={to}
      className={`relative flex items-center gap-2 rounded-xl px-3.5 py-2 text-sm font-medium transition-all duration-200 ${
        isActive
          ? 'bg-indigo-50 text-indigo-700 shadow-xs'
          : 'text-slate-600 hover:bg-slate-100/80 hover:text-slate-900'
      }`}
    >
      {Icon && <Icon className={`h-4 w-4 ${isActive ? 'text-indigo-600' : 'text-slate-400'}`} />}
      <span>{children}</span>
      {unreadCount > 0 && (
        <span className="flex h-5 min-w-[20px] items-center justify-center rounded-full bg-indigo-600 px-1 text-[11px] font-bold text-white">
          {unreadCount > 9 ? '9+' : unreadCount}
        </span>
      )}
    </Link>
  );
}

function MobileNavLink({ to, children, icon: Icon, onClick, unreadCount }) {
  const location = useLocation();
  const isActive = location.pathname === to;
  return (
    <Link
      to={to}
      onClick={onClick}
      className={`flex items-center justify-between rounded-xl px-4 py-2.5 text-sm font-medium transition-colors ${
        isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
      }`}
    >
      <div className="flex items-center gap-3">
        {Icon && <Icon className="h-4 w-4" />}
        <span>{children}</span>
      </div>
      {unreadCount > 0 && (
        <span className="rounded-full bg-indigo-600 px-2 py-0.5 text-xs font-bold text-white">
          {unreadCount}
        </span>
      )}
    </Link>
  );
}

function initialsOf(profile) {
  const source = profile?.name || profile?.email || '';
  const parts = source.trim().split(/\s+/);
  if (parts.length === 0 || !parts[0]) return '?';
  return parts.length === 1 ? parts[0][0].toUpperCase() : (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function AccountCluster({ profile, isAdmin }) {
  return (
    <div className="flex items-center gap-3 rounded-xl p-1">
      <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-slate-900 to-slate-700 text-xs font-bold text-white shadow-sm ring-2 ring-white">
        {initialsOf(profile)}
      </div>
      <div className="hidden leading-tight lg:block">
        <p className="truncate text-sm font-semibold text-slate-800">{profile?.name || profile?.email || 'Account'}</p>
        <div className="flex items-center gap-1.5 mt-0.5">
          {isAdmin ? (
            <span className="inline-flex items-center gap-1 rounded-full bg-indigo-50 px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-indigo-700 ring-1 ring-inset ring-indigo-600/20">
              <ShieldCheck className="h-3 w-3" /> Admin
            </span>
          ) : (
            <span className="text-[11px] text-slate-400">Employee</span>
          )}
        </div>
      </div>
    </div>
  );
}

function Layout({ children }) {
  const { firebaseUser, profile, isAdmin } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!firebaseUser) return;
    api
      .listNotifications()
      .then((res) => {
        const unread = (res.data || []).filter((n) => !n.read_at).length;
        setUnreadCount(unread);
      })
      .catch(() => {});
  }, [firebaseUser]);

  return (
    <div className="flex min-h-screen flex-col bg-slate-50/60 font-sans">
      {firebaseUser && (
        <header className="sticky top-0 z-40 border-b border-slate-200/80 bg-white/90 backdrop-blur-md">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
            <Link to="/" className="flex items-center gap-3 group">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-violet-600 text-white shadow-md shadow-indigo-500/25 transition-transform duration-200 group-hover:scale-105">
                <Store className="h-5 w-5" />
              </div>
              <div className="leading-tight">
                <span className="block font-extrabold text-slate-900 tracking-tight text-base group-hover:text-indigo-600 transition-colors">
                  Internal Marketplace
                </span>
                <span className="block text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  Company Portal
                </span>
              </div>
            </Link>

            <nav className="hidden items-center gap-1.5 md:flex">
              {NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  icon={item.icon}
                  unreadCount={item.badge ? unreadCount : 0}
                >
                  {item.label}
                </NavLink>
              ))}
              {isAdmin && (
                <NavLink to="/admin" icon={ShieldCheck}>
                  Admin
                </NavLink>
              )}
            </nav>

            <div className="hidden items-center gap-3 md:flex">
              <Link to="/posts/new">
                <Button variant="primary" size="sm" icon={PlusCircle}>
                  Create Post
                </Button>
              </Link>
              <div className="h-6 w-px bg-slate-200" />
              <AccountCluster profile={profile} isAdmin={isAdmin} />
              <Button
                variant="secondary"
                size="sm"
                onClick={signOutUser}
                className="text-slate-600 hover:text-rose-600"
                title="Sign out"
              >
                <LogOut className="h-4 w-4" />
              </Button>
            </div>

            <button
              type="button"
              aria-label="Toggle menu"
              className="rounded-xl p-2 text-slate-600 hover:bg-slate-100 md:hidden"
              onClick={() => setMenuOpen((v) => !v)}
            >
              {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>

          {menuOpen && (
            <nav className="flex flex-col gap-1 border-t border-slate-200/80 px-4 py-4 md:hidden bg-white animate-in slide-in-from-top-2">
              <div className="mb-2">
                <Link to="/posts/new" onClick={() => setMenuOpen(false)}>
                  <Button variant="primary" className="w-full" icon={PlusCircle}>
                    Create New Post
                  </Button>
                </Link>
              </div>

              {NAV_ITEMS.map((item) => (
                <MobileNavLink
                  key={item.to}
                  to={item.to}
                  icon={item.icon}
                  unreadCount={item.badge ? unreadCount : 0}
                  onClick={() => setMenuOpen(false)}
                >
                  {item.label}
                </MobileNavLink>
              ))}
              {isAdmin && (
                <MobileNavLink to="/admin" icon={ShieldCheck} onClick={() => setMenuOpen(false)}>
                  Admin Dashboard
                </MobileNavLink>
              )}
              <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3">
                <AccountCluster profile={profile} isAdmin={isAdmin} />
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => {
                    setMenuOpen(false);
                    signOutUser();
                  }}
                  icon={LogOut}
                >
                  Sign Out
                </Button>
              </div>
            </nav>
          )}
        </header>
      )}

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">{children}</main>

      {firebaseUser && (
        <footer className="border-t border-slate-200/80 bg-white px-4 py-6 text-center text-xs text-slate-400 sm:px-6">
          <div className="mx-auto max-w-6xl flex flex-col sm:flex-row items-center justify-between gap-3">
            <div className="flex items-center gap-2 font-medium text-slate-500">
              <Store className="h-4 w-4 text-indigo-600" />
              <span>Internal Marketplace</span>
              <span>·</span>
              <span>Company Systems</span>
            </div>
            <p>© {new Date().getFullYear()} Internal Corporate Portal. All rights reserved.</p>
          </div>
        </footer>
      )}
    </div>
  );
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <Home />
            </RequireAuth>
          }
        />
        <Route
          path="/categories/:categoryId"
          element={
            <RequireAuth>
              <CategoryPage />
            </RequireAuth>
          }
        />
        <Route
          path="/posts/new"
          element={
            <RequireAuth>
              <CreateEditPost />
            </RequireAuth>
          }
        />
        <Route
          path="/posts/:postId"
          element={
            <RequireAuth>
              <PostDetail />
            </RequireAuth>
          }
        />
        <Route
          path="/my-posts"
          element={
            <RequireAuth>
              <MyPosts />
            </RequireAuth>
          }
        />
        <Route
          path="/request-category"
          element={
            <RequireAuth>
              <RequestCategory />
            </RequireAuth>
          }
        />
        <Route
          path="/notifications"
          element={
            <RequireAuth>
              <Notifications />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuth>
              <RequireAdmin>
                <AdminDashboard />
              </RequireAdmin>
            </RequireAuth>
          }
        />
      </Routes>
    </Layout>
  );
}
