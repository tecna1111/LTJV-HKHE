import { create } from 'zustand';

const readStored = (key) => localStorage.getItem(key) || sessionStorage.getItem(key);
const authKeys = ['cosre_token', 'cosre_refresh_token', 'cosre_username', 'cosre_full_name', 'cosre_role'];

const useAuthStore = create((set) => ({
  token: readStored('cosre_token'),
  refreshToken: readStored('cosre_refresh_token'),
  username: readStored('cosre_username'),
  fullName: readStored('cosre_full_name'),
  role: readStored('cosre_role'),
  setIdentity: (username, fullName, role) => set({ username, fullName, role }),
  setAuth: (token, refreshToken, username, fullName, role, remember = true) => {
    authKeys.forEach((key) => { localStorage.removeItem(key); sessionStorage.removeItem(key); });
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem('cosre_token', token);
    storage.setItem('cosre_refresh_token', refreshToken);
    storage.setItem('cosre_username', username);
    storage.setItem('cosre_full_name', fullName);
    storage.setItem('cosre_role', role);
    set({ token, refreshToken, username, fullName, role });
  },
  clearAuth: () => {
    authKeys.forEach((key) => { localStorage.removeItem(key); sessionStorage.removeItem(key); });
    set({ token: null, refreshToken: null, username: null, fullName: null, role: null });
  },
}));

export const getAuthToken = () => readStored('cosre_token');
export const getRefreshToken = () => readStored('cosre_refresh_token');
export const replaceTokens = (token, refreshToken) => {
  const storage = localStorage.getItem('cosre_refresh_token') ? localStorage : sessionStorage;
  storage.setItem('cosre_token', token);
  storage.setItem('cosre_refresh_token', refreshToken);
  useAuthStore.setState({ token, refreshToken });
};
export default useAuthStore;
