import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ModuleLayout from '../../../components/workspace/ModuleLayout';
import Notice from '../../../components/workspace/Notice';
import useRemote from '../../../components/workspace/useRemote';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getAnswerFeedback, saveAnswerFeedback } from '../evaluationService';
function Feedback({ answerId }) {
  const loader = useCallback(() => getAnswerFeedback(answerId), [answerId]);
  const { data, loading, error, reload } = useRemote(loader);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  const role = useAuthStore(s => s.role);
  async function save(e) {
    e.preventDefault();setBusy(true);setNotice({});
    try { await saveAnswerFeedback(answerId, text.trim()); setNotice({ message: 'Đã lưu phản hồi của bạn.' }); reload(); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể lưu phản hồi. Hãy kiểm tra quyền và trạng thái đợt.') }); } finally { setBusy(false); }
  }
  return <><Notice error={error || notice.error} message={notice.message} />{error && <button onClick={reload}>Thử lại</button>}
    {loading ? <p>Đang tải phản hồi…</p> : !error && <div className="dm-grid"><section className="dm-panel"><h2>Phản hồi câu trả lời #{answerId}</h2>{data?.length === 0 && <p>Chưa có phản hồi.</p>}{data?.map(f => <article className="dm-card" key={f.id}><strong>Thành viên #{f.reviewerId}</strong><p>{f.feedback}</p><small>{new Date(f.updatedAt).toLocaleString('vi-VN')}</small></article>)}</section>
    {role === 'STUDENT' && <form className="dm-panel" onSubmit={save}><h2>Phản hồi của bạn</h2><p className="dm-muted">Chỉ phản hồi câu trả lời của đồng đội. Gửi lại sẽ thay thế phản hồi trước của bạn; đợt đã khóa không cho sửa.</p><fieldset disabled={busy}><label>Nội dung<textarea required maxLength={2000} value={text} onChange={e => setText(e.target.value)} /></label><button className="primary" disabled={!text.trim()}>{busy ? 'Đang lưu…' : 'Lưu phản hồi'}</button></fieldset></form>}</div>}</>;
}
export default function AnswerFeedbackPage() {
  const [params, setParams] = useSearchParams();
  const id = params.get('answerId') || '';
  const [input, setInput] = useState(id);
  const valid = /^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id));
  return <ModuleLayout title="Phản hồi câu trả lời milestone" description="Mở phản hồi bằng mã câu trả lời đã nộp."><form className="dm-panel" onSubmit={e => { e.preventDefault(); setParams({ answerId: input }); }}><label>Mã câu trả lời<input required type="number" min="1" step="1" value={input} onChange={e => setInput(e.target.value)} /></label><button className="primary">Xem phản hồi</button></form>{valid && <Feedback key={id} answerId={id} />}</ModuleLayout>;
}
