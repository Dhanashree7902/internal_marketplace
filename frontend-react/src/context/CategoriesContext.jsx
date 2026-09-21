import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useAuth } from './AuthContext.jsx';

const CategoriesContext = createContext(null);

// Categories rarely change and are read by five different pages (Home,
// CategoryPage, AdminDashboard, CreateEditPost, PostDetail). Fetching them
// here once, at the app root, and handing every consumer the same cached
// list is what actually stops the repeated /categories calls -- the
// in-flight dedup in api/client.js only protects a single overlapping
// fetch, not "the same data re-fetched on every page navigation."
export function CategoriesProvider({ children }) {
  const { firebaseUser } = useAuth();
  const [categories, setCategories] = useState(null);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    return api
      .listCategories('CategoriesProvider')
      .then((res) => setCategories(res.data))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!firebaseUser) {
      setCategories(null);
      return;
    }
    refresh();
  }, [firebaseUser, refresh]);

  return (
    <CategoriesContext.Provider value={{ categories, loading, refresh }}>
      {children}
    </CategoriesContext.Provider>
  );
}

export function useCategories() {
  const ctx = useContext(CategoriesContext);
  if (!ctx) throw new Error('useCategories must be used within CategoriesProvider');
  return ctx;
}
