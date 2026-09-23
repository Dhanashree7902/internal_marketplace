import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { api } from '../api/client.js';
import {
  Badge,
  Button,
  EmptyState,
  ErrorState,
  formatDate,
  PageHeader,
  Pagination,
  Panel,
  SkeletonList,
} from '../components/ui.jsx';
import { Package, Clock, CheckCircle2, Archive, PlusCircle, ExternalLink, XCircle, Trash2, RotateCcw, Pencil } from 'lucide-react';

const STATUSES = [
  { key: 'ACTIVE', label: 'Active Listings', icon: CheckCircle2 },
  { key: 'CLOSED', label: 'Closed Items', icon: Clock },
  { key: 'ARCHIVED', label: 'Archived', icon: Archive },
];

const POSTS_PAGE_SIZE = 10;

const EMPTY_TAB_STATE = { posts: null, page: 1, totalPages: 0, totalElements: null, hasNext: false, hasPrevious: false, error: null };

export default function MyPosts() {
  const { profile } = useAuth();
  const [tabs, setTabs] = useState({
    ACTIVE: { ...EMPTY_TAB_STATE },
    CLOSED: { ...EMPTY_TAB_STATE },
    ARCHIVED: { ...EMPTY_TAB_STATE },
  });
  const [activeTab, setActiveTab] = useState('ACTIVE');
  const [pageLoading, setPageLoading] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [feedback, setFeedback] = useState(null);

  // Each status tab is its own server-side paginated list -- fetching only the
  // requested page's rows (mine=true&status=<tab>&page=<n>) instead of every
  // post the user has ever made, unlike the old client-side-filtered full fetch.
  const fetchTab = useCallback((status, page) => {
    setPageLoading(true);
    return api
      .listPosts({ mine: true, status, page, size: POSTS_PAGE_SIZE }, 'MyPosts')
      .then((res) => {
        // A page that's now past the end (e.g. the last item on the last page
        // was just closed/archived) steps back one page instead of showing a
        // dead "empty" page when there's actually earlier content to show.
        if ((res.data || []).length === 0 && page > 1) {
          return fetchTab(status, page - 1);
        }
        setTabs((prev) => ({
          ...prev,
          [status]: {
            posts: res.data,
            page: res.page,
            totalPages: res.totalPages || 0,
            totalElements: res.totalElements ?? 0,
            hasNext: Boolean(res.hasNext),
            hasPrevious: Boolean(res.hasPrevious),
            error: null,
          },
        }));
      })
      .catch((err) => {
        setTabs((prev) => ({
          ...prev,
          [status]: { ...prev[status], error: err.message || 'Failed to load your listings.' },
        }));
      })
      .finally(() => setPageLoading(false));
  }, []);

  useEffect(() => {
    if (!profile) return;
    STATUSES.forEach(({ key }) => fetchTab(key, 1));
  }, [profile, fetchTab]);

  function switchTab(key) {
    setActiveTab(key);
  }

  function goToPreviousPage() {
    const tab = tabs[activeTab];
    if (tab.hasPrevious) fetchTab(activeTab, Math.max(1, tab.page - 1));
  }

  function goToNextPage() {
    const tab = tabs[activeTab];
    if (tab.hasNext) fetchTab(activeTab, tab.page + 1);
  }

  // Shared by close/archive: both just move a post between two tabs by
  // changing its status, then resync whichever two tabs are affected.
  async function moveToStatus(id, newStatus, affectedTabs, successMessage) {
    try {
      await api.updatePost(id, { status: newStatus });
      affectedTabs.forEach((tab) => fetchTab(tab, tabs[tab].page));
      setFeedback({ type: 'success', message: successMessage });
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || 'Failed to update post.' });
    } finally {
      setTimeout(() => setFeedback(null), 3000);
    }
  }

  function close(id) {
    moveToStatus(id, 'CLOSED', ['ACTIVE', 'CLOSED'], 'Post marked as closed.');
  }

  // Only the owner can reach this at all -- the button only renders on their
  // own My Posts page, and PostService#updatePostAsOwner rejects the PATCH
  // server-side for anyone else.
  function archive(id) {
    if (!window.confirm('Archive this post? It will move out of Active Listings into Archived.')) {
      return;
    }
    moveToStatus(id, 'ARCHIVED', ['ACTIVE', 'ARCHIVED'], 'Post archived successfully.');
  }

  // Restoring makes the post ACTIVE again -- same visibility rules as any
  // other active post, so it reappears on the Home page for every user, not
  // just this page's owner-only view.
  function restore(id) {
    moveToStatus(id, 'ACTIVE', ['ARCHIVED', 'ACTIVE'], 'Post restored to Active Listings.');
  }

  // Only closed or archived posts are deletable (enforced again server-side
  // in PostService#deletePostAsOwner, so a direct API call can't bypass
  // this). Deletable from whichever of those two tabs is currently open.
  async function deletePost(id) {
    if (!window.confirm('Delete this post permanently? This cannot be undone.')) {
      return;
    }
    const tab = activeTab;
    setDeletingId(id);
    try {
      await api.deletePost(id);
      // Remove it from the visible list the instant the delete succeeds --
      // don't wait on the resync below, which only refreshes counts/pagination.
      setTabs((prev) => ({
        ...prev,
        [tab]: { ...prev[tab], posts: prev[tab].posts.filter((p) => p.id !== id) },
      }));
      setFeedback({ type: 'success', message: 'Post deleted successfully.' });
      fetchTab(tab, tabs[tab].page);
    } catch (err) {
      setFeedback({ type: 'error', message: err.message || 'Failed to delete post.' });
    } finally {
      setDeletingId(null);
      setTimeout(() => setFeedback(null), 3000);
    }
  }

  const current = tabs[activeTab];
  const initialLoading = STATUSES.every(({ key }) => tabs[key].posts === null && !tabs[key].error);
  const hasAnyPosts = STATUSES.some(({ key }) => (tabs[key].totalElements || 0) > 0);
  // A failed fetch leaves totalElements at its initial null/0, which looks
  // identical to "genuinely zero posts" to hasAnyPosts above. Without this,
  // three tabs that all failed (e.g. a backend/query error) rendered as "you
  // haven't listed anything yet" instead of the real error -- indistinguishable
  // from actually having no posts, which is the bug this page was reported for.
  const allTabsErrored = STATUSES.every(({ key }) => tabs[key].error);

  if (initialLoading) return <SkeletonList />;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Seller Portal"
        title="My Listings"
        description="Manage your published items, track status, and close completed deals."
        action={
          <Link to="/posts/new">
            <Button variant="primary" icon={PlusCircle}>
              New Listing
            </Button>
          </Link>
        }
      />

      {feedback && (
        <div
          className={`rounded-2xl p-4 text-sm font-semibold flex items-center justify-between animate-in fade-in ${
            feedback.type === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'
          }`}
        >
          <span>{feedback.message}</span>
          <button onClick={() => setFeedback(null)} className="text-xs font-bold underline">
            Dismiss
          </button>
        </div>
      )}

      {allTabsErrored ? (
        <ErrorState
          title="Couldn't load your listings"
          description={tabs.ACTIVE.error || tabs.CLOSED.error || tabs.ARCHIVED.error}
          onRetry={() => STATUSES.forEach(({ key }) => fetchTab(key, tabs[key].page))}
        />
      ) : !hasAnyPosts ? (
        <EmptyState
          title="You haven't listed any items yet"
          description="Create your first marketplace post to start selling or renting to colleagues."
          action={
            <Link to="/posts/new">
              <Button variant="primary" icon={PlusCircle}>
                Create Post
              </Button>
            </Link>
          }
        />
      ) : (
        <div className="space-y-4">
          {/* Status Tabs */}
          <div className="flex border-b border-slate-200 gap-2">
            {STATUSES.map(({ key, label, icon: Icon }) => {
              const count = tabs[key].totalElements;
              const isActive = activeTab === key;
              return (
                <button
                  key={key}
                  onClick={() => switchTab(key)}
                  className={`flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition-all cursor-pointer ${
                    isActive
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-slate-500 hover:text-slate-900'
                  }`}
                >
                  <Icon className={`h-4 w-4 ${isActive ? 'text-indigo-600' : 'text-slate-400'}`} />
                  <span>{label}</span>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                      isActive ? 'bg-indigo-100 text-indigo-700' : 'bg-slate-100 text-slate-600'
                    }`}
                  >
                    {count ?? '—'}
                  </span>
                </button>
              );
            })}
          </div>

          {/* List Content */}
          {current.error ? (
            <ErrorState description={current.error} onRetry={() => fetchTab(activeTab, current.page)} />
          ) : current.posts === null ? (
            <SkeletonList />
          ) : current.posts.length === 0 ? (
            <EmptyState
              title={`No ${activeTab.toLowerCase()} listings`}
              description="Switch between status tabs above to view your other listings."
            />
          ) : (
            <Panel>
              {current.posts.map((p) => (
                <div
                  key={p.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 transition-colors hover:bg-slate-50/80"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <Badge tone={p.post_type}>{p.post_type}</Badge>
                      <Badge tone={p.status}>{p.status}</Badge>
                      {p.price != null && (
                        <span className="text-sm font-extrabold text-slate-900">${p.price}</span>
                      )}
                    </div>
                    <Link to={`/posts/${p.id}`} className="mt-2 block">
                      <h3 className="text-base font-bold text-slate-900 hover:text-indigo-600 transition-colors">
                        {p.title}
                      </h3>
                      <p className="mt-0.5 text-xs text-slate-500 line-clamp-1">{p.description}</p>
                    </Link>
                    {p.updated_at && (
                      <span className="mt-2 block text-[11px] text-slate-400">
                        Updated {formatDate(p.updated_at)}
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                    <Link to={`/posts/${p.id}`}>
                      <Button variant="secondary" size="sm" icon={ExternalLink}>
                        View Detail
                      </Button>
                    </Link>

                    <Link to={`/posts/${p.id}/edit`}>
                      <Button variant="secondary" size="sm" icon={Pencil}>
                        Edit
                      </Button>
                    </Link>

                    {activeTab === 'ARCHIVED' && (
                      <Button variant="secondary" size="sm" onClick={() => restore(p.id)} icon={RotateCcw}>
                        Restore
                      </Button>
                    )}

                    {activeTab === 'ACTIVE' && (
                      <Button variant="secondary" size="sm" onClick={() => archive(p.id)} icon={Archive}>
                        Archive
                      </Button>
                    )}

                    {activeTab === 'ACTIVE' && (
                      <Button variant="danger" size="sm" onClick={() => close(p.id)} icon={XCircle}>
                        Mark Closed
                      </Button>
                    )}

                    {(activeTab === 'CLOSED' || activeTab === 'ARCHIVED') && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => deletePost(p.id)}
                        icon={Trash2}
                        loading={deletingId === p.id}
                      >
                        Delete
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </Panel>
          )}

          {!current.error && (
            <Pagination
              page={current.page}
              totalPages={current.totalPages}
              hasPrevious={current.hasPrevious}
              hasNext={current.hasNext}
              onPrevious={goToPreviousPage}
              onNext={goToNextPage}
              loading={pageLoading}
            />
          )}
        </div>
      )}
    </div>
  );
}
