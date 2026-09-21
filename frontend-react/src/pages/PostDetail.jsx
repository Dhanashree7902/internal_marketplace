import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import { useCategories } from '../context/CategoriesContext.jsx';
import { Badge, Button, Card, formatDate, Panel, Textarea, Spinner, Modal, Field, Input } from '../components/ui.jsx';
import {
  ArrowLeft,
  ChevronRight,
  MessageSquare,
  Flag,
  Calendar,
  User,
  Tag,
  DollarSign,
  Share2,
  Check,
  AlertTriangle,
} from 'lucide-react';

export default function PostDetail() {
  const { postId } = useParams();
  const navigate = useNavigate();
  const { categories } = useCategories();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentText, setCommentText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reportModalOpen, setReportModalOpen] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reporting, setReporting] = useState(false);
  const [copied, setCopied] = useState(false);
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    api.getPost(postId).then((res) => setPost(res.data));
    api.listComments(postId).then((res) => setComments(res.data));
  }, [postId]);

  const category = categories?.find((c) => c.id === post?.category_id);

  async function submitComment(e) {
    e.preventDefault();
    if (!commentText.trim()) return;
    setSubmitting(true);
    try {
      await api.createComment(postId, { text: commentText });
      setCommentText('');
      const updated = await api.listComments(postId);
      setComments(updated.data);
      setFeedback({ type: 'success', message: 'Comment posted successfully.' });
      setTimeout(() => setFeedback(null), 3000);
    } finally {
      setSubmitting(false);
    }
  }

  async function submitReport(e) {
    e.preventDefault();
    if (!reportReason.trim()) return;
    setReporting(true);
    try {
      await api.createReport({ targetType: 'post', targetId: postId, reason: reportReason });
      setReportModalOpen(false);
      setReportReason('');
      setFeedback({ type: 'success', message: 'Post report submitted to administrators.' });
      setTimeout(() => setFeedback(null), 4000);
    } finally {
      setReporting(false);
    }
  }

  function handleShare() {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  if (!post) return <Spinner />;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      {/* Navigation & Breadcrumb */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-indigo-600 transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to listings</span>
        </button>

        <nav className="hidden sm:flex items-center gap-1.5 text-xs font-medium text-slate-400">
          <Link to="/" className="hover:text-indigo-600 transition-colors">
            Home
          </Link>
          <ChevronRight className="h-3 w-3" />
          {category ? (
            <Link to={`/categories/${category.id}`} className="hover:text-indigo-600 transition-colors">
              {category.name}
            </Link>
          ) : (
            <span>Post</span>
          )}
        </nav>
      </div>

      {feedback && (
        <div
          className={`flex items-center justify-between rounded-xl px-4 py-3 text-sm font-medium animate-in fade-in ${
            feedback.type === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'
          }`}
        >
          <span>{feedback.message}</span>
          <button onClick={() => setFeedback(null)} className="text-xs font-bold underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Main Post Card */}
      <Card className="overflow-hidden border-slate-200/80 shadow-md">
        <div className="border-b border-slate-100 bg-slate-50/50 p-6 sm:p-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Badge tone={post.post_type}>{post.post_type}</Badge>
              <Badge tone={post.status}>{post.status}</Badge>
              {category && (
                <span className="text-xs font-semibold text-indigo-600 bg-indigo-50 px-2.5 py-0.5 rounded-full">
                  {category.name}
                </span>
              )}
            </div>

            <button
              onClick={handleShare}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 transition-colors"
            >
              {copied ? <Check className="h-3.5 w-3.5 text-emerald-600" /> : <Share2 className="h-3.5 w-3.5" />}
              <span>{copied ? 'Link Copied!' : 'Share Listing'}</span>
            </button>
          </div>

          <h1 className="mt-4 text-2xl font-extrabold text-slate-900 sm:text-3xl tracking-tight">
            {post.title}
          </h1>

          <div className="mt-4 flex flex-wrap items-center gap-4 text-xs text-slate-500 border-t border-slate-200/60 pt-4">
            <div className="flex items-center gap-1.5">
              <User className="h-3.5 w-3.5 text-slate-400" />
              <span>{post.user_name || post.user_email || 'Unknown user'}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5 text-slate-400" />
              <span>Posted {post.created_at ? formatDate(post.created_at) : 'recently'}</span>
            </div>
          </div>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          {/* Price Callout */}
          <div className="flex items-center justify-between rounded-2xl bg-gradient-to-r from-indigo-50 via-slate-50 to-slate-50 p-4 border border-indigo-100">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Asking Price</span>
              <p className="text-3xl font-black text-slate-900">
                {post.price != null ? `$${post.price.toFixed(2)}` : 'Free / Contact for detail'}
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600 text-white shadow-md shadow-indigo-600/20">
              <DollarSign className="h-6 w-6" />
            </div>
          </div>

          {/* Description */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
              Description & Details
            </h3>
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-700 bg-slate-50/50 p-4 rounded-xl border border-slate-100">
              {post.description}
            </p>
          </div>

          {/* Report Button Footer */}
          <div className="flex items-center justify-between border-t border-slate-100 pt-4">
            <span className="text-xs text-slate-400">Not matching corporate guidelines?</span>
            <Button variant="danger" size="sm" icon={Flag} onClick={() => setReportModalOpen(true)}>
              Report Post
            </Button>
          </div>
        </div>
      </Card>

      {/* Comments Section */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MessageSquare className="h-4 w-4 text-indigo-600" />
            <h2 className="text-base font-bold text-slate-900">
              Discussion ({comments.length})
            </h2>
          </div>
        </div>

        {comments.length === 0 ? (
          <Card className="p-8 text-center">
            <p className="text-sm font-medium text-slate-600">No comments yet</p>
            <p className="mt-1 text-xs text-slate-400">Ask a question or start a conversation about this item below.</p>
          </Card>
        ) : (
          <Panel>
            {comments.map((c) => (
              <div key={c.id} className="flex gap-3.5 p-4 sm:p-5">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-indigo-700 font-bold text-xs">
                  <User className="h-4 w-4" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-slate-800">{c.user_email || 'Unknown user'}</span>
                    {c.created_at && (
                      <span className="text-[11px] text-slate-400">{formatDate(c.created_at)}</span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-slate-700 leading-relaxed">{c.text}</p>
                </div>
              </div>
            ))}
          </Panel>
        )}

        {/* Add Comment Form */}
        <Card className="p-4 sm:p-5">
          <form onSubmit={submitComment} className="space-y-3">
            <Textarea
              rows={3}
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="Write a comment or ask a question to the seller..."
            />
            <div className="flex justify-end">
              <Button type="submit" loading={submitting} disabled={!commentText.trim()}>
                Post Comment
              </Button>
            </div>
          </form>
        </Card>
      </section>

      {/* Report Modal */}
      <Modal
        isOpen={reportModalOpen}
        onClose={() => setReportModalOpen(false)}
        title="Report Post to Administrators"
      >
        <form onSubmit={submitReport} className="space-y-4">
          <div className="flex items-center gap-3 rounded-xl bg-amber-50 p-3 text-amber-800 text-xs">
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600" />
            <p>Reports are submitted anonymously to team moderators to review policy violations.</p>
          </div>

          <Field label="Reason for reporting" required>
            <Textarea
              value={reportReason}
              onChange={(e) => setReportReason(e.target.value)}
              placeholder="Please explain why this post breaks rules or contains inappropriate content..."
              required
            />
          </Field>

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-3">
            <Button type="button" variant="secondary" onClick={() => setReportModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="danger" loading={reporting}>
              Submit Report
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
