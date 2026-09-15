import { useEffect, useState } from 'react';
import { getApiError } from '../../config/axios';
export default function useRemote(loader) {
  const [result, setResult] = useState({ data: null, loading: true, error: '' });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    let current = true;
    // Reset state when the selected scope changes; discard responses from the old scope.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setResult({ data: null, loading: true, error: '' });
    Promise.resolve().then(loader).then(data => { if (current) setResult({ data, loading: false, error: '' }); })
      .catch(error => { if (current) setResult({ data: null, loading: false, error: getApiError(error, 'Không tải được dữ liệu. Vui lòng thử lại.') }); });
    return () => { current = false; };
  }, [loader, revision]);
  return { ...result, reload: () => setRevision(n => n + 1) };
}
