import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import ModuleLayout from '../../../components/workspace/ModuleLayout';
import Notice from '../../../components/workspace/Notice';
import useRemote from '../../../components/workspace/useRemote';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getTeams } from '../../team/teamService';
import { getRound, openFinal, lockTeamEvaluations, getFinalGrades, saveFinalGrade } from '../evaluationService';
const loadTeams = () => getTeams().then(r => r.data.filter(t => t.projectId));
function GradeForm({ team, target, existing, onSaved }) {
  const [score, setScore] = useState(existing?.score ?? '');
  const [feedback, setFeedback] = useState(existing?.feedback || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function save(e) {
    e.preventDefault(); setBusy(true); setError('');
    try { await saveFinalGrade({ teamId: team.id, projectId: team.projectId, studentId: target ? Number(target) : null, score: Number(score), feedback }); onSaved(); }
    catch (err) { setError(getApiError(err, 'Không thể lưu điểm.')); } finally { setBusy(false); }
  }
  return <form onSubmit={save}><Notice error={error} /><fieldset disabled={busy}><label>Điểm (0–10)<input required type="number" min="0" max="10" step="0.01" value={score} onChange={e => setScore(e.target.value)} /></label><label>Nhận xét<textarea maxLength={2000} value={feedback} onChange={e => setFeedback(e.target.value)} /></label><button className="primary">{busy ? 'Đang lưu…' : 'Lưu đánh giá'}</button></fieldset></form>;
}
function TeamGrades({ team }) {
  const role = useAuthStore(s => s.role);
  const loader = useCallback(() => Promise.all([getRound(team.id, team.projectId), getFinalGrades(team.id, team.projectId)]), [team.id, team.projectId]);
  const { data, loading, error, reload } = useRemote(loader);
  const [target, setTarget] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState({});
  async function changeRound(lock) {
    if (lock && !window.confirm('Khóa đợt sẽ chặn mọi bài chấm và phản hồi mới, không thể mở lại. Tiếp tục?')) return;
    setBusy(true); setNotice({});
    try { await (lock ? lockTeamEvaluations : openFinal)(team.id, team.projectId); reload(); setNotice({ message: lock ? 'Đã khóa đợt.' : 'Đã mở đánh giá cuối dự án.' }); }
    catch (err) { setNotice({ error: getApiError(err, 'Không thể thay đổi đợt.') }); } finally { setBusy(false); }
  }
  const [round, grades] = data || [null, []];
  const existing = grades.find(g => String(g.studentId ?? '') === target);
  return <><Notice error={error || notice.error} message={notice.message} />{error && <button onClick={reload}>Thử lại</button>}
    {loading ? <p>Đang tải đợt đánh giá…</p> : round && <><section className="dm-panel"><div className="dm-actions"><h2>Trạng thái đợt</h2><span className="dm-badge">{round.locked ? 'Đã khóa' : round.finalOpen ? 'Đang mở' : 'Chưa mở'}</span></div>
    {role === 'LECTURER' && <div className="dm-actions"><button disabled={busy || round.locked || round.finalOpen} onClick={() => changeRound(false)}>Mở đợt cuối dự án</button><button className="danger" disabled={busy || round.locked} onClick={() => changeRound(true)}>Khóa đợt</button></div>}
    {role === 'STUDENT' && <p>{round.locked ? 'Đợt đã kết thúc. Bạn có thể xem kết quả dưới đây.' : round.finalOpen ? <Link to={`/peer-evaluations?teamId=${team.id}`}>Chấm đánh giá chéo cho đồng đội</Link> : 'Giảng viên chưa mở đợt đánh giá.'}</p>}</section>
    <div className="dm-grid"><section className="dm-panel"><h2>Kết quả cuối dự án</h2>{grades.length === 0 && <p>Chưa có điểm được công bố.</p>}
      {grades.map(g => <article key={g.id} className="dm-card"><h3>{g.studentId == null ? 'Điểm nhóm' : team.members.find(m => m.id === g.studentId)?.fullName || `Sinh viên #${g.studentId}`} · {g.score}/10</h3><p>{g.feedback || 'Chưa có nhận xét.'}</p></article>)}</section>
      {role === 'LECTURER' && <section className="dm-panel"><h2>Chấm nhóm / cá nhân</h2><label>Đối tượng<select disabled={busy} value={target} onChange={e => setTarget(e.target.value)}><option value="">Cả nhóm</option>{team.members.map(m => <option key={m.id} value={m.id}>{m.fullName}</option>)}</select></label>
      {round.finalOpen && !round.locked ? <GradeForm key={`${target}:${existing?.updatedAt || ''}`} team={team} target={target} existing={existing} onSaved={() => { setNotice({ message: 'Đã lưu đánh giá.' }); reload(); }} /> : <p>Mở đợt để chấm. Khi đã khóa, điểm chỉ được xem.</p>}</section>}</div></>}
  </>;
}
export default function FinalEvaluationPage() {
  const { data, loading, error, reload } = useRemote(loadTeams);
  const [selected, setSelected] = useState('');
  const team = data?.find(t => String(t.id) === selected);
  return <ModuleLayout title="Đánh giá cuối dự án" description="Theo dõi đợt đánh giá và kết quả nhóm, cá nhân."><Notice error={error} />{error && <button onClick={reload}>Thử lại</button>}
    {loading ? <p>Đang tải nhóm…</p> : <label>Nhóm<select value={selected} onChange={e => setSelected(e.target.value)}><option value="">Chọn nhóm</option>{(data || []).map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>}
    {data?.length === 0 && <p>Chưa có nhóm được giao dự án.</p>}{team && <TeamGrades key={team.id} team={team} />}</ModuleLayout>;
}
