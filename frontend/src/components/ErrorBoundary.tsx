import React from 'react';

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
}

/**
 * Catches render-time exceptions anywhere below it so a single broken component
 * shows a recoverable fallback instead of a blank white screen. Important for a
 * multi-user deployment where one unexpected data shape shouldn't take down the
 * whole session.
 */
export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: unknown) {
    // Log for diagnostics; never surface raw error details to the user.
    // eslint-disable-next-line no-console
    console.error('Unhandled UI error:', error);
  }

  handleReset = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="page">
          <div className="error-state" role="alert">
            <h1>Something went wrong</h1>
            <p className="subtle">
              An unexpected error interrupted this page. You can try reloading, or head back home.
            </p>
            <div className="error-actions">
              <button type="button" onClick={() => window.location.reload()}>
                Reload
              </button>
              <a className="secondary-link" href="/" onClick={this.handleReset}>
                Go home
              </a>
            </div>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
