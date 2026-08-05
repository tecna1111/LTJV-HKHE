import { create } from 'zustand';

const readStored = (key) => localStorage.getItem(key) || sessionStorage.getItem(key);
const authKeys = ['cosre_token', 'cosre_username', 'cosre_full_name', 'cosre_role'];

const useAuthStore = create((set) => ({
  token: readStored('cosre_token'),
  username: readStored('cosre_username'),
  fullName: readStored('cosre_full_name'),
  role: readStored('cosre_role'),
  setAuth: (token, username, fullName, role, remember = true) => {
    authKeys.forEach((key) => { localStorage.removeItem(key); sessionStorage.removeItem(key); });
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem('cosre_token', token);
    storage.setItem('cosre_username', username);
    storage.setItem('cosre_full_name', fullName);
    storage.setItem('cosre_role', role);
    set({ token, username, fullName, role });
  },
  clearAuth: () => {
    authKeys.forEach((key) => { localStorage.removeItem(key); sessionStorage.removeItem(key); });
    set({ token: null, username: null, fullName: null, role: null });
  },
}));

export const getAuthToken = () => readStored('cosre_token');
export default useAuthStore;
