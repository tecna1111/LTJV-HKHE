export default function Notice({ error, message }) {
  if (!error && !message) return null;
  return <div className={`dm-notice ${error ? 'error' : 'success'}`} role={error ? 'alert' : 'status'}>{error || message}</div>;
}
