import { useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import useCollaboration from '../useCollaboration';
import { textEdit, visibleText } from '../collaborationModel';
import CollaborationStatus from './CollaborationStatus';
import './WhiteboardPage.css';

export default function TextEditorPage() {
  const { id } = useParams();
  const role = useAuthStore(s => s.role), displayName = useAuthStore(s => s.fullName || s.username);
  const session = useCollaboration(id, 'text');
  const document = useMemo(() => visibleText(session.state), [session.state]);
  const [error, setError] = useState('');
  const [composition, setComposition] = useState(null);
  const composing = useRef(false), compositionBase = useRef(null);
  const change = (next, base = document) => {
    try { const op = textEdit(base, next, crypto.randomUUID()); if (op) session.enqueue(op); setError(''); }
    catch (e) { setError(e.message); }
  };
  return <DashboardShell role={role} displayName={displayName} pageTitle="Tài liệu nhóm">
    <main className="whiteboard-page">
      <header><div><Link to={`/teams/${id}/workspace`}>← Workspace</Link><h1>Tài liệu nhóm #{id}</h1>
        <p>Cùng viết ghi chú và báo cáo. Các thay đổi được lưu tự động.</p></div><Link to={`/teams/${id}/whiteboard`}>Mở bảng vẽ</Link></header>
      <CollaborationStatus session={session}/>
      {error && <p role="alert" className="collaboration-error">{error}</p>}
      <textarea className="collaboration-editor" aria-label="Nội dung tài liệu nhóm" spellCheck
        disabled={!session.ready || session.blocked} value={composition ?? document.text}
        onCompositionStart={() => { composing.current = true; compositionBase.current = document; setComposition(document.text); }}
        onCompositionEnd={e => { composing.current = false; change(e.currentTarget.value, compositionBase.current); setComposition(null); }}
        onChange={e => { if (composing.current) setComposition(e.target.value); else change(e.target.value); }}/>
      <footer>{document.text.length} ký tự · Hỗ trợ nhiều người sửa đồng thời. Mỗi lần dán tối đa 4000 ký tự.</footer>
    </main>
  </DashboardShell>;
}
