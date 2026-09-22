import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { BookCard } from '../components/BookCard';
import type { Page, Shelf } from '../types';

export default function MyShelves() {
  const [shelves, setShelves] = useState<Shelf[]>([]);
  const [newShelfName, setNewShelfName] = useState('');

  function loadShelves() {
    apiClient.get<Page<Shelf>>('/shelves').then((res) => setShelves(res.data.content));
  }

  useEffect(loadShelves, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!newShelfName.trim()) return;
    await apiClient.post('/shelves', { name: newShelfName });
    setNewShelfName('');
    loadShelves();
  }

  async function handleDelete(shelfId: number) {
    await apiClient.delete(`/shelves/${shelfId}`);
    loadShelves();
  }

  return (
    <div className="page">
      <h1>My Shelves</h1>
      <form onSubmit={handleCreate} className="inline-form">
        <input
          placeholder="New shelf name (e.g. Favorites)"
          value={newShelfName}
          onChange={(e) => setNewShelfName(e.target.value)}
        />
        <button type="submit">Create shelf</button>
      </form>

      {shelves.map((shelf) => (
        <section key={shelf.id} className="shelf">
          <div className="shelf-header">
            <h2>{shelf.name}</h2>
            <button className="link-button" onClick={() => handleDelete(shelf.id)}>Delete shelf</button>
          </div>
          <div className="book-grid">
            {shelf.books.map((book) => <BookCard key={book.id} book={book} />)}
          </div>
          {shelf.books.length === 0 && <p className="subtle">No books on this shelf yet.</p>}
        </section>
      ))}
    </div>
  );
}
