import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { Badge, Button, EmptyState, formatDate, PageHeader, Panel, SkeletonCard } from '../components/ui.jsx';
import { SearchBar } from '../components/SearchBar.jsx';
import { ChevronRight, Grid, List, Tag, ArrowLeft, Folder, PlusCircle } from 'lucide-react';

export default function CategoryPage() {
  const { categoryId } = useParams();
  const [category, setCategory] = useState(null);
  const [posts, setPosts] = useState(null);
  const [nextCursor, setNextCursor] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [query, setQuery] = useState('');
  const [viewMode, setViewMode] = useState('grid');

  useEffect(() => {
    api.listCategories().then((res) => setCategory(res.data.find((c) => c.id === categoryId) || null));
  }, [categoryId]);

  useEffect(() => {
    setPosts(null);
    const timer = setTimeout(() => {
      api.listPosts({ categoryId, q: query.trim() }).then((res) => {
        setPosts(res.data);
        setNextCursor(res.nextCursor);
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [categoryId, query]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      const res = await api.listPosts({ categoryId, q: query.trim(), cursor: nextCursor });
      setPosts((prev) => [...prev, ...res.data]);
      setNextCursor(res.nextCursor);
    } finally {
      setLoadingMore(false);
    }
  }

  return (
    <div className="space-y-6">
      {/* Breadcrumb Navigation */}
      <nav className="flex items-center gap-2 text-xs font-medium text-slate-400">
        <Link to="/" className="hover:text-indigo-600 transition-colors">
          Home
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-slate-600">Categories</span>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="font-semibold text-slate-900">{category?.name || 'Category'}</span>
      </nav>

      <PageHeader
        eyebrow="Category Browse"
        title={category?.name || 'Category'}
        description={category?.description || 'Active posts and listings inside this category.'}
        action={
          <Link to="/posts/new">
            <Button variant="primary" icon={PlusCircle}>
              Post in this Category
            </Button>
          </Link>
        }
      />

      {/* Filter and View Mode Switcher */}
      <div className="flex flex-col gap-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
        <div className="flex-1 sm:max-w-md">
          <SearchBar value={query} onChange={setQuery} placeholder={`Search within ${category?.name || 'category'}...`} />
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <div className="flex rounded-xl bg-slate-100 p-1 text-slate-600">
            <button
              onClick={() => setViewMode('grid')}
              className={`rounded-lg p-1.5 transition-all cursor-pointer ${
                viewMode === 'grid' ? 'bg-white text-indigo-600 shadow-xs' : 'hover:text-slate-900'
              }`}
              title="Grid view"
            >
              <Grid className="h-4 w-4" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`rounded-lg p-1.5 transition-all cursor-pointer ${
                viewMode === 'list' ? 'bg-white text-indigo-600 shadow-xs' : 'hover:text-slate-900'
              }`}
              title="List view"
            >
              <List className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {posts === null ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : posts.length === 0 ? (
        <EmptyState
          title={query ? 'No matching posts found' : 'No listings in this category yet'}
          description={
            query
              ? `No posts matched "${query}".`
              : 'Be the first person to post an item or offer in this category!'
          }
          action={
            <Link to="/posts/new">
              <Button variant="primary" icon={PlusCircle}>
                Create Post
              </Button>
            </Link>
          }
        />
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
          {posts.map((p) => (
            <Link
              key={p.id}
              to={`/posts/${p.id}`}
              className="group flex flex-col justify-between overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-200 hover:-translate-y-1 hover:border-indigo-200 hover:shadow-xl"
            >
              <div>
                <div className="flex items-center justify-between gap-2">
                  <Badge tone={p.post_type}>{p.post_type}</Badge>
                  <Badge tone={p.status}>{p.status}</Badge>
                </div>
                <h3 className="mt-4 text-base font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2">
                  {p.title}
                </h3>
                <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-slate-500">
                  {p.description}
                </p>
              </div>

              <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-3">
                <span className="text-xs text-slate-400">
                  {p.created_at ? formatDate(p.created_at) : ''}
                </span>
                {p.price != null ? (
                  <span className="text-lg font-extrabold text-slate-900">${p.price.toFixed(2)}</span>
                ) : (
                  <span className="text-xs font-semibold text-slate-400">Free / Negotiable</span>
                )}
              </div>
            </Link>
          ))}
        </div>
      ) : (
        <Panel>
          {posts.map((p) => (
            <Link
              key={p.id}
              to={`/posts/${p.id}`}
              className="flex items-center justify-between gap-4 px-6 py-4 transition-colors hover:bg-slate-50/80"
            >
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <Badge tone={p.post_type}>{p.post_type}</Badge>
                  <span className="text-xs text-slate-400">
                    {p.created_at ? formatDate(p.created_at) : ''}
                  </span>
                </div>
                <h3 className="mt-1 text-sm font-bold text-slate-900 truncate hover:text-indigo-600">
                  {p.title}
                </h3>
                <p className="mt-0.5 text-xs text-slate-500 line-clamp-1">{p.description}</p>
              </div>

              <div className="flex shrink-0 items-center gap-4">
                {p.price != null && (
                  <span className="text-base font-extrabold text-slate-900">${p.price}</span>
                )}
                <Badge tone={p.status}>{p.status}</Badge>
                <ChevronRight className="h-4 w-4 text-slate-400" />
              </div>
            </Link>
          ))}
        </Panel>
      )}

      {nextCursor && (
        <div className="mt-8 flex justify-center">
          <Button variant="secondary" onClick={loadMore} loading={loadingMore}>
            Load More
          </Button>
        </div>
      )}
    </div>
  );
}
