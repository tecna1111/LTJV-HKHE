import { BookOpenCheck, FolderKanban, Users } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import { getApiError } from '../../../config/axios';
import useAuthStore from '../../../store/useAuthStore';
import { getTeams } from '../teamService';
import './TeamManagementPage.css';

export default function StudentTeamsPage() {
  const navigate = useNavigate();
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getTeams()
      .then((result) => setTeams(result.data || []))
      .catch((exception) => setError(getApiError(exception, 'Không thể tải nhóm của bạn.')))
      .finally(() => setLoading(false));
  }, []);

  return <DashboardShell role="STUDENT" displayName={displayName} activePath="/student/teams" pageTitle="Workspace nhóm">
    <div className="workspace-page">
      <section className="workspace-heading"><div><span>NHÓM CỦA TÔI</span><h1>Workspace nhóm</h1><p>Xem đề tài, thành viên và tiến độ của nhóm bạn.</p></div></section>
      {error && <div className="team-feedback error">{error}</div>}
      {loading ? <div className="team-empty">Đang tải nhóm…</div> : teams.length === 0 ? <div className="team-empty"><Users size={36}/><h3>Bạn chưa thuộc nhóm nào</h3><p>Giảng viên cần thêm bạn vào một nhóm trước.</p></div> : <div className="team-grid">{teams.map((team) => <article className="team-card" key={team.id}>
        <div className="team-card-head"><span>LỚP #{team.classroomId}</span></div>
        <h2>{team.name}</h2><p>{team.description || 'Chưa có mô tả cho nhóm.'}</p>
        <div className={`team-project ${team.projectId ? 'selected' : ''}`}><BookOpenCheck size={17}/>{team.projectId ? `Đề tài #${team.projectId}` : 'Giảng viên chưa gán đề tài'}</div>
        <button className="team-project" onClick={() => navigate(`/teams/${team.id}/workspace`)}><FolderKanban size={17}/> Mở workspace</button>
        <div className="team-members-title"><strong>Thành viên ({team.members.length})</strong></div>
        <div className="team-members">{team.members.map((member) => <div key={member.id}><span>{member.fullName?.slice(0, 1).toUpperCase() || '?'}</span><div><strong>{member.fullName}</strong><small>@{member.username}</small></div></div>)}</div>
      </article>)}</div>}
    </div>
  </DashboardShell>;
}
