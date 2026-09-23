import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { Button, EmptyState, formatDate, PageHeader, Panel, SkeletonList } from '../components/ui.jsx';
import { Bell, Check, Sparkles, MessageSquare, CheckCircle2, Trash2 } from 'lucide-react';

export default function Notifications() {
  const [notifications, setNotifications] = useState(null);

  function refresh() {
    api.listNotifications().then((res) => setNotifications(res.data));
  }

  useEffect(refresh, []);

  async function markRead(id) {
    await api.markNotificationRead(id);
    refresh();
  }

  async function deleteNotification(id) {
    await api.deleteNotification(id);
    refresh();
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        eyebrow="Activity Center"
        title="Notifications"
        description="Stay updated with comments, approvals, and post updates."
      />

      {notifications === null ? (
        <SkeletonList />
      ) : notifications.length === 0 ? (
        <EmptyState
          title="You're all caught up!"
          description="New notifications regarding your listings or comments will appear here."
          icon={Bell}
        />
      ) : (
        <Panel>
          {notifications.map((n) => (
            <div
              key={n.id}
              className={`relative flex items-center justify-between gap-4 p-5 transition-colors ${
                !n.read_at ? 'bg-indigo-50/40 font-medium' : 'hover:bg-slate-50/80'
              }`}
            >
              {!n.read_at && <span className="absolute inset-y-0 left-0 w-1 bg-indigo-600 rounded-r" />}

              <div className="flex items-start gap-3.5 min-w-0">
                <div
                  className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-xl ${
                    !n.read_at ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  <Bell className="h-4 w-4" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm text-slate-800 leading-snug">{n.message}</p>
                  {n.created_at && (
                    <span className="mt-1 block text-xs text-slate-400">
                      {formatDate(n.created_at)}
                    </span>
                  )}
                </div>
              </div>

              <div className="flex shrink-0 items-center gap-2">
                {!n.read_at && (
                  <Button variant="ghost" size="sm" icon={Check} onClick={() => markRead(n.id)}>
                    Mark Read
                  </Button>
                )}
                <Button
                  variant="danger"
                  size="sm"
                  icon={Trash2}
                  onClick={() => deleteNotification(n.id)}
                  aria-label="Delete notification"
                >
                  Delete
                </Button>
              </div>
            </div>
          ))}
        </Panel>
      )}
    </div>
  );
}
