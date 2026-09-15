import { useEffect, useRef, useState } from 'react';
import { Canvas, PencilBrush, Path } from 'fabric';
import { Link, useParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import useCollaboration from '../useCollaboration';
import CollaborationStatus from './CollaborationStatus';
import './WhiteboardPage.css';

const COLORS = ['#0f172a', '#2563eb', '#dc2626', '#16a34a', '#9333ea', '#f59e0b'];
export default function WhiteboardPage() {
  const { id } = useParams();
  const role = useAuthStore(s => s.role), displayName = useAuthStore(s => s.fullName || s.username);
  const session = useCollaboration(id, 'whiteboard');
  const live = useRef(session);
  const element = useRef(null), canvas = useRef(null), dragging = useRef(false);
  const [color, setColor] = useState(COLORS[0]), [width, setWidth] = useState(4), [mode, setMode] = useState('draw');
  const [error, setError] = useState(''), [renderTick, setRenderTick] = useState(0);
  useEffect(() => { live.current = session; }, [session]);
  useEffect(() => {
    const board = new Canvas(element.current, { isDrawingMode: true, backgroundColor: '#ffffff', preserveObjectStacking: true });
    board.setDimensions({ width: 1200, height: 650 });
    board.freeDrawingBrush = new PencilBrush(board);
    board.freeDrawingBrush.color = COLORS[0]; board.freeDrawingBrush.width = 4;
    canvas.current = board;
    const put = object => {
      object.collaborationId ||= crypto.randomUUID();
      const objectId = object.collaborationId;
      try {
        live.current.enqueue({ operationId: crypto.randomUUID(), action: 'object.put', payload: {
          id: objectId, expectedVersion: live.current.state[objectId]?.version || 0, value: object.toObject(),
        } }); setError('');
      } catch (e) { setError(e.message); setRenderTick(v => v + 1); }
    };
    board.on('path:created', e => put(e.path));
    board.on('object:modified', e => { if (e.target && e.target.type !== 'activeselection') put(e.target); });
    board.on('mouse:down', () => { dragging.current = true; });
    board.on('mouse:up', () => { dragging.current = false; setRenderTick(v => v + 1); });
    return () => { canvas.current = null; void board.dispose(); };
  }, [id]);
  useEffect(() => {
    const board = canvas.current;
    if (!board) return;
    board.isDrawingMode = session.ready && !session.blocked && mode === 'draw';
    board.selection = false; board.selectionKey = null;
    board.skipTargetFind = !session.ready || session.blocked || mode === 'draw';
    board.freeDrawingBrush.color = color; board.freeDrawingBrush.width = Number(width);
  }, [color, width, mode, session.ready, session.blocked]);
  useEffect(() => {
    const board = canvas.current;
    if (!board || dragging.current) return;
    let cancelled = false;
    const render = async () => {
      const wanted = session.ready ? Object.entries(session.state).filter(([, item]) => !item.deleted) : [];
      const objects = await Promise.all(wanted.map(async ([objectId, item]) => {
        const path = await Path.fromObject(item.value);
        path.collaborationId = objectId; return path;
      }));
      if (cancelled || dragging.current || canvas.current !== board) return;
      const selected = board.getActiveObject()?.collaborationId;
      board.discardActiveObject(); board.remove(...board.getObjects());
      board.add(...objects);
      const active = objects.find(o => o.collaborationId === selected);
      if (active) board.setActiveObject(active);
      board.requestRenderAll();
    };
    void render().catch(e => { if (!cancelled) setError(`Không thể hiển thị bảng: ${e.message}`); });
    return () => { cancelled = true; };
  }, [session.state, session.ready, renderTick]);
  const remove = all => {
    const objects = all ? canvas.current.getObjects() : canvas.current.getActiveObjects();
    if (all && !window.confirm('Xóa các nét vẽ đang hiển thị? Nét mới của người khác sẽ được giữ.')) return;
    for (const object of objects) {
      const objectId = object.collaborationId;
      try { session.enqueue({ operationId: crypto.randomUUID(), action: 'object.delete', payload: {
        id: objectId, expectedVersion: session.state[objectId]?.version || 0,
      } }); setError(''); } catch (e) { setError(e.message); break; }
    }
  };
  const download = () => {
    const a = document.createElement('a'); a.download = `whiteboard-${id}.png`;
    a.href = canvas.current.toDataURL({ format: 'png' }); a.click();
  };
  return <DashboardShell role={role} displayName={displayName} pageTitle="Bảng vẽ nhóm">
    <main className="whiteboard-page">
      <header><div><Link to={`/teams/${id}/workspace`}>← Workspace</Link><h1>Bảng vẽ nhóm #{id}</h1><p>Cùng phác thảo ý tưởng. Mỗi nét được lưu tự động.</p></div>
        <Link to={`/teams/${id}/document`}>Mở tài liệu nhóm</Link></header>
      <CollaborationStatus session={session}/>
      {error && <p role="alert" className="collaboration-error">{error}</p>}
      <section className="whiteboard-toolbar">
        <button onClick={() => setMode('draw')} aria-pressed={mode === 'draw'}>Vẽ</button>
        <button onClick={() => setMode('select')} aria-pressed={mode === 'select'}>Chọn / di chuyển</button>
        <div className="color-list">{COLORS.map(c => <button key={c} aria-label={`Màu ${c}`} className={c === color ? 'active' : ''} style={{ background: c }} onClick={() => setColor(c)}/>)}</div>
        <label>Độ dày <input type="range" min="1" max="24" value={width} onChange={e => setWidth(e.target.value)}/>{width}</label>
        <button disabled={!session.ready || session.blocked} onClick={() => remove(false)}>Xóa nét đã chọn</button>
        <button disabled={!session.ready || session.blocked} onClick={() => remove(true)}>Xóa bảng</button><button onClick={download}>Tải PNG</button>
      </section>
      <div className="canvas-shell"><canvas ref={element}/></div>
      <footer>Hai người sửa cùng nét: thao tác đến sau được giữ trong bản nháp và báo xung đột, không ghi đè bản đã lưu.</footer>
    </main>
  </DashboardShell>;
}
