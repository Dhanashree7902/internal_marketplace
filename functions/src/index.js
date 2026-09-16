import { initializeApp } from 'firebase-admin/app';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { onDocumentCreated } from 'firebase-functions/v2/firestore';

initializeApp();
const db = getFirestore();

async function notifyAdmins({ type, referenceId, message }) {
  const admins = await db.collection('users').where('role', '==', 'admin').where('status', '==', 'ACTIVE').get();
  const batch = db.batch();
  admins.forEach((adminDoc) => {
    const ref = db.collection('notifications').doc();
    batch.set(ref, {
      user_id: adminDoc.id,
      type,
      reference_id: referenceId,
      message,
      read_at: null,
      created_at: FieldValue.serverTimestamp(),
    });
  });
  await batch.commit();
}

// CATEGORY_REQUESTED -> notify admins (spec Section 11).
// Approve/reject notifications to the requester are written synchronously
// by the Cloud Run backend (categoryRequests.controller.js) since the
// requester needs the result as part of the same request/response cycle
// the admin action produced.
export const onCategoryRequestCreate = onDocumentCreated('categoryRequests/{requestId}', async (event) => {
  const request = event.data.data();
  await notifyAdmins({
    type: 'CATEGORY_REQUESTED',
    referenceId: event.params.requestId,
    message: `${request.requested_by} requested a new category: "${request.proposed_name}".`,
  });
});

// POST_REPORTED is handled by the in-process NotificationEventListener in
// backend-java (it needs to resolve the post title/creator, which requires
// the Java PostRepository/UserRepository already wired up there).

// COMMENT_CREATED -> notify the post owner (unless commenting on their own post).
export const onCommentCreate = onDocumentCreated('comments/{commentId}', async (event) => {
  const comment = event.data.data();
  const postDoc = await db.collection('posts').doc(comment.post_id).get();
  if (!postDoc.exists) return;

  const post = postDoc.data();
  if (post.user_id === comment.user_id) return;

  await db.collection('notifications').add({
    user_id: post.user_id,
    type: 'COMMENT_CREATED',
    reference_id: comment.post_id,
    message: `New comment on your post "${post.title}".`,
    read_at: null,
    created_at: FieldValue.serverTimestamp(),
  });
});

// POST_CREATED -> search indexing is handled declaratively by the
// "Search with Algolia" Firebase Extension configured on the `posts`
// collection (see README) rather than a hand-written trigger here.
