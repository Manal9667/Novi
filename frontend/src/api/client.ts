import axios from 'axios';

// Default to a same-origin relative path so the app works both in local dev
// (Vite proxies /api -> the backend, see vite.config.ts) and in the Docker
// deployment (nginx proxies /api -> the backend, see nginx.conf) without
// depending on CORS or the backend port being published. Override with
// VITE_API_BASE_URL when the API lives on a different origin.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL
});

// SECURITY TRADEOFF: the JWT is kept in localStorage, which is readable by any
// script running on the page, so a successful XSS could exfiltrate it. The more
// robust option is to have the backend issue the token in an httpOnly, Secure,
// SameSite cookie so JavaScript can never read it. That is deliberately not done
// here because it would require re-architecting the API's stateless Bearer-token
// auth (cookie issuance + CSRF protection + credentialed CORS). Until then, the
// mitigation is to keep the app free of XSS sinks (React escapes by default; avoid
// dangerouslySetInnerHTML) and to keep token lifetimes short.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('novi_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('novi_token');
      localStorage.removeItem('novi_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
