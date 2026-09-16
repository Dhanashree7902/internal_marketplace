import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { Badge, Button, Card, EmptyState, formatDate, PageHeader, Panel, SectionHeading, SkeletonCard } from '../components/ui.jsx';
import { SearchBar } from '../components/SearchBar.jsx';
import {
  Sparkles,
  PlusCircle,
  Grid,
  List,
  Tag,
  Clock,
  User,
  ChevronRight,
  TrendingUp,
  SlidersHorizontal,
  Layers,
  Laptop,
  Briefcase,
  Smartphone,
  BookOpen,
  Armchair,
  Box,
} from 'lucide-react';

const CATEGORY_ICONS = [Laptop, Smartphone, Armchair, Briefcase, BookOpen, Box];

function getCategoryIcon(index) {
  return CATEGORY_ICONS[index % CATEGORY_ICONS.length] || Box;
}

const CATEGORY_GRADIENTS = [
  'from-indigo-500 to-indigo-600',
  'from-violet-500 to-purple-600',
  'from-sky-500 to-blue-600',
  'from-emerald-500 to-teal-600',
  'from-amber-500 to-orange-600',
  'from-rose-500 to-pink-600',
];

function gradientFor(id) {
  let hash = 0;
  for (let i = 0; i < id.length; i++) hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
  return CATEGORY_GRADIENTS[hash % CATEGORY_GRADIENTS.length];
}

export default function Home() {
  const [categories, setCategories] = useState(null);
  const [posts, setPosts] = useState(null);
  const [nextCursor, setNextCursor] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [query, setQuery] = useState('');
  const [postTypeFilter, setPostTypeFilter] = useState('ALL');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [viewMode, setViewMode] = useState('grid'); // 'grid' | 'list'

  useEffect(() => {
    api.listCategories().then((res) => setCategories(res.data));
  }, []);

  useEffect(() => {
    setPosts(null);
    const timer = setTimeout(() => {
      const params = { q: query.trim() };
      if (postTypeFilter !== 'ALL') params.postType = postTypeFilter;
      if (selectedCategory) params.categoryId = selectedCategory;
      api.listPosts(params).then((res) => {
        setPosts(res.data);
        setNextCursor(res.nextCursor);
      });
    }, 300);
    return () => clearTimeout(timer);
  }, [query, postTypeFilter, selectedCategory]);

  async function loadMore() {
    setLoadingMore(true);
    try {
      const params = { q: query.trim(), cursor: nextCursor };
      if (postTypeFilter !== 'ALL') params.postType = postTypeFilter;
      if (selectedCategory) params.categoryId = selectedCategory;
      const res = await api.listPosts(params);
      setPosts((prev) => [...prev, ...res.data]);
      setNextCursor(res.nextCursor);
    } finally {
      setLoadingMore(false);
    }
  }

  const categoryName = (id) => categories?.find((c) => c.id === id)?.name;

  return (
    <div className="space-y-8">
      {/* Hero Banner */}
      {!query && !selectedCategory && postTypeFilter === 'ALL' && (
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 p-8 text-white shadow-xl shadow-indigo-900/20 sm:p-10">
          <div className="absolute -right-12 -top-12 h-64 w-64 rounded-full bg-indigo-500/20 blur-3xl" />
          <div className="absolute -bottom-12 right-1/4 h-64 w-64 rounded-full bg-violet-500/20 blur-3xl" />
          
          <div className="relative z-10 max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-3.5 py-1 text-xs font-semibold text-indigo-200 backdrop-blur-md border border-white/10">
              <Sparkles className="h-3.5 w-3.5 text-amber-400" />
              <span>Official Employee Marketplace</span>
            </div>
            <h1 className="mt-4 text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
              Buy, sell, rent & share with your team.
            </h1>
            <p className="mt-3 text-base text-indigo-200 leading-relaxed">
              Find equipment, office items, tech gear, or rent resources directly from verified colleagues across your organization.
            </p>

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <Link to="/posts/new">
                <Button variant="accent" size="lg" icon={PlusCircle}>
                  Create New Post
                </Button>
              </Link>
              <a
                href="#browse-categories"
                className="inline-flex items-center gap-2 rounded-xl bg-white/10 px-4 py-2.5 text-sm font-medium text-white hover:bg-white/20 transition-all backdrop-blur-sm"
              >
                <span>Browse Categories</span>
                <ChevronRight className="h-4 w-4" />
              </a>
            </div>
          </div>
        </div>
      )}

      {/* Categories Grid */}
      <section id="browse-categories">
        <SectionHeading
          title="Categories"
          description="Explore equipment and items by category"
          action={
            selectedCategory ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedCategory('')}
                className="text-xs"
              >
                Show All Categories
              </Button>
            ) : null
          }
        />

        {categories === null ? (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-24 animate-pulse rounded-2xl bg-slate-200" />
            ))}
          </div>
        ) : categories.length === 0 ? (
          <EmptyState title="No active categories yet" description="Request a new category to get started." />
        ) : (
          <div className="grid grid-cols-2 gap-3.5 sm:grid-cols-3 md:grid-cols-6">
            {categories.map((c, idx) => {
              const Icon = getCategoryIcon(idx);
              const isSelected = selectedCategory === c.id;
              return (
                <button
                  key={c.id}
                  onClick={() => setSelectedCategory(isSelected ? '' : c.id)}
                  className={`group text-left relative overflow-hidden rounded-2xl p-4 transition-all duration-200 cursor-pointer ${
                    isSelected
                      ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30 scale-[1.02]'
                      : 'bg-white border border-slate-200/80 hover:border-indigo-300 hover:shadow-md hover:-translate-y-0.5'
                  }`}
                >
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-xl transition-colors ${
                      isSelected
                        ? 'bg-white/20 text-white'
                        : `bg-gradient-to-tr ${gradientFor(c.id)} text-white shadow-sm`
                    }`}
                  >
                    <Icon className="h-5 w-5" />
                  </div>
                  <h3
                    className={`mt-3 text-sm font-bold truncate ${
                      isSelected ? 'text-white' : 'text-slate-900 group-hover:text-indigo-600'
                    }`}
                  >
                    {c.name}
                  </h3>
                  {c.description && (
                    <p
                      className={`mt-0.5 line-clamp-1 text-xs ${
                        isSelected ? 'text-indigo-100' : 'text-slate-500'
                      }`}
                    >
                      {c.description}
                    </p>
                  )}
                </button>
              );
            })}
          </div>
        )}
      </section>

      {/* Main Posts Feed Section */}
      <section className="space-y-4">
        <div className="flex flex-col gap-4 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm sm:flex-row sm:items-center sm:justify-between">
          <div className="flex-1 sm:max-w-md">
            <SearchBar value={query} onChange={setQuery} placeholder="Search posts by title or description..." />
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Post Type Segmented Control */}
            <div className="flex rounded-xl bg-slate-100 p-1 text-xs font-semibold">
              {['ALL', 'SELL', 'RENT'].map((type) => (
                <button
                  key={type}
                  onClick={() => setPostTypeFilter(type)}
                  className={`rounded-lg px-3 py-1.5 transition-all cursor-pointer ${
                    postTypeFilter === type
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {type === 'ALL' ? 'All Types' : type}
                </button>
              ))}
            </div>

            {/* Grid / List View Switcher */}
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

        {/* Posts Content */}
        {posts === null ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        ) : posts.length === 0 ? (
          <EmptyState
            title={query ? 'No posts found' : 'No posts available yet'}
            description={
              query
                ? `No posts matched "${query}". Try searching with another keyword.`
                : 'Be the pioneer! Click the button below to create the first listing.'
            }
            action={
              <Link to="/posts/new">
                <Button variant="primary" icon={PlusCircle}>
                  Create New Listing
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
                className="group relative flex flex-col overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-200 hover:-translate-y-1 hover:border-indigo-200 hover:shadow-xl"
              >
                <div className="flex items-start justify-between gap-2">
                  <Badge tone={p.post_type}>{p.post_type}</Badge>
                  <Badge tone={p.status}>{p.status}</Badge>
                </div>

                <div className="mt-4 flex-1">
                  <h3 className="text-base font-bold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2">
                    {p.title}
                  </h3>
                  <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-slate-500">
                    {p.description}
                  </p>
                </div>

                <div className="mt-3 flex items-center gap-1.5 text-xs font-medium text-slate-500">
                  <User className="h-3.5 w-3.5 text-slate-400" />
                  <span className="truncate">{p.user_name || p.user_email || 'Unknown user'}</span>
                </div>

                <div className="mt-3 flex items-center justify-between border-t border-slate-100 pt-3">
                  <div>
                    <span className="block text-[10px] uppercase font-bold tracking-wider text-slate-400">
                      {categoryName(p.category_id) || 'General'}
                    </span>
                    <span className="text-xs text-slate-400">
                      {p.created_at ? formatDate(p.created_at) : ''}
                    </span>
                  </div>
                  {p.price != null ? (
                    <span className="text-lg font-extrabold text-slate-900">
                      ${p.price.toFixed(2)}
                    </span>
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
                    <span className="text-xs font-semibold text-slate-400">
                      {categoryName(p.category_id)}
                    </span>
                  </div>
                  <h3 className="mt-1 text-sm font-bold text-slate-900 truncate hover:text-indigo-600">
                    {p.title}
                  </h3>
                  <p className="mt-0.5 text-xs text-slate-500 line-clamp-1">{p.description}</p>
                  <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-400">
                    <User className="h-3 w-3" />
                    <span className="truncate">{p.user_name || p.user_email || 'Unknown user'}</span>
                  </div>
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
              Load More Posts
            </Button>
          </div>
        )}
      </section>
    </div>
  );
}
