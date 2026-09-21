import { auth } from '../firebase.js';

// Requests already in flight, keyed by "METHOD path". A second call for the
// same GET while the first hasn't resolved yet (e.g. React StrictMode's
// double-invoked mount effects, or two components requesting the same data
// in the same tick) reuses that in-flight promise instead of firing a second
// network call. This does NOT cache resolved data -- once a request settles
// it's removed from the map, so a later call at a different time still hits
// the network. Persistent caching (for data like categories that rarely
// change) belongs at the call-site/context level, not here -- see
// CategoriesContext.
const inFlightRequests = new Map();

function logRequest(method, path, source) {
  if (import.meta.env.DEV) {
    console.debug(`[api] ${method} ${path}`, source ? `<- ${source}` : '');
  }
}

async function performRequest(path, { method, body }) {
  const idToken = await auth?.currentUser?.getIdToken();
  const res = await fetch(`/api/v1${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(idToken ? { Authorization: `Bearer ${idToken}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    const payload = await res.json().catch(() => ({}));
    throw new Error(payload.error || `Request failed: ${res.status}`);
  }

  return res.status === 204 ? null : res.json();
}

async function request(path, { method = 'GET', body, source } = {}) {
  logRequest(method, path, source);

  if (method !== 'GET') {
    return performRequest(path, { method, body });
  }

  const dedupeKey = `${method} ${path}`;
  const existing = inFlightRequests.get(dedupeKey);
  if (existing) {
    if (import.meta.env.DEV) {
      console.debug(`[api] coalesced duplicate in-flight request: ${dedupeKey}`, source ? `<- ${source}` : '');
    }
    return existing;
  }

  const promise = performRequest(path, { method, body }).finally(() => {
    inFlightRequests.delete(dedupeKey);
  });
  inFlightRequests.set(dedupeKey, promise);
  return promise;
}

export const api = {
  me: () => request('/me'),
  listCategories: (source) => request('/categories', { source }),
  createCategory: (body) => request('/admin/categories', { method: 'POST', body }),
  updateCategory: (id, body) => request(`/admin/categories/${id}`, { method: 'PATCH', body }),
  listPosts: (params = {}, source) => {
    const cleaned = Object.fromEntries(
      Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
    );
    return request(`/posts?${new URLSearchParams(cleaned)}`, { source });
  },
  getPost: (id) => request(`/posts/${id}`),
  createPost: (body) => request('/posts', { method: 'POST', body }),
  updatePost: (id, body) => request(`/posts/${id}`, { method: 'PATCH', body }),
  deletePost: (id) => request(`/posts/${id}`, { method: 'DELETE' }),
  listComments: (postId) => request(`/posts/${postId}/comments`),
  createComment: (postId, body) => request(`/posts/${postId}/comments`, { method: 'POST', body }),
  createReport: (body) => request('/reports', { method: 'POST', body }),
  listReports: () => request('/admin/reports'),
  reviewReport: (id, status) => request(`/admin/reports/${id}`, { method: 'PATCH', body: { status } }),
  requestCategory: (body) => request('/category-requests', { method: 'POST', body }),
  listCategoryRequests: () => request('/admin/category-requests'),
  approveCategoryRequest: (id) => request(`/admin/category-requests/${id}/approve`, { method: 'POST' }),
  rejectCategoryRequest: (id) => request(`/admin/category-requests/${id}/reject`, { method: 'POST' }),
  listNotifications: () => request('/notifications'),
  markNotificationRead: (id) => request(`/notifications/${id}/read`, { method: 'POST' }),
  deleteNotification: (id) => request(`/notifications/${id}`, { method: 'DELETE' }),
  searchEmployees: (query) => request(`/admin/users?query=${encodeURIComponent(query)}`),
  promoteToAdmin: (uid) => request(`/admin/users/${uid}/promote`, { method: 'POST' }),
  listRoleAssignments: () => request('/admin/users/role-assignments'),
  requestRoleRemoval: () => request('/admin/role-removal-requests', { method: 'POST' }),
  listRoleRemovalRequests: () => request('/admin/role-removal-requests'),
  approveRoleRemoval: (id) => request(`/admin/role-removal-requests/${id}/approve`, { method: 'POST' }),
  rejectRoleRemoval: (id) => request(`/admin/role-removal-requests/${id}/reject`, { method: 'POST' }),
  cancelRoleRemoval: (id) => request(`/admin/role-removal-requests/${id}/cancel`, { method: 'POST' }),
};
