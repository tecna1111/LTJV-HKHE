import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  CheckCircle2, Inbox, Info, Lock, RefreshCw,
  Send, Star, Users, X,
} from 'lucide-react';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { getTeams } from '../../team/teamService';
import { getCriteria, getGivenEvaluations, getReceivedEvaluations, submitPeerEvaluation, getRound } from '../evaluationService';
import './PeerEvaluationPage.css';

const STATUS_META = {
  DRAFT: { label: 'Bản nháp', tone: 'draft' },
  SUBMITTED: { label: 'Đã nộp', tone: 'submitted' },
  LOCKED: { label: 'Đã khóa', tone: 'locked' },
};

function initials(name = '') {
  return name.trim().split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase() || '?';
}

function EvaluationModal({ teammate, criteria, existing, onClose, onSubmit, saving }) {
  const [scores, setScores] = useState(() => {
    const map = {};
    criteria.forEach((c) => {
      const detail = existing?.details?.find((d) => d.criteriaId === c.id);
      map[c.id] = detail ? String(detail.score) : '';
    });
    return map;
  });
  const [criteriaComments, setCriteriaComments] = useState(() => {
    const map = {};
    criteria.forEach((c) => {
      const detail = existing?.details?.find((d) => d.criteriaId === c.id);
      map[c.id] = detail?.comment || '';
    });
    return map;
  });
  const [comment, setComment] = useState(existing?.comment || '');
  const [error, setError] = useState('');

  const weightedPreview = useMemo(() => {
    let total = 0;
    let hasAll = criteria.length > 0;
    criteria.forEach((c) => {
      const raw = scores[c.id];
      if (raw === '' || raw === undefined) { hasAll = false; return; }
      const value = Number(raw);
      total += (value / Number(c.maxScore)) * Number(c.weight) * 10;
    });
    return hasAll ? total.toFixed(2) : null;
  }, [scores, criteria]);

  const handleSubmit = (event) => {
    event.preventDefault();
    setError('');
    const details = [];
    for (const c of criteria) {
      const raw = scores[c.id];
      if (raw === '' || raw === undefined) { setError(`Vui lòng chấm điểm cho tiêu chí "${c.title}".`); return; }
      const value = Number(raw);
      if (Number.isNaN(value) || value < 0 || value > Number(c.maxScore)) {
        setError(`Điểm tiêu chí "${c.title}" phải nằm trong khoảng 0 - ${c.maxScore}.`);
        return;
      }
      details.push({ criteriaId: c.id, score: value, comment: criteriaComments[c.id] || undefined });
    }
    onSubmit({ evaluateeId: teammate.id, comment: comment || undefined, details });
  };

  return (
    <div className="pe-modal-backdrop" onMouseDown={onClose}>
      <form className="pe-modal" onSubmit={handleSubmit} onMouseDown={(e) => e.stopPropagation()}>
        <div className="pe-modal-title">
          <div><span>ĐÁNH GIÁ CHÉO</span><h2>{teammate.fullName}</h2><small>@{teammate.username}</small></div>
          <button type="button" onClick={onClose}><X /></button>
        </div>

        {criteria.length === 0 ? (
          <p className="pe-modal-empty"><Info size={16} /> Dự án này chưa có tiêu chí đánh giá. Vui lòng liên hệ giảng viên.</p>
        ) : (
          <div className="pe-criteria-list">
            {criteria.map((c) => (
              <div className="pe-criteria-item" key={c.id}>
                <div className="pe-criteria-head">
                  <div><strong>{c.title}</strong>{c.description && <small>{c.description}</small>}</div>
                  <span>Trọng số {Math.round(Number(c.weight) * 100)}%</span>
                </div>
                <div className="pe-criteria-inputs">
                  <label>
                    Điểm (0 - {c.maxScore})
                    <input
                      type="number" min="0" max={c.maxScore} step="0.5" required
                      value={scores[c.id]}
                      onChange={(e) => setScores((cur) => ({ ...cur, [c.id]: e.target.value }))}
                    />
                  </label>
                  <label>
                    Nhận xét (tùy chọn)
                    <input
                      type="text" maxLength="500" placeholder="Ví dụ: chủ động, đúng deadline…"
                      value={criteriaComments[c.id]}
                      onChange={(e) => setCriteriaComments((cur) => ({ ...cur, [c.id]: e.target.value }))}
                    />
                  </label>
                </div>
              </div>
            ))}
          </div>
        )}

        <label className="pe-overall-comment">
          Nhận xét chung
          <textarea maxLength="1000" placeholder="Nhận xét tổng thể về đóng góp của thành viên…" value={comment} onChange={(e) => setComment(e.target.value)} />
        </label>

        {weightedPreview !== null && (
          <div className="pe-preview"><Star size={16} /> Điểm quy đổi tạm tính: <strong>{weightedPreview}</strong> / 10</div>
        )}
        {error && <div className="pe-error"><X size={15} /> {error}</div>}

        <div className="pe-modal-actions">
          <button type="button" onClick={onClose}>Hủy</button>
          <button className="pe-primary" disabled={saving || criteria.length === 0}>
            {saving ? 'Đang nộp…' : <><Send size={16} /> Nộp đánh giá</>}
          </button>
        </div>
      </form>
    </div>
  );
}

function PeerEvaluationPage() {
  const { fullName, username } = useAuthStore();

  const [params, setParams] = useSearchParams();
  const selectedId = params.get('teamId');
  const [allTeams, setAllTeams] = useState([]);
  const [round, setRound] = useState(null);
  const [loading, setLoading] = useState(true);
  const [team, setTeam] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [given, setGiven] = useState([]);
  const [received, setReceived] = useState([]);
  const [feedback, setFeedback] = useState({ type: '', text: '' });
  const [activeTeammate, setActiveTeammate] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setFeedback({ type: '', text: '' });
    setCriteria([]); setGiven([]); setReceived([]); setRound(null);
    try {
      const teamsResult = await getTeams();
      const teams = teamsResult.data || [];
      setAllTeams(teams.filter(t => t.projectId));
      const myTeam = selectedId ? teams.find(t => String(t.id) === selectedId) : teams.find((t) => t.projectId) || null;
      setRound(null);
      setTeam(myTeam);

      if (myTeam?.projectId) {
        const [criteriaResult, givenResult, receivedResult, roundResult] = await Promise.all([
          getCriteria(myTeam.projectId),
          getGivenEvaluations(myTeam.projectId),
          getReceivedEvaluations(myTeam.projectId),
          getRound(myTeam.id, myTeam.projectId),
        ]);
        setRound(roundResult);
        setCriteria(criteriaResult.data || []);
        setGiven((givenResult.data || []).filter(r => r.teamId === myTeam.id && r.milestoneId == null));
        setReceived((receivedResult.data || []).filter(r => r.teamId === myTeam.id && r.milestoneId == null));
      } else {
        setCriteria([]); setGiven([]); setReceived([]);
      }
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải dữ liệu đánh giá.') });
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { loadAll(); }, [loadAll]);

  const teammates = useMemo(
    () => (team?.members || []).filter((m) => m.username !== username),
    [team, username],
  );

  const givenByEvaluatee = useMemo(() => {
    const map = new Map();
    given.forEach((g) => map.set(g.evaluateeId, g));
    return map;
  }, [given]);

  const receivedAverage = useMemo(() => {
    if (received.length === 0) return null;
    const total = received.reduce((sum, r) => sum + Number(r.totalScore || 0), 0);
    return (total / received.length).toFixed(2);
  }, [received]);

  const openEvaluation = (teammate) => {
    if (!round?.finalOpen || round.locked) { setFeedback({ type: 'error', text: 'Đợt chưa mở hoặc đã khóa.' }); return; }
    setActiveTeammate(teammate);
  };

  const handleSubmit = async (payload) => {
    if (!team) return;
    setSaving(true);
    try {
      await submitPeerEvaluation({
        projectId: team.projectId,
        milestoneId: null,
        teamId: team.id,
        ...payload,
      });
      setFeedback({ type: 'success', text: 'Đã nộp đánh giá thành công.' });
      setActiveTeammate(null);
      await loadAll();
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể nộp đánh giá.') });
    } finally {
      setSaving(false);
    }
  };

  const nameOf = (userId) => teammates.find((m) => m.id === userId)?.fullName
    || (team?.leader?.id === userId ? team.leader.fullName : null)
    || `Người dùng #${userId}`;

  return (
    <DashboardShell role="STUDENT" displayName={fullName || username} activePath="/peer-evaluations" pageTitle="Đánh giá chéo">
        <div className="pe-content">
          <section className="pe-context-panel" aria-label="Nhóm và đợt đánh giá">
            <label>Nhóm đánh giá<select value={team?.id || ''} disabled={loading || saving || !allTeams.length} onChange={e => { setActiveTeammate(null);setParams({ teamId: e.target.value }); }}><option value="" disabled>Chọn nhóm</option>{allTeams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>
            <div className={`pe-round-state ${round?.locked ? 'locked' : round?.finalOpen ? 'open' : 'waiting'}`} role="status"><Info size={20}/><div><strong>{loading ? 'Đang tải đợt đánh giá…' : round?.locked ? 'Đợt đánh giá đã khóa' : round?.finalOpen ? 'Đợt đánh giá đang mở' : 'Chờ giảng viên mở đợt đánh giá'}</strong><p>{round?.locked ? 'Bạn có thể xem lại các đánh giá đã gửi.' : round?.finalOpen ? 'Chọn thành viên bên dưới để gửi đánh giá.' : 'Bạn có thể gửi đánh giá khi giảng viên mở đợt cho nhóm.'}</p></div></div>
            <button className="pe-refresh" onClick={loadAll} disabled={loading || saving} aria-label="Tải lại đánh giá"><RefreshCw size={18}/><span>Tải lại</span></button>
          </section>
          <div className="pe-heading">
            <div>
              <span>ĐÁNH GIÁ CHÉO</span>
              <h1>Đánh giá thành viên nhóm</h1>
              <p>Chấm điểm đóng góp của từng thành viên theo bộ tiêu chí do giảng viên thiết lập.</p>
            </div>
          </div>

          {feedback.text && (
            <div className={`pe-feedback ${feedback.type}`}>
              {feedback.type === 'success' ? <CheckCircle2 size={17} /> : <X size={17} />}{feedback.text}
            </div>
          )}

          {loading ? (
            <div className="pe-empty">Đang tải dữ liệu…</div>
          ) : !team ? (
            <div className="pe-empty"><Users size={36} /><h3>Bạn chưa có nhóm</h3><p>Đánh giá chéo sẽ khả dụng sau khi bạn được xếp vào một nhóm.</p></div>
          ) : !team.projectId ? (
            <div className="pe-empty"><Info size={36} /><h3>Nhóm chưa có đề tài</h3><p>Đánh giá chéo sẽ khả dụng sau khi nhóm được gán đề tài.</p></div>
          ) : (
            <>
              <div className="pe-stats">
                <article><Users /><div><strong>{teammates.length}</strong><span>Thành viên cần đánh giá</span></div></article>
                <article><Send /><div><strong>{given.length}</strong><span>Đã đánh giá</span></div></article>
                <article><Star /><div><strong>{receivedAverage ?? '—'}</strong><span>Điểm trung bình nhận được</span></div></article>
              </div>

              <h2 className="pe-section-title">Thành viên nhóm — {team.name}</h2>
              {criteria.length === 0 && (
                <p className="pe-notice"><Info size={16} /> Giảng viên chưa thiết lập tiêu chí đánh giá cho đề tài này.</p>
              )}
              <div className="pe-teammate-grid">
                {teammates.length === 0 ? (
                  <div className="pe-empty small"><Users size={28} /><p>Nhóm hiện chỉ có mình bạn.</p></div>
                ) : teammates.map((m) => {
                  const existing = givenByEvaluatee.get(m.id);
                  const status = STATUS_META[existing?.status] || null;
                  return (
                    <article className="pe-teammate-card" key={m.id}>
                      <div className="pe-teammate-head">
                        <span>{initials(m.fullName)}</span>
                        <div><strong>{m.fullName}</strong><small>@{m.username}</small></div>
                        {status && <em className={`pe-status pe-status--${status.tone}`}>{status.label}</em>}
                      </div>
                      {existing && <p className="pe-teammate-score">Điểm đã chấm: <strong>{existing.totalScore}</strong>/10</p>}
                      <button
                        className={existing ? 'pe-secondary' : 'pe-primary'}
                        disabled={!round?.finalOpen || round.locked || existing?.status === 'LOCKED' || criteria.length === 0}
                        onClick={() => openEvaluation(m)}
                      >
                        {existing?.status === 'LOCKED' ? <><Lock size={15} /> Đã khóa</> : existing ? 'Sửa đánh giá' : 'Đánh giá'}
                      </button>
                    </article>
                  );
                })}
              </div>

              <h2 className="pe-section-title">Đánh giá tôi nhận được ({received.length})</h2>
              {received.length === 0 ? (
                <div className="pe-empty small"><Inbox size={28} /><p>Chưa có ai đánh giá bạn.</p></div>
              ) : (
                <div className="pe-received-list">
                  {received.map((r) => (
                    <article className="pe-received-card" key={r.id}>
                      <header>
                        <strong>{nameOf(r.evaluatorId)}</strong>
                        <span>{r.totalScore}/10</span>
                      </header>
                      {r.comment && <p>“{r.comment}”</p>}
                      <div className="pe-received-details">
                        {r.details.map((d) => (
                          <span key={d.criteriaId}>{d.criteriaTitle}: <strong>{d.score}</strong>/{d.maxScore}</span>
                        ))}
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </>
          )}
        </div>

      {activeTeammate && (
        <EvaluationModal
          teammate={activeTeammate}
          criteria={criteria}
          existing={givenByEvaluatee.get(activeTeammate.id)}
          saving={saving}
          onClose={() => setActiveTeammate(null)}
          onSubmit={handleSubmit}
        />
      )}
    </DashboardShell>
  );
}

export default PeerEvaluationPage;
