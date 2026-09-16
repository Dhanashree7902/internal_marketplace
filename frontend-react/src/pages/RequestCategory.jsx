import { useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { Button, Card, Field, Input, PageHeader, Panel, Textarea } from '../components/ui.jsx';
import { FolderPlus, Send, CheckCircle2, AlertCircle } from 'lucide-react';

export default function RequestCategory() {
  const { isAdmin } = useAuth();
  const [form, setForm] = useState({ proposedName: '', reason: '' });
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setSubmitting(true);
    setStatus(null);
    try {
      await api.requestCategory(form);
      setStatus({
        type: 'success',
        message: isAdmin
          ? `Category "${form.proposedName}" created and available immediately.`
          : 'Proposal submitted successfully! Administrators will review your request.',
      });
      setForm({ proposedName: '', reason: '' });
    } catch (err) {
      setStatus({ type: 'error', message: err.message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <PageHeader
        eyebrow="Marketplace Expansion"
        title="Request a New Category"
        description={
          isAdmin
            ? 'As an admin, submitting here creates the category immediately — no review needed.'
            : "Don't see a fitting category for your item? Propose one to company administrators."
        }
      />

      <Card className="p-6 sm:p-8">
        <form onSubmit={submit} className="space-y-5">
          <Field label="Proposed Category Name" hint="Keep it short and general" required>
            <Input
              placeholder="e.g. Photography & Video Gear"
              value={form.proposedName}
              onChange={(e) => setForm({ ...form, proposedName: e.target.value })}
              required
            />
          </Field>

          <Field label="Why is this category needed?" hint="Explain the purpose for the team" required>
            <Textarea
              placeholder="Describe what kind of items or services colleagues would list in this category..."
              value={form.reason}
              onChange={(e) => setForm({ ...form, reason: e.target.value })}
              required
            />
          </Field>

          {status && (
            <div
              className={`flex items-center gap-2 rounded-xl p-4 text-xs font-semibold ${
                status.type === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'
              }`}
            >
              {status.type === 'success' ? <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" /> : <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />}
              <span>{status.message}</span>
            </div>
          )}

          <div className="flex justify-end border-t border-slate-100 pt-4">
            <Button type="submit" loading={submitting} icon={Send}>
              Submit Category Proposal
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
