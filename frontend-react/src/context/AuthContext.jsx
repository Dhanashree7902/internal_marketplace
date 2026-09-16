import { createContext, useContext, useEffect, useState } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth, configError } from '../firebase.js';
import { api } from '../api/client.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [firebaseUser, setFirebaseUser] = useState(auth ? undefined : null);
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    if (!auth) return undefined;

    return onAuthStateChanged(auth, async (user) => {
      setFirebaseUser(user);
      if (user) {
        try {
          setProfile(await api.me());
        } catch {
          setProfile(null);
        }
      } else {
        setProfile(null);
      }
    });
  }, []);

  const loading = firebaseUser === undefined;
  const isAdmin = profile?.role === 'admin';

  if (configError) {
    return (
      <main style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
        <h1>Configuration needed</h1>
        <p>{configError}</p>
      </main>
    );
  }

  return (
    <AuthContext.Provider value={{ firebaseUser, profile, loading, isAdmin }}>{children}</AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
