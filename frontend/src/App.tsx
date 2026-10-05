import React from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Recommendations from './pages/Recommendations';
import FindYourNextRead from './pages/FindYourNextRead';
import Wrapped from './pages/Wrapped';
import Search from './pages/Search';
import Scan from './pages/Scan';
import BookDetails from './pages/BookDetails';
import MyLibrary from './pages/MyLibrary';
import MyShelves from './pages/MyShelves';
import ReadingHistory from './pages/ReadingHistory';
import Profile from './pages/Profile';
import Settings from './pages/Settings';
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
          {/* Signed-out visitors see the marketing landing page; signed-in users get their home dashboard. */}
          <Route path="/" element={isAuthenticated ? <Home /> : <Landing />} />
          <Route path="/recommendations" element={<ProtectedRoute><Recommendations /></ProtectedRoute>} />
          <Route path="/find" element={<ProtectedRoute><FindYourNextRead /></ProtectedRoute>} />
          <Route path="/wrapped" element={<ProtectedRoute><Wrapped /></ProtectedRoute>} />
          <Route path="/search" element={<ProtectedRoute><Search /></ProtectedRoute>} />
          <Route path="/scan" element={<ProtectedRoute><Scan /></ProtectedRoute>} />
          <Route path="/books/:id" element={<ProtectedRoute><BookDetails /></ProtectedRoute>} />
          <Route path="/library" element={<ProtectedRoute><MyLibrary /></ProtectedRoute>} />
          <Route path="/shelves" element={<ProtectedRoute><MyShelves /></ProtectedRoute>} />
          <Route path="/history" element={<ProtectedRoute><ReadingHistory /></ProtectedRoute>} />
          <Route path="/profile/:username" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </>
  );
}
