import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,  setUser]  = useState(() => {
    try { return JSON.parse(localStorage.getItem('vf_user')); } catch { return null; }
  });
  const [token, setToken] = useState(() => localStorage.getItem('vf_token'));

  const login = useCallback((authResponse) => {
    const { accessToken, user: userData } = authResponse;
    localStorage.setItem('vf_token', accessToken);
    localStorage.setItem('vf_user',  JSON.stringify(userData));
    setToken(accessToken);
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('vf_token');
    localStorage.removeItem('vf_user');
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
};
