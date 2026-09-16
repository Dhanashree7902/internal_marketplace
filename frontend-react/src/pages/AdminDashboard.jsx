import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  Field,
  formatDate,
  Input,
  Modal,
  PageHeader,
  Panel,
  SectionHeading,
  StatTile,
  SkeletonList,
} from '../components/ui.jsx';
import {
  ShieldCheck,
  FolderPlus,
  Folder,
  CheckCircle2,
  XCircle,
  Edit2,
  Trash2,
  PlusCircle,
  Clock,
  Layers,
  Sparkles,
  Flag,
  Search,
  UserPlus,
  History,
  UserMinus,
} from 'lucide-react';

export default function AdminDashboard() {
  const { profile } = useAuth();
  const [requests, setRequests] = useState(null);
  const [categories, setCategories] = useState(null);
  const [reports, setReports] = useState(null);
  const [newCategory, setNewCategory] = useState({ name: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [message, setMessage] = useState(null);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);

  const [userQuery, setUserQuery] = useState('');
  const [candidates, setCandidates] = useState([]);
  const [searchingUsers, setSearchingUsers] = useState(false);
  const [selectedCandidate, setSelectedCandidate] = useState(null);
  const [promoting, setPromoting] = useState(false);
  const [assignments, setAssignments] = useState(null);

  const [removalRequests, setRemovalRequests] = useState(null);
  const [requestingRemoval, setRequestingRemoval] = useState(false);
  const [removalActionId, setRemovalActionId] = useState(null);

  function refreshRequests() {
    api.listCategoryRequests().then((res) => setRequests(res.data));
  }

  function refreshCategories() {
    api.listCategories().then((res) => setCategories(res.data));
  }

  function refreshReports() {
    api.listReports().then((res) => setReports(res.data));
  }

  function refreshAssignments() {
    api.listRoleAssignments().then((res) => setAssignments(res.data));
  }

  function refreshRemovalRequests() {
    api.listRoleRemovalRequests().then((res) => setRemovalRequests(res.data));
  }

  useEffect(refreshRequests, []);
  useEffect(refreshCategories, []);
  useEffect(refreshReports, []);
  useEffect(refreshAssignments, []);
  useEffect(refreshRemovalRequests, []);

  useEffect(() => {
    const query = userQuery.trim();
    if (query.length < 2) {
      setCandidates([]);
      setSearchingUsers(false);
      return undefined;
    }
    setSearchingUsers(true);
    const timeout = setTimeout(() => {
      api.searchEmployees(query)
        .then((res) => setCandidates(res.data))
        .finally(() => setSearchingUsers(false));
    }, 300);
    return () => clearTimeout(timeout);
  }, [userQuery]);

  async function confirmPromote() {
    setPromoting(true);
    try {
      await api.promoteToAdmin(selectedCandidate.uid);
      setMessage({ type: 'success', text: `${selectedCandidate.name} was granted Admin access.` });
      setSelectedCandidate(null);
      setUserQuery('');
      setCandidates([]);
      refreshAssignments();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setPromoting(false);
    }
  }

  async function requestRoleRemoval() {
    setRequestingRemoval(true);
    setMessage(null);
    try {
      await api.requestRoleRemoval();
      setMessage({ type: 'success', text: 'Your Admin access removal request was submitted for review.' });
      refreshRemovalRequests();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setRequestingRemoval(false);
    }
  }

  async function decideRoleRemoval(id, action) {
    setRemovalActionId(id);
    setMessage(null);
    try {
      await action(id);
      refreshRemovalRequests();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setRemovalActionId(null);
    }
  }

  async function resolveReport(id, status) {
    setMessage(null);
    try {
      await api.reviewReport(id, status);
      refreshReports();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  async function createCategory(e) {
    e.preventDefault();
    setCreating(true);
    setMessage(null);
    try {
      await api.createCategory(newCategory);
      setMessage({ type: 'success', text: `Category "${newCategory.name}" created successfully.` });
      setNewCategory({ name: '', description: '' });
      refreshCategories();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setCreating(false);
    }
  }

  function startEdit(c) {
    setMessage(null);
    setEditing({ id: c.id, name: c.name, description: c.description || '' });
  }

  function cancelEdit() {
    setEditing(null);
  }

  async function saveEdit(e) {
    e.preventDefault();
    setSaving(true);
    try {
      await api.updateCategory(editing.id, { name: editing.name, description: editing.description });
      setEditing(null);
      refreshCategories();
      setMessage({ type: 'success', text: 'Category updated.' });
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    } finally {
      setSaving(false);
    }
  }

  async function removeCategory(c) {
    if (!window.confirm(`Delete category "${c.name}"? It will be archived and hidden from users.`)) return;
    setMessage(null);
    try {
      await api.updateCategory(c.id, { status: 'ARCHIVED' });
      refreshCategories();
    } catch (err) {
      setMessage({ type: 'error', text: err.message });
    }
  }

  async function approve(id) {
    await api.approveCategoryRequest(id);
    refreshRequests();
    refreshCategories();
  }

  async function reject(id) {
    await api.rejectCategoryRequest(id);
    refreshRequests();
  }

  const myPendingRemoval = removalRequests?.find((r) => r.requested_by_uid === profile?.uid);

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="System Administration"
        title="Admin Control Panel"
        description="Manage marketplace categories, review user proposals, and configure system rules."
      />

      {/* Metrics Row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatTile
          label="Pending Category Requests"
          value={requests === null ? '—' : requests.length}
          icon={Clock}
        />
        <StatTile
          label="Active Categories"
          value={categories === null ? '—' : categories.length}
          icon={Folder}
        />
        <StatTile
          label="Pending Reports"
          value={reports === null ? '—' : reports.length}
          icon={Flag}
        />
      </div>

      {/* Message Feedback */}
      {message && (
        <div
          className={`rounded-2xl p-4 text-sm font-semibold flex items-center justify-between animate-in fade-in ${
            message.type === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'
          }`}
        >
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} className="text-xs font-bold underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Reported Content */}
      <section className="space-y-4">
        <SectionHeading
          title="Reported Content"
          description="Review posts and comments flagged by users. Resolve confirms action was taken; Dismiss clears the report."
        />

        {reports === null ? (
          <SkeletonList />
        ) : reports.length === 0 ? (
          <EmptyState
            title="No pending reports"
            description="Content reported by team members will appear here for review."
            icon={Flag}
          />
        ) : (
          <Panel>
            {reports.map((r) => (
              <div
                key={r.id}
                className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between transition-colors hover:bg-slate-50/80"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-bold text-slate-900 truncate">
                      {r.target_title || `${r.target_type} ${r.target_id}`}
                    </h4>
                    <Badge tone="PENDING">{r.target_type?.toUpperCase()}</Badge>
                  </div>
                  {r.target_creator_name && (
                    <p className="mt-0.5 text-xs text-slate-500">Posted by {r.target_creator_name}</p>
                  )}
                  <p className="mt-1 text-xs text-slate-600 font-medium">"{r.reason}"</p>
                  <span className="mt-1 block text-[11px] text-slate-400">
                    Reported by {r.reporter_name || r.reporter_email || 'a user'}
                    {r.created_at && ` on ${formatDate(r.created_at)}`}
                  </span>
                </div>
                <div className="flex shrink-0 gap-2 self-end sm:self-center">
                  <Button size="sm" variant="primary" icon={CheckCircle2} onClick={() => resolveReport(r.id, 'RESOLVED')}>
                    Resolve
                  </Button>
                  <Button size="sm" variant="danger" icon={XCircle} onClick={() => resolveReport(r.id, 'DISMISSED')}>
                    Dismiss
                  </Button>
                </div>
              </div>
            ))}
          </Panel>
        )}
      </section>

      {/* User Management */}
      <section className="space-y-4">
        <SectionHeading
          title="User Management"
          description="Search for an employee and grant them Admin access."
        />

        <Card className="p-5 space-y-4">
          <Field label="Search Employees" hint="By name or email">
            <Input
              icon={Search}
              placeholder="e.g. Jane Doe or jane@company.com"
              value={userQuery}
              onChange={(e) => setUserQuery(e.target.value)}
            />
          </Field>

          {searchingUsers ? (
            <SkeletonList />
          ) : userQuery.trim().length >= 2 && candidates.length === 0 ? (
            <p className="text-xs text-slate-500">No matching employees found.</p>
          ) : candidates.length > 0 ? (
            <div className="divide-y divide-slate-100 rounded-xl border border-slate-200 overflow-hidden">
              {candidates.map((c) => (
                <div key={c.uid} className="flex items-center justify-between gap-4 p-4">
                  <div className="min-w-0">
                    <h4 className="text-sm font-bold text-slate-900 truncate">{c.name}</h4>
                    <p className="text-xs text-slate-500 truncate">
                      {c.email}
                      {c.department && ` · ${c.department}`}
                    </p>
                  </div>
                  <Button size="sm" variant="primary" icon={UserPlus} onClick={() => setSelectedCandidate(c)}>
                    Make Admin
                  </Button>
                </div>
              ))}
            </div>
          ) : null}
        </Card>

        <Panel
          title="Recent Admin Assignments"
          description="Assigned By, Assigned To, and when the promotion happened."
        >
          {assignments === null ? (
            <div className="p-4">
              <SkeletonList />
            </div>
          ) : assignments.length === 0 ? (
            <EmptyState
              title="No admin assignments yet"
              description="Promotions made from this panel will be logged here."
              icon={History}
            />
          ) : (
            assignments.map((a, idx) => (
              <div key={idx} className="flex items-center justify-between gap-4 p-4">
                <p className="text-sm text-slate-700">
                  <span className="font-bold text-slate-900">{a.assigned_to_name}</span> was made Admin by{' '}
                  <span className="font-semibold text-slate-900">{a.assigned_by_name}</span>
                </p>
                {a.created_at && <span className="shrink-0 text-xs text-slate-400">{formatDate(a.created_at)}</span>}
              </div>
            ))
          )}
        </Panel>
      </section>

      {/* Pending Role Change Requests */}
      <section className="space-y-4">
        <SectionHeading
          title="Pending Role Change Requests"
          description="Admins stepping back to Employee must be approved by another Admin."
          action={
            !myPendingRemoval && (
              <Button
                variant="danger"
                icon={UserMinus}
                loading={requestingRemoval}
                onClick={requestRoleRemoval}
              >
                Request Admin Access Removal
              </Button>
            )
          }
        />

        {removalRequests === null ? (
          <SkeletonList />
        ) : removalRequests.length === 0 ? (
          <EmptyState
            title="No pending role change requests"
            description="Requests from admins to remove their own Admin access will appear here for review."
            icon={UserMinus}
          />
        ) : (
          <Panel>
            {removalRequests.map((r) => {
              const isOwnRequest = r.requested_by_uid === profile?.uid;
              return (
                <div
                  key={r.id}
                  className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between transition-colors hover:bg-slate-50/80"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-slate-900">{r.requested_by_name}</h4>
                      <Badge tone="PENDING">PENDING</Badge>
                    </div>
                    <p className="mt-1 text-xs text-slate-600">{r.requested_by_email}</p>
                    {r.created_at && (
                      <span className="mt-1 block text-[11px] text-slate-400">
                        Requested on {formatDate(r.created_at)}
                      </span>
                    )}
                  </div>
                  <div className="flex shrink-0 gap-2 self-end sm:self-center">
                    {isOwnRequest ? (
                      <Button
                        size="sm"
                        variant="secondary"
                        loading={removalActionId === r.id}
                        onClick={() => decideRoleRemoval(r.id, api.cancelRoleRemoval)}
                      >
                        Cancel Request
                      </Button>
                    ) : (
                      <>
                        <Button
                          size="sm"
                          variant="primary"
                          icon={CheckCircle2}
                          loading={removalActionId === r.id}
                          onClick={() => decideRoleRemoval(r.id, api.approveRoleRemoval)}
                        >
                          Approve
                        </Button>
                        <Button
                          size="sm"
                          variant="danger"
                          icon={XCircle}
                          loading={removalActionId === r.id}
                          onClick={() => decideRoleRemoval(r.id, api.rejectRoleRemoval)}
                        >
                          Reject
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </Panel>
        )}
      </section>

      {/* Category Management */}
      <section className="space-y-4">
        <SectionHeading title="Category Management" description="Create new taxonomy categories or update active ones." />

        <Panel>
          {/* Create Form Header */}
          <form onSubmit={createCategory} className="border-b border-slate-100 bg-slate-50/50 p-5 space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">Add New Category</h3>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-12 sm:items-end">
              <div className="sm:col-span-5">
                <Field label="Category Name" required>
                  <Input
                    placeholder="e.g. Office Ergonomics"
                    value={newCategory.name}
                    onChange={(e) => setNewCategory({ ...newCategory, name: e.target.value })}
                    required
                  />
                </Field>
              </div>
              <div className="sm:col-span-5">
                <Field label="Description">
                  <Input
                    placeholder="Brief description..."
                    value={newCategory.description}
                    onChange={(e) => setNewCategory({ ...newCategory, description: e.target.value })}
                  />
                </Field>
              </div>
              <div className="sm:col-span-2">
                <Button type="submit" loading={creating} icon={PlusCircle} className="w-full">
                  Create
                </Button>
              </div>
            </div>
          </form>

          {/* Categories List */}
          {categories === null ? (
            <div className="p-4 space-y-2">
              <SkeletonList />
            </div>
          ) : categories.length === 0 ? (
            <EmptyState title="No categories defined" description="Use the form above to create the first category." />
          ) : (
            categories.map((c) =>
              editing?.id === c.id ? (
                <form key={c.id} onSubmit={saveEdit} className="p-5 bg-indigo-50/40 border-y border-indigo-100 space-y-3">
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-12 sm:items-end">
                    <div className="sm:col-span-5">
                      <Field label="Category Name" required>
                        <Input
                          value={editing.name}
                          onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                          required
                        />
                      </Field>
                    </div>
                    <div className="sm:col-span-5">
                      <Field label="Description">
                        <Input
                          value={editing.description}
                          onChange={(e) => setEditing({ ...editing, description: e.target.value })}
                        />
                      </Field>
                    </div>
                    <div className="sm:col-span-2 flex gap-2">
                      <Button type="submit" size="sm" loading={saving}>
                        Save
                      </Button>
                      <Button type="button" size="sm" variant="secondary" onClick={cancelEdit}>
                        Cancel
                      </Button>
                    </div>
                  </div>
                </form>
              ) : (
                <div key={c.id} className="flex items-center justify-between gap-4 p-5 transition-colors hover:bg-slate-50/80">
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-slate-900">{c.name}</h4>
                      <Badge tone={c.status}>{c.status}</Badge>
                    </div>
                    {c.description && <p className="mt-0.5 text-xs text-slate-500">{c.description}</p>}
                  </div>
                  <div className="flex items-center gap-2">
                    <Button size="sm" variant="secondary" icon={Edit2} onClick={() => startEdit(c)}>
                      Edit
                    </Button>
                    <Button size="sm" variant="danger" icon={Trash2} onClick={() => removeCategory(c)}>
                      Archive
                    </Button>
                  </div>
                </div>
              )
            )
          )}
        </Panel>
      </section>

      {/* Pending Category Requests */}
      <section className="space-y-4">
        <SectionHeading
          title="Pending Category Proposals"
          description="Review user category requests. Approving auto-creates the category."
        />

        {requests === null ? (
          <SkeletonList />
        ) : requests.length === 0 ? (
          <EmptyState
            title="No pending proposals"
            description="Category proposals submitted by team members will appear here for review."
          />
        ) : (
          <Panel>
            {requests.map((r) => (
              <div
                key={r.id}
                className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between transition-colors hover:bg-slate-50/80"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-bold text-slate-900">{r.proposed_name}</h4>
                    <Badge tone="PENDING">PENDING</Badge>
                  </div>
                  <p className="mt-1 text-xs text-slate-600 font-medium">"{r.reason}"</p>
                  {r.created_at && (
                    <span className="mt-1 block text-[11px] text-slate-400">
                      Requested on {formatDate(r.created_at)}
                    </span>
                  )}
                </div>
                <div className="flex shrink-0 gap-2 self-end sm:self-center">
                  <Button size="sm" variant="primary" icon={CheckCircle2} onClick={() => approve(r.id)}>
                    Approve
                  </Button>
                  <Button size="sm" variant="danger" icon={XCircle} onClick={() => reject(r.id)}>
                    Reject
                  </Button>
                </div>
              </div>
            ))}
          </Panel>
        )}
      </section>

      <Modal isOpen={!!selectedCandidate} onClose={() => setSelectedCandidate(null)} title="Grant Admin Access">
        {selectedCandidate && (
          <div className="space-y-5">
            <p className="text-sm text-slate-600">
              Grant Admin access to <span className="font-bold text-slate-900">{selectedCandidate.name}</span> (
              {selectedCandidate.email})? They will immediately gain full access to all Admin Dashboard features
              and permissions.
            </p>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setSelectedCandidate(null)} disabled={promoting}>
                Cancel
              </Button>
              <Button variant="primary" icon={ShieldCheck} loading={promoting} onClick={confirmPromote}>
                Confirm
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
