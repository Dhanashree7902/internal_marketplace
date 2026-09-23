import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCategories } from '../context/CategoriesContext.jsx';
import { Badge, Button, Card, ErrorState, Field, Input, PageHeader, Select, Spinner, Textarea } from '../components/ui.jsx';
import { PlusCircle, Save, Eye, AlertCircle } from 'lucide-react';

const LISTING_TYPES = [
  'SELL',
  'BUY_REQUEST',
  'RENT',
  'OFFER',
  'REQUEST',
  'INFORMATION',
  'Other',
];

const EMPTY_FORM = {
  categoryId: '',
  title: '',
  description: '',
  // Deriving the default from the list itself, rather than a separately
  // hardcoded literal, is what actually prevents the recurring bug where
  // editing LISTING_TYPES leaves this default pointing at a value that no
  // longer exists in the dropdown.
  postType: LISTING_TYPES[0],
  customPostType: '',
  price: '',
};

export default function CreateEditPost() {
  const navigate = useNavigate();
  const { postId } = useParams();
  const isEditMode = Boolean(postId);
  const { profile, isAdmin } = useAuth();
  const { categories } = useCategories();

  const [form, setForm] = useState(EMPTY_FORM);
  const [loadedPost, setLoadedPost] = useState(null);
  const [loadingPost, setLoadingPost] = useState(isEditMode);
  const [loadError, setLoadError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!isEditMode) return;
    api
      .getPost(postId)
      .then((res) => setLoadedPost(res.data))
      .catch((err) => setLoadError(err.message || 'Failed to load this post.'))
      .finally(() => setLoadingPost(false));
  }, [postId, isEditMode]);

  useEffect(() => {
    if (!loadedPost) return;
    setForm({
      categoryId: loadedPost.category_id,
      title: loadedPost.title,
      description: loadedPost.description,
      postType: loadedPost.post_type,
      customPostType: '',
      price: loadedPost.price != null ? String(loadedPost.price) : '',
    });
  }, [loadedPost]);

  // Only decided once both the post and the viewer's own profile are loaded --
  // profile can still be null for a moment after auth resolves (AuthContext's
  // api.me() is a separate async call), and treating that as "not the owner"
  // would flash an incorrect access-denied screen for the real owner.
  const accessDenied =
    isEditMode && loadedPost && profile && !isAdmin && loadedPost.user_id !== profile.uid;

  const isOtherType = !isEditMode && form.postType === 'Other';
  // The value actually saved: either the chosen category, or -- when "Other"
  // is picked -- whatever custom listing type the user typed in its place.
  const resolvedPostType = isOtherType ? form.customPostType.trim() : form.postType;

  async function submit(e) {
    e.preventDefault();
    setError(null);

    if (isOtherType && !resolvedPostType) {
      setError('Please enter a custom listing type.');
      return;
    }

    const price = form.price === '' ? undefined : Number(form.price);
    if (price !== undefined && price < 0) {
      setError('Price cannot be negative.');
      return;
    }

    setSubmitting(true);
    try {
      if (isEditMode) {
        await api.updatePost(postId, { title: form.title, description: form.description, price });
        navigate(`/posts/${postId}`);
      } else {
        const { id } = await api.createPost({
          categoryId: form.categoryId,
          title: form.title,
          description: form.description,
          postType: resolvedPostType,
          price,
        });
        navigate(`/posts/${id}`);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  const selectedCategoryName = categories?.find((c) => c.id === form.categoryId)?.name || 'Category Name';

  if (loadingPost) return <Spinner />;

  if (loadError) {
    return <ErrorState title="Couldn't load this post" description={loadError} />;
  }

  if (accessDenied) {
    return (
      <ErrorState
        title="You don't have permission to edit this post"
        description="Only the post's owner or an admin can make changes to it."
      />
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Marketplace"
        title={isEditMode ? 'Edit Listing' : 'Create a New Listing'}
        description={
          isEditMode
            ? 'Update the title, description, or price of this listing.'
            : 'Share items, gear, or services with your colleagues across the company.'
        }
      />

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        {/* Form Controls */}
        <div className="lg:col-span-7">
          <Card className="p-6 sm:p-8">
            <form onSubmit={submit} className="space-y-5">
              {/* Post Type Selector */}
              {isEditMode ? (
                <Field label="Listing Type" hint="Can't be changed after creation">
                  <Input value={form.postType} disabled />
                </Field>
              ) : (
                <Field label="Listing Type" required>
                  <Select
                    value={form.postType}
                    onChange={(e) => setForm({ ...form, postType: e.target.value })}
                    required
                  >
                    {LISTING_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </Select>
                </Field>
              )}

              {isOtherType && (
                <Field label="Custom Listing Type" hint="Describe this listing type in a few words" required>
                  <Input
                    placeholder="e.g. Volunteer Opportunity"
                    value={form.customPostType}
                    onChange={(e) => setForm({ ...form, customPostType: e.target.value })}
                    required
                  />
                </Field>
              )}

              {/* Category */}
              {isEditMode ? (
                <Field label="Category" hint="Can't be changed after creation">
                  <Input value={selectedCategoryName} disabled />
                </Field>
              ) : (
                <Field label="Category" required>
                  <Select
                    value={form.categoryId}
                    onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                    required
                  >
                    <option value="" disabled>
                      Select a category
                    </option>
                    {(categories || []).map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </Select>
                </Field>
              )}

              {/* Title */}
              <Field label="Title" hint="Be concise and clear" required>
                <Input
                  placeholder="e.g. Ergonomic Office Chair (Barely Used)"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  required
                />
              </Field>

              {/* Price */}
              <Field label="Price ($ USD)" hint="Leave blank if free or negotiable">
                <div className="relative">
                  <div className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 font-bold text-sm">
                    $
                  </div>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="0.00"
                    value={form.price}
                    onChange={(e) => setForm({ ...form, price: e.target.value })}
                    className="w-full rounded-xl border border-slate-200 bg-white py-2 pl-8 pr-3.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15"
                  />
                </div>
              </Field>

              {/* Description */}
              <Field label="Description" required>
                <Textarea
                  placeholder="Provide essential details such as condition, specs, location, or availability..."
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  required
                />
              </Field>

              {error && (
                <div className="flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs font-semibold text-rose-700">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
                <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
                  Cancel
                </Button>
                <Button type="submit" loading={submitting} icon={isEditMode ? Save : PlusCircle}>
                  {isEditMode ? 'Save Changes' : 'Publish Listing'}
                </Button>
              </div>
            </form>
          </Card>
        </div>

        {/* Live Preview Column */}
        <div className="lg:col-span-5 space-y-4">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-400">
            <Eye className="h-4 w-4 text-indigo-600" />
            <span>Live Marketplace Preview</span>
          </div>

          <Card className="p-6 relative overflow-hidden bg-gradient-to-br from-white to-slate-50 border-indigo-100 shadow-md">
            <div className="flex items-center justify-between">
              <Badge tone={isOtherType ? resolvedPostType : form.postType}>
                {resolvedPostType || 'Other'}
              </Badge>
              <Badge tone="ACTIVE">ACTIVE</Badge>
            </div>

            <div className="mt-4 space-y-2">
              <h3 className="text-base font-bold text-slate-900 line-clamp-2">
                {form.title || 'Your Post Title Will Appear Here'}
              </h3>
              <p className="text-xs text-slate-500 line-clamp-3 leading-relaxed">
                {form.description || 'Your item details and description will be displayed here in full view for buyers.'}
              </p>
            </div>

            <div className="mt-6 flex items-center justify-between border-t border-slate-200/80 pt-4">
              <div>
                <span className="block text-[10px] uppercase font-bold tracking-wider text-indigo-600">
                  {selectedCategoryName}
                </span>
                <span className="text-[11px] text-slate-400">Preview Mode</span>
              </div>
              <span className="text-xl font-black text-slate-900">
                {form.price ? `$${Number(form.price).toFixed(2)}` : '$0.00'}
              </span>
            </div>
          </Card>

          <div className="rounded-xl bg-indigo-50/60 p-4 border border-indigo-100 text-xs text-indigo-900">
            <p className="font-semibold">💡 Tips for a successful listing:</p>
            <ul className="mt-1 space-y-1 list-disc list-inside text-indigo-700">
              <li>Use a clear, descriptive title.</li>
              <li>Include item condition and any accessories.</li>
              <li>Set a fair price or note if open to offers.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
