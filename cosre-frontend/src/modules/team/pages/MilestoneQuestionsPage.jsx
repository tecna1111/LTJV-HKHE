import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { getTeamWorkspace } from '../teamService';
import {
  getMilestoneQuestions, createMilestoneQuestion, updateMilestoneQuestion, deleteMilestoneQuestion,
  getMilestoneAnswers, submitMilestoneAnswer, reviewMilestoneAnswer,
  getAnswerPeerFeedback, submitAnswerPeerFeedback,
} from '../milestoneQuestionService';
import './MilestoneQuestionsPage.css';

export default function MilestoneQuestionsPage() {
  const { id } = useParams();
  const role = useAuthStore(state => state.role);
  const username = useAuthStore(state => state.username);
  const [workspace, setWorkspace] = useState(null);
  const [milestoneId, setMilestoneId] = useState('');
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [peerFeedback, setPeerFeedback] = useState({});
  const [peerDrafts, setPeerDrafts] = useState({});
  const [draft, setDraft] = useState('');
  const [answerDrafts, setAnswerDrafts] = useState({});
  const [reviews, setReviews] = useState({});
  const [editing, setEditing] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    getTeamWorkspace(id).then(data => {
      if (!active) return;
      setWorkspace(data);
      setMilestoneId(String(data.project?.milestones?.[0]?.id || ''));
    }).catch(e => { if (active) setError(getApiError(e)); });
    return () => { active = false; };
  }, [id]);

  const reload = async () => {
    if (!milestoneId) return;
    const items = await getMilestoneQuestions(id, milestoneId);
    const entries = await Promise.all(items.map(async q => [q.id, await getMilestoneAnswers(id, q.id)]));
    const feedbackEntries = await Promise.all(entries.flatMap(([, list]) => list).map(async a =>
      [a.id, await getAnswerPeerFeedback(a.id)]));
    setQuestions(items);
    setAnswers(Object.fromEntries(entries));
    setPeerFeedback(Object.fromEntries(feedbackEntries));
  };
  useEffect(() => {
    let active = true;
    if (!milestoneId) return undefined;
    getMilestoneQuestions(id, milestoneId).then(async items => {
      const entries = await Promise.all(items.map(async q => [q.id, await getMilestoneAnswers(id, q.id)]));
      const feedbackEntries = await Promise.all(entries.flatMap(([, list]) => list).map(async a =>
        [a.id, await getAnswerPeerFeedback(a.id)]));
      if (active) { setQuestions(items); setAnswers(Object.fromEntries(entries));
        setPeerFeedback(Object.fromEntries(feedbackEntries)); }
    }).catch(e => { if (active) setError(getApiError(e)); });
    return () => { active = false; };
  }, [id, milestoneId]);

  const action = async callback => {
    setBusy(true); setError('');
    try { await callback(); await reload(); }
    catch (e) { setError(getApiError(e)); }
    finally { setBusy(false); }
  };
  const milestone = workspace?.project?.milestones?.find(m => String(m.id) === milestoneId);
  const myId = workspace?.team?.members?.find(member => member.username === username)?.id;

  return <main className="milestone-questions-page">
    <Link to={`/teams/${id}/workspace`}>← Quay lại workspace</Link>
    <h1>Câu hỏi và câu trả lời cột mốc</h1>
    {error && <p className="milestone-questions-error" role="alert">{error}</p>}
    {!workspace ? <p>Đang tải nhóm…</p> : !workspace.project ? <p>Nhóm chưa có dự án.</p> : <>
      <label>Cột mốc
        <select value={milestoneId} onChange={e => { setMilestoneId(e.target.value); setQuestions([]); setAnswers({}); }}>
          {workspace.project.milestones.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
        </select>
      </label>
      <p>{milestone?.description}</p>
      {role === 'LECTURER' && <form onSubmit={e => { e.preventDefault(); void action(async () => {
        await createMilestoneQuestion(milestoneId, draft); setDraft('');
      }); }}>
        <label>Câu hỏi mới<textarea required maxLength={1000} value={draft} onChange={e => setDraft(e.target.value)} /></label>
        <button disabled={busy || !draft.trim()}>Thêm câu hỏi</button>
      </form>}
      {questions.length === 0 && <p>Chưa có câu hỏi cho cột mốc này.</p>}
      {questions.map(q => { const myAnswer = answers[q.id]?.find(a => a.studentId === myId); return <section className="milestone-question" key={q.id}>
        {editing?.id === q.id ? <form onSubmit={e => { e.preventDefault(); void action(async () => {
          await updateMilestoneQuestion(q.id, editing.text); setEditing(null);
        }); }}>
          <textarea required maxLength={1000} value={editing.text}
            onChange={e => setEditing({ ...editing, text: e.target.value })} />
          <button disabled={busy}>Lưu</button><button type="button" onClick={() => setEditing(null)}>Hủy</button>
        </form> : <h2>{q.questionText}</h2>}
        {role === 'LECTURER' && editing?.id !== q.id && <div className="milestone-question-actions">
          <button disabled={busy || (answers[q.id] || []).length > 0}
            onClick={() => setEditing({ id: q.id, text: q.questionText })}>Sửa</button>
          <button disabled={busy || (answers[q.id] || []).length > 0}
            onClick={() => void action(() => deleteMilestoneQuestion(q.id))}>Xóa</button>
        </div>}
        {role === 'STUDENT' && !myAnswer?.reviewedAt && <form
          onSubmit={e => { e.preventDefault(); void action(async () => {
            await submitMilestoneAnswer(id, q.id, answerDrafts[q.id]);
          }); }}>
          <label>Câu trả lời của bạn<textarea required maxLength={5000}
            value={answerDrafts[q.id] ?? myAnswer?.answerText ?? ''}
            onChange={e => setAnswerDrafts({ ...answerDrafts, [q.id]: e.target.value })} /></label>
          <button disabled={busy || !(answerDrafts[q.id] ?? myAnswer?.answerText ?? '').trim()}>
            {myAnswer ? 'Cập nhật câu trả lời' : 'Nộp câu trả lời'}</button>
        </form>}
        {(answers[q.id] || []).map(a => <article key={a.id}>
          <h3>{workspace.team.members.find(member => member.id === a.studentId)?.fullName || `Sinh viên #${a.studentId}`}</h3><p>{a.answerText}</p>
          {a.score !== null && <p>Điểm: {a.score}/10</p>}
          {a.lecturerFeedback && <p>Phản hồi: {a.lecturerFeedback}</p>}
          {(peerFeedback[a.id] || []).map(item => <p key={item.id}>Đánh giá chéo: {item.feedback}</p>)}
          {role === 'STUDENT' && a.studentId !== myId && <form onSubmit={e => { e.preventDefault();
            void action(() => submitAnswerPeerFeedback(a.id,
              peerDrafts[a.id] ?? peerFeedback[a.id]?.find(item => item.reviewerId === myId)?.feedback ?? '')); }}>
            <label>Đánh giá chéo câu trả lời<textarea required maxLength={2000}
              value={peerDrafts[a.id] ?? peerFeedback[a.id]?.find(item => item.reviewerId === myId)?.feedback ?? ''}
              onChange={e => setPeerDrafts({ ...peerDrafts, [a.id]: e.target.value })} /></label>
            <button disabled={busy || !(peerDrafts[a.id] ?? peerFeedback[a.id]?.find(item => item.reviewerId === myId)?.feedback ?? '').trim()}>Gửi đánh giá chéo</button>
          </form>}
          {role === 'LECTURER' && <form onSubmit={e => { e.preventDefault(); void action(() =>
            reviewMilestoneAnswer(id, a.id, Number(reviews[a.id]?.score ?? a.score ?? 0),
              reviews[a.id]?.feedback ?? a.lecturerFeedback ?? '')); }}>
            <label>Điểm<input required type="number" min="0" max="10" step="0.01"
              value={reviews[a.id]?.score ?? a.score ?? ''}
              onChange={e => setReviews({ ...reviews, [a.id]: { ...reviews[a.id], score: e.target.value } })} /></label>
            <label>Phản hồi<textarea maxLength={2000}
              value={reviews[a.id]?.feedback ?? a.lecturerFeedback ?? ''}
              onChange={e => setReviews({ ...reviews, [a.id]: { ...reviews[a.id], feedback: e.target.value } })} /></label>
            <button disabled={busy}>Lưu đánh giá</button>
          </form>}
        </article>)}
      </section>; })}
    </>}
  </main>;
}
