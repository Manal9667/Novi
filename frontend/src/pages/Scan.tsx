import React, { useMemo, useState } from 'react';
import { AxiosError } from 'axios';
import { apiClient } from '../api/client';
import type {
  ConfirmScanResponse,
  ReadingStatus,
  ScanCandidate,
  ScanResult
} from '../types';

type Mode = 'book' | 'shelf';

const LOW_CONFIDENCE = 0.55;

interface Selection {
  include: boolean;
  status: ReadingStatus;
}

export default function Scan() {
  const [mode, setMode] = useState<Mode>('shelf');
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<ScanResult | null>(null);
  const [selections, setSelections] = useState<Record<number, Selection>>({});
  const [confirming, setConfirming] = useState(false);
  const [summary, setSummary] = useState<ConfirmScanResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const matchedCandidates = useMemo(
    () => (result?.candidates ?? []).filter((c) => c.matchedBook),
    [result]
  );

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    setPreview(selected ? URL.createObjectURL(selected) : null);
    setResult(null);
    setSummary(null);
    setError(null);
  }

  async function handleScan(e: React.FormEvent) {
    e.preventDefault();
    if (!file) return;
    setScanning(true);
    setError(null);
    setSummary(null);
    try {
      const form = new FormData();
      form.append('image', file);
      const endpoint = mode === 'book' ? '/scan/book' : '/scan/shelf';
      const { data } = await apiClient.post<ScanResult>(endpoint, form);
      setResult(data);
      // Default: pre-select every confidently matched book as "Want to Read".
      const defaults: Record<number, Selection> = {};
      data.candidates.forEach((c) => {
        defaults[c.id] = {
          include: !!c.matchedBook && c.confidence >= LOW_CONFIDENCE,
          status: 'WANT_TO_READ'
        };
      });
      setSelections(defaults);
    } catch (err) {
      const ax = err as AxiosError<{ message?: string }>;
      if (ax.response?.status === 503) {
        setError(
          'The scanner needs a vision model to be configured on the server (ANTHROPIC_API_KEY). It looks like it is not enabled.'
        );
      } else {
        setError(ax.response?.data?.message ?? 'Scan failed. Please try another photo.');
      }
    } finally {
      setScanning(false);
    }
  }

  function toggleInclude(candidate: ScanCandidate) {
    if (!candidate.matchedBook) return;
    setSelections((prev) => ({
      ...prev,
      [candidate.id]: { ...prev[candidate.id], include: !prev[candidate.id]?.include }
    }));
  }

  function setStatus(candidateId: number, status: ReadingStatus) {
    setSelections((prev) => ({ ...prev, [candidateId]: { ...prev[candidateId], status } }));
  }

  function selectAll(include: boolean) {
    setSelections((prev) => {
      const next = { ...prev };
      matchedCandidates.forEach((c) => {
        next[c.id] = { ...next[c.id], include };
      });
      return next;
    });
  }

  async function handleConfirm() {
    if (!result) return;
    setConfirming(true);
    setError(null);
    try {
      const decisions = result.candidates
        .filter((c) => c.matchedBook) // unmatched can't be added without a manual choice
        .map((c) => ({
          candidateId: c.id,
          confirm: !!selections[c.id]?.include,
          status: selections[c.id]?.status ?? 'WANT_TO_READ'
        }));
      const { data } = await apiClient.post<ConfirmScanResponse>(
        `/scan/sessions/${result.sessionId}/confirm`,
        { decisions }
      );
      setSummary(data);
      setResult(null);
    } catch (err) {
      const ax = err as AxiosError<{ message?: string }>;
      setError(ax.response?.data?.message ?? 'Could not add the selected books.');
    } finally {
      setConfirming(false);
    }
  }

  const selectedCount = matchedCandidates.filter((c) => selections[c.id]?.include).length;

  return (
    <div className="page">
      <h1>Scan your books</h1>
      <p className="subtle">
        Snap a photo of a single book or a whole shelf and Novi will identify the books so you can add
        them to your library in one go. Nothing is added until you confirm.
      </p>

      <div className="filter-bar">
        <button className={mode === 'shelf' ? 'active' : ''} onClick={() => setMode('shelf')}>
          Bookshelf
        </button>
        <button className={mode === 'book' ? 'active' : ''} onClick={() => setMode('book')}>
          Single book
        </button>
      </div>

      <form onSubmit={handleScan} className="scan-form">
        <input type="file" accept="image/*" capture="environment" onChange={handleFileChange} />
        <button type="submit" disabled={!file || scanning}>
          {scanning ? 'Scanning…' : mode === 'book' ? 'Scan book' : 'Scan shelf'}
        </button>
      </form>

      {preview && (
        <div className="scan-preview">
          <img src={preview} alt="Selected upload preview" />
        </div>
      )}

      {error && <p className="form-error">{error}</p>}

      {summary && (
        <div className="scan-summary">
          <h2>Done!</h2>
          <p>
            Added <strong>{summary.addedCount}</strong> book{summary.addedCount === 1 ? '' : 's'} to your
            library
            {summary.alreadyInLibraryCount > 0 && ` (${summary.alreadyInLibraryCount} already there)`}
            {summary.skippedCount > 0 && `, skipped ${summary.skippedCount}`}.
          </p>
        </div>
      )}

      {result && (
        <section className="scan-results">
          <div className="section-header">
            <h2>{result.detectedCount} book{result.detectedCount === 1 ? '' : 's'} detected</h2>
            {matchedCandidates.length > 0 && (
              <div className="scan-bulk">
                <button className="secondary" onClick={() => selectAll(true)}>Select all</button>
                <button className="secondary" onClick={() => selectAll(false)}>Clear all</button>
              </div>
            )}
          </div>

          {result.candidates.length === 0 && (
            <p className="subtle">No readable books were found. Try a clearer, closer photo.</p>
          )}

          <ul className="scan-candidate-list">
            {result.candidates.map((c) => {
              const low = c.confidence < LOW_CONFIDENCE;
              const sel = selections[c.id];
              return (
                <li key={c.id} className="scan-candidate">
                  <div className="scan-cover">
                    {c.matchedBook?.coverImageUrl ? (
                      <img src={c.matchedBook.coverImageUrl} alt={c.matchedBook.title} />
                    ) : (
                      <div className="book-cover-placeholder">
                        {(c.matchedBook?.title ?? c.detectedTitle ?? '?')[0]}
                      </div>
                    )}
                  </div>
                  <div className="scan-candidate-body">
                    {c.matchedBook ? (
                      <>
                        <strong>{c.matchedBook.title}</strong>
                        <p className="subtle">{c.matchedBook.authorNames.join(', ')}</p>
                      </>
                    ) : (
                      <>
                        <strong>{c.detectedTitle}</strong>
                        {c.detectedAuthor && <p className="subtle">{c.detectedAuthor}</p>}
                        <p className="form-error">No confident catalog match — skipped.</p>
                      </>
                    )}

                    <div className="scan-confidence">
                      <span>{low ? 'Possible match' : 'Match'}: {Math.round(c.confidence * 100)}%</span>
                      <div className="bar-track">
                        <div
                          className="bar-fill"
                          style={{ width: `${Math.round(c.confidence * 100)}%` }}
                        />
                      </div>
                    </div>

                    {c.matchedBook && (
                      <div className="scan-actions">
                        <label className="scan-include">
                          <input
                            type="checkbox"
                            checked={!!sel?.include}
                            onChange={() => toggleInclude(c)}
                          />
                          Add to library
                        </label>
                        <select
                          value={sel?.status ?? 'WANT_TO_READ'}
                          disabled={!sel?.include}
                          onChange={(e) => setStatus(c.id, e.target.value as ReadingStatus)}
                        >
                          <option value="WANT_TO_READ">Want to read</option>
                          <option value="CURRENTLY_READING">Currently reading</option>
                          <option value="READ">Read</option>
                          <option value="DNF">Did not finish</option>
                        </select>
                      </div>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>

          {matchedCandidates.length > 0 && (
            <button onClick={handleConfirm} disabled={confirming || selectedCount === 0}>
              {confirming
                ? 'Adding…'
                : `Add ${selectedCount} selected book${selectedCount === 1 ? '' : 's'} to my library`}
            </button>
          )}
        </section>
      )}
    </div>
  );
}
