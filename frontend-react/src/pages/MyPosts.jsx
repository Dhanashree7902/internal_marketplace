import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { api } from '../api/client.js';
import { Badge, Button, EmptyState, formatDate, PageHeader, Panel, SkeletonList } from '../components/ui.jsx';
import { Package, Clock, CheckCircle2, Archive, PlusCircle, ExternalLink, XCircle } from 'lucide-react';

const STATUSES = [
  { key: 'ACTIVE', label: 'Active Listings', icon: CheckCircle2 },
  { key: 'CLOSED', label: 'Closed Items', icon: Clock },
  { key: 'ARCHIVED', label: 'Archived', icon: Archive },
];

export default function MyPosts() {
  const { profile } = useAuth();
  const [postsByStatus, setPostsByStatus] = useState({});
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ACTIVE');

  useEffect(() => {
    if (!profile) return;
    Promise.all(
      STATUSES.map(({ key }) =>
        api.listPosts({ status: key }, 'MyPosts').then((res) => [key, res.data.filter((p) => p.user_id === profile.uid)])
      )
    ).then((entries) => {
      setPostsByStatus(Object.fromEntries(entries));
      setLoading(false);
    });
  }, [profile]);

  async function close(id) {
    await api.updatePost(id, { status: 'CLOSED' });
    setPostsByStatus((prev) => ({
      ...prev,
      ACTIVE: (prev.ACTIVE || []).filter((p) => p.id !== id),
      CLOSED: [...(prev.CLOSED || []), { ...(prev.ACTIVE || []).find((p) => p.id === id), status: 'CLOSED' }],
    }));
  }

  if (loading) return <SkeletonList />;

  const hasAnyPosts = Object.values(postsByStatus).some((list) => list.length > 0);
  const activeItems = postsByStatus[activeTab] || [];

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

      {!hasAnyPosts ? (
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
              const count = (postsByStatus[key] || []).length;
              const isActive = activeTab === key;
              return (
                <button
                  key={key}
                  onClick={() => setActiveTab(key)}
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
                    {count}
                  </span>
                </button>
              );
            })}
          </div>

          {/* List Content */}
          {activeItems.length === 0 ? (
            <EmptyState
              title={`No ${activeTab.toLowerCase()} listings`}
              description="Switch between status tabs above to view your other listings."
            />
          ) : (
            <Panel>
              {activeItems.map((p) => (
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

                    {activeTab === 'ACTIVE' && (
                      <Button variant="danger" size="sm" onClick={() => close(p.id)} icon={XCircle}>
                        Mark Closed
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </Panel>
          )}
        </div>
      )}
    </div>
  );
}
