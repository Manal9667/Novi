import React from 'react';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { useAsync } from '../hooks/useAsync';
import { readingHistoryService } from '../services';
import type { ReadingHistoryEntry } from '../types';

const EVENT_LABELS: Record<string, string> = {
  ADDED_TO_LIBRARY: 'Added to library',
  STARTED_READING: 'Started reading',
  FINISHED_READING: 'Finished reading',
  STATUS_CHANGED: 'Changed status',
  MARKED_DNF: 'Marked as DNF',
  REMOVED_FROM_LIBRARY: 'Removed from library'
};

export default function ReadingHistory() {
  const history = useAsync<ReadingHistoryEntry[]>(
    () => readingHistoryService.get().then((p) => p.content),
    []
  );

  return (
    <div className="page">
      <h1>Reading History</h1>
      <AsyncSection state={history} loadingLabel="Loading your history…">
        {(events) =>
          events.length > 0 ? (
            <ul className="history-list">
              {events.map((event) => (
                <li key={event.id}>
                  <span className="history-date">
                    {new Date(event.occurredAt).toLocaleDateString()}
                  </span>
                  <span>{EVENT_LABELS[event.eventType] || event.eventType}</span>
                  <strong>{event.bookTitle}</strong>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState message="No history yet — add a book to your library to get started." />
          )
        }
      </AsyncSection>
    </div>
  );
}
