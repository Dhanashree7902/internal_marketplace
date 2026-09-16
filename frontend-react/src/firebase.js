import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

// A missing/placeholder config must never blank-crash the whole app at
// import time — that leaves an empty <div id="root"> with no visible
// error. Fall back to a disabled auth client and surface a clear message
// in the UI instead (see configError below).
export let configError = null;
export let app = null;
export let auth = null;

if (!firebaseConfig.apiKey || !firebaseConfig.projectId) {
  configError = 'Firebase is not configured: copy frontend/.env.example to frontend/.env and fill in your project config.';
  console.error(configError);
} else {
  try {
    app = initializeApp(firebaseConfig);
    auth = getAuth(app);
  } catch (err) {
    configError = `Firebase failed to initialize: ${err.message}`;
    console.error(configError, err);
  }
}

const googleProvider = new GoogleAuthProvider();
const allowedDomain = import.meta.env.VITE_ALLOWED_WORKSPACE_DOMAIN;
if (allowedDomain) {
  googleProvider.setCustomParameters({ hd: allowedDomain });
}

export function signInWithGoogle() {
  if (!auth) return Promise.reject(new Error(configError || 'Firebase is not configured'));
  return signInWithPopup(auth, googleProvider);
}

export function signOutUser() {
  if (!auth) return Promise.resolve();
  return signOut(auth);
}
