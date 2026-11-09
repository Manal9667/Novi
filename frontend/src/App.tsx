import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import FindYourNextRead from './pages/FindYourNextRead';
import BookDetails from './pages/BookDetails';
import MyLibrary from './pages/MyLibrary';
import NotFound from './pages/NotFound';

export default function App() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  return (
    <>
      <NavBar />
      <main className="app-main route-fade" key={location.pathname}>
        <Routes location={location}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          {/* The natural-language discovery experience is the home page.
              Signed-out visitors see the marketing landing page. */}
          <Route path="/" element={isAuthenticated ? <FindYourNextRead /> : <Landing />} />
          <Route path="/books/:id" element={<ProtectedRoute><BookDetails /></ProtectedRoute>} />
          <Route path="/library" element={<ProtectedRoute><MyLibrary /></ProtectedRoute>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}
