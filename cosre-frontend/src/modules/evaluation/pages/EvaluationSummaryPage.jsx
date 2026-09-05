import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2, ClipboardList, FolderKanban, Lock, LogOut, RefreshCw, Star, Users, X } from 'lucide-react';
import BrandLogo from '../../../components/BrandLogo';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { getTeams } from '../../team/teamService';
import { getCriteria, getTeamSummary, lockTeamEvaluations } from '../evaluationService';
import './EvaluationSummaryPage.css';

function initials(name = '') {
  return name.trim().split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase() || '?';
}

function EvaluationSummaryPage() {
  const navigate = useNavigate();
  const { fullName, username, clearAuth } = useAuthStore();

  const [teams, setTeams] = useState([]);
  const [selectedTeamId, setSelectedTeamId] = useState(null);
  const [criteria, setCriteria] = useState([]);
  const [summary, setSummary] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingSummary, setLoadingSummary] = useState(false);
  const [locking, setLocking] = useState(false);
  const [feedback, setFeedback] = useState({ type: '', text: '' });

  const projectTeams = useMemo(() => teams.filter((t) => t.projectId), [teams]);
  const selectedTeam = useMemo(() => projectTeams.find((t) => t.id === selectedTeamId) || null, [projectTeams, selectedTeamId]);

  useEffect(() => {
    getTeams()
      .then((result) => {
        const list = (result.data || []).filter((t) => t.projectId);
        setTeams(list);
        if (list.length > 0) setSelectedTeamId(list[0].id);
      })
      .catch((error) => setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải danh sách nhóm.') }))
      .finally(() => setLoading(false));
  }, []);

  const loadSummary = async (team) => {
    if (!team) return;
    setLoadingSummary(true);
    setFeedback({ type: '', text: '' });
    try {
      const memberIds = team.members.map((m) => m.id);
      const [criteriaResult, summaryResult] = await Promise.all([
        getCriteria(team.projectId),
        memberIds.length > 0 ? getTeamSummary(team.id, team.projectId, memberIds) : Promise.resolve({ data: [] }),
      ]);
      setCriteria(criteriaResult.data || []);
      setSummary(summaryResult.data || []);
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể tải bảng tổng hợp điểm.') });
    } finally {
      setLoadingSummary(false);
    }
  };

  useEffect(() => { if (selectedTeam) loadSummary(selectedTeam); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [selectedTeam?.id]);

  const criteriaTitle = (id) => criteria.find((c) => c.id === id)?.title || `Tiêu chí #${id}`;
  const memberName = (studentId) => selectedTeam?.members.find((m) => m.id === studentId)?.fullName || `SV #${studentId}`;

  const isLocked = summary.some((s) => s.receivedEvaluations.some((e) => e.status === 'LOCKED'));

  const handleLock = async () => {
    if (!selectedTeam) return;
    if (!window.confirm(`Khóa toàn bộ đánh giá chéo của nhóm "${selectedTeam.name}"? Sinh viên sẽ không thể sửa lại sau khi khóa.`)) return;
    setLocking(true);
    try {
      await lockTeamEvaluations(selectedTeam.id, selectedTeam.projectId);
      setFeedback({ type: 'success', text: 'Đã khóa đánh giá của nhóm.' });
      await loadSummary(selectedTeam);
    } catch (error) {
      setFeedback({ type: 'error', text: getApiError(error, 'Không thể khóa đánh giá.') });
    } finally {
      setLocking(false);
    }
  };

  return (
    <main className="es-shell">
      <aside className="es-sidebar">
        <BrandLogo />
        <nav>
          <button onClick={() => navigate('/dashboard')}><FolderKanban size={19} /> Tổng quan</button>
          <button className="active"><Star size={19} /> Đánh giá chéo</button>
        </nav>
        <button className="es-logout" onClick={() => { clearAuth(); navigate('/login'); }}><LogOut size={18} /> Đăng xuất</button>
      </aside>

      <section className="es-main">
        <header className="es-topbar">
          <div><small>WORKSPACE / EVALUATION SUMMARY</small><strong>Xin chào, {fullName || username}</strong></div>
          <button onClick={() => loadSummary(selectedTeam)} aria-label="Làm mới"><RefreshCw size={18} /></button>
        </header>

        <div className="es-content">
          <div className="es-heading">
            <div><span>ĐÁNH GIÁ CHÉO</span><h1>Tổng hợp điểm nhóm</h1><p>Xem điểm trung bình sinh viên chấm cho nhau và chốt điểm khi kết thúc đợt đánh giá.</p></div>
            {selectedTeam && (
              <button className="es-lock" disabled={locking || isLocked} onClick={handleLock}>
                <Lock size={16} /> {isLocked ? 'Đã khóa' : locking ? 'Đang khóa…' : 'Khóa đánh giá nhóm'}
              </button>
            )}
          </div>

          {feedback.text && (
            <div className={`es-feedback ${feedback.type}`}>
              {feedback.type === 'success' ? <CheckCircle2 size={17} /> : <X size={17} />}{feedback.text}
            </div>
          )}

          {loading ? (
            <div className="es-empty">Đang tải danh sách nhóm…</div>
          ) : projectTeams.length === 0 ? (
            <div className="es-empty"><Users size={36} /><h3>Chưa có nhóm nào có đề tài</h3><p>Gán đề tài cho nhóm để bắt đầu theo dõi đánh giá chéo.</p></div>
          ) : (
            <>
              <div className="es-team-tabs">
                {projectTeams.map((t) => (
                  <button key={t.id} className={t.id === selectedTeamId ? 'active' : ''} onClick={() => setSelectedTeamId(t.id)}>
                    {t.name}
                  </button>
                ))}
              </div>

              {loadingSummary ? (
                <div className="es-empty">Đang tải bảng điểm…</div>
              ) : summary.length === 0 ? (
                <div className="es-empty"><ClipboardList size={32} /><h3>Chưa có dữ liệu</h3><p>Nhóm chưa có bài đánh giá chéo nào được nộp.</p></div>
              ) : (
                <div className="es-summary-grid">
                  {summary.map((s) => (
                    <article className="es-summary-card" key={s.studentId}>
                      <header>
                        <span>{initials(memberName(s.studentId))}</span>
                        <div><strong>{memberName(s.studentId)}</strong><small>{s.totalReviewsReceived} lượt đánh giá</small></div>
                        <em>{s.averageScore}/10</em>
                      </header>
                      {Object.keys(s.criteriaAverageBreakdown).length > 0 && (
                        <div className="es-breakdown">
                          {Object.entries(s.criteriaAverageBreakdown).map(([criteriaId, avg]) => (
                            <span key={criteriaId}>{criteriaTitle(Number(criteriaId))}: <strong>{avg}</strong></span>
                          ))}
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </section>
    </main>
  );
}

export default EvaluationSummaryPage;
