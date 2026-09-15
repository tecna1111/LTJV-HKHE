import { useEffect, useRef, useState } from 'react';
import api, { getApiError } from '../../config/axios';
import useAuthStore from '../../store/useAuthStore';
import { createStompClient } from '../../realtime/createStompClient';
import { projectPending } from './collaborationModel';

export default function useCollaboration(teamId, kind) {
  const username = useAuthStore(s => s.username);
  const control = useRef(null);
  const [view, setView] = useState({ state: {}, revision: 0, ready: false, pending: 0, connected: false, error: '' });
  useEffect(() => {
    let disposed = false, busy = false, connected = false, error = '', denied = false, blocked = false;
    let snapshot = null, queue = [], projection = {}, projectionKey = '';
    const key = `cosre-collaboration:${username}:${teamId}:${kind}`;
    const path = `/collaboration/teams/${teamId}/documents/${kind}`;
    try { queue = JSON.parse(sessionStorage.getItem(key) || '[]'); }
    catch { error = 'Không đọc được bản nháp trong trình duyệt.'; blocked = true; }
    const persist = () => {
      try { sessionStorage.setItem(key, JSON.stringify(queue)); }
      catch { throw new Error('Trình duyệt không đủ dung lượng giữ bản nháp. Hãy tải bản nháp trước khi tiếp tục.'); }
    };
    const emit = () => {
      const nextKey = `${snapshot?.revision}:${denied}:${queue.map(op => op.operationId).join(',')}`;
      if (nextKey !== projectionKey) {
        projection = snapshot && !denied ? projectPending(snapshot, queue) : {};
        projectionKey = nextKey;
      }
      if (!disposed) setView({ state: projection,
        revision: snapshot?.revision || 0, ready: !!snapshot && !denied,
        pending: queue.length, connected, error, blocked });
    };
    const accept = data => { if (!snapshot || data.revision >= snapshot.revision) snapshot = data; };
    const failure = e => {
      error = getApiError(e, e.message || 'Không thể đồng bộ');
      if ([401, 403, 404].includes(e.response?.status)) { denied = true; snapshot = null; }
      if (e.response?.status >= 400 && e.response?.status < 500) blocked = true;
      emit();
    };
    const refresh = async () => {
      try { const response = await api.get(path, { timeout: 15000 }); if (disposed) return;
        accept(response.data.data); denied = false; emit();
      } catch (e) { if (!disposed) failure(e); }
    };
    const pump = async () => {
      if (busy || blocked || disposed || !snapshot || denied) return;
      busy = true;
      try {
        while (queue.length && !disposed) {
          const op = queue[0];
          const response = await api.post(`${path}/operations`, op, { timeout: 15000 });
          if (disposed) return;
          queue.shift(); persist(); accept(response.data.data); error = ''; emit();
        }
      } catch (e) { if (!disposed) failure(e); }
      finally { busy = false; }
    };
    const sync = async () => { await refresh(); await pump(); };
    control.current = {
      enqueue: operation => {
        if (!snapshot || denied || blocked) throw new Error(error || 'Chưa sẵn sàng đồng bộ');
        // Updating a pending canvas object waits for its revision acknowledgement.
        if (kind === 'whiteboard' && queue.some(op => op.payload.id === operation.payload.id))
          throw new Error('Nét vẽ này đang chờ lưu. Hãy đợi xác nhận trước khi sửa tiếp.');
        queue.push(operation);
        try { persist(); } catch (e) { queue.pop(); throw e; }
        emit(); void pump();
      },
      retry: () => { blocked = false; error = ''; void sync(); },
      exportDraft: () => {
        const url = URL.createObjectURL(new Blob([JSON.stringify({ teamId, kind, operations: queue }, null, 2)], { type: 'application/json' }));
        const a = document.createElement('a'); a.href = url; a.download = `cosre-${kind}-${teamId}-draft.json`; a.click(); URL.revokeObjectURL(url);
      },
      discard: () => {
        if (busy) return;
        if (!window.confirm('Bỏ các thao tác chưa lưu và tải bản của nhóm? Bạn có thể tải bản nháp trước.')) return;
        queue = []; persist(); blocked = false; error = ''; void sync();
      },
    };
    const client = createStompClient({
      onConnect: () => {
        if (disposed) return;
        connected = true;
        client.subscribe(`/topic/collaboration/teams/${teamId}/${kind}`, () => { void refresh(); });
        void sync();
      },
      onDisconnect: () => { connected = false; emit(); },
      onError: message => { connected = false; error = message; emit(); },
    });
    client.activate(); void sync();
    // Also reconciles after a missed notification or a broker/server restart.
    const timer = setInterval(() => { void sync(); }, 5000);
    const online = () => { void sync(); };
    window.addEventListener('online', online);
    const beforeUnload = e => { if (queue.length) { e.preventDefault(); e.returnValue = ''; } };
    window.addEventListener('beforeunload', beforeUnload);
    return () => { disposed = true; clearInterval(timer); void client.deactivate();
      window.removeEventListener('online', online); window.removeEventListener('beforeunload', beforeUnload); };
  }, [teamId, kind, username]);
  return { ...view, enqueue: op => control.current?.enqueue(op), retry: () => control.current?.retry(),
    exportDraft: () => control.current?.exportDraft(), discard: () => control.current?.discard() };
}
