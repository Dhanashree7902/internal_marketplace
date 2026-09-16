import { auth } from '../firebase.js';

async function request(path, { method = 'GET', body } = {}) {
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

export const api = {
  me: () => request('/me'),
  listCategories: () => request('/categories'),
  createCategory: (body) => request('/admin/categories', { method: 'POST', body }),
  updateCategory: (id, body) => request(`/admin/categories/${id}`, { method: 'PATCH', body }),
  listPosts: (params = {}) => {
    const cleaned = Object.fromEntries(
      Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
    );
    return request(`/posts?${new URLSearchParams(cleaned)}`);
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
