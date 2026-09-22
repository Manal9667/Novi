import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import type { Page, ReadingHistoryEntry } from '../types';

const EVENT_LABELS: Record<string, string> = {
  ADDED_TO_LIBRARY: 'Added to library',
  STARTED_READING: 'Started reading',
  FINISHED_READING: 'Finished reading',
  STATUS_CHANGED: 'Changed status',
  MARKED_DNF: 'Marked as DNF',
  REMOVED_FROM_LIBRARY: 'Removed from library'
};

export default function ReadingHistory() {
  const [events, setEvents] = useState<ReadingHistoryEntry[]>([]);

  useEffect(() => {
    apiClient.get<Page<ReadingHistoryEntry>>('/reading-history').then((res) => setEvents(res.data.content));
  }, []);

  return (
    <div className="page">
      <h1>Reading History</h1>
      <ul className="history-list">
        {events.map((event) => (
          <li key={event.id}>
            <span className="history-date">{new Date(event.occurredAt).toLocaleDateString()}</span>
            <span>{EVENT_LABELS[event.eventType] || event.eventType}</span>
            <strong>{event.bookTitle}</strong>
          </li>
        ))}
      </ul>
      {events.length === 0 && <p className="subtle">No history yet - add a book to your library to get started.</p>}
    </div>
  );
}
