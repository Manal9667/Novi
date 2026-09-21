import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { NavBar } from './components/NavBar';
import { ProtectedRoute } from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Recommendations from './pages/Recommendations';
import Search from './pages/Search';
import Scan from './pages/Scan';
import BookDetails from './pages/BookDetails';
import MyLibrary from './pages/MyLibrary';
import MyShelves from './pages/MyShelves';
import ReadingHistory from './pages/ReadingHistory';
import Profile from './pages/Profile';
import Settings from './pages/Settings';

export default function App() {
  return (
    <>
      <NavBar />
      <main>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
          <Route path="/recommendations" element={<ProtectedRoute><Recommendations /></ProtectedRoute>} />
          <Route path="/search" element={<ProtectedRoute><Search /></ProtectedRoute>} />
          <Route path="/scan" element={<ProtectedRoute><Scan /></ProtectedRoute>} />
          <Route path="/books/:id" element={<ProtectedRoute><BookDetails /></ProtectedRoute>} />
          <Route path="/library" element={<ProtectedRoute><MyLibrary /></ProtectedRoute>} />
          <Route path="/shelves" element={<ProtectedRoute><MyShelves /></ProtectedRoute>} />
          <Route path="/history" element={<ProtectedRoute><ReadingHistory /></ProtectedRoute>} />
          <Route path="/profile/:username" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
          <Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
        </Routes>
      </main>
    </>
  );
}
