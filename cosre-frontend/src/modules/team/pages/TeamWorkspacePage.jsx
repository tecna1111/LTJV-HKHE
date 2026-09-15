import { useEffect, useState } from 'react';
import { CheckCircle2, Circle, Flag, Palette, Users } from 'lucide-react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getTeamWorkspace, setMilestoneDone } from '../teamService';
import './TeamWorkspacePage.css';

export default function TeamWorkspacePage(){
  const { id }=useParams(); const navigate=useNavigate(); const role=useAuthStore(s=>s.role); const displayName=useAuthStore(s=>s.fullName||s.username); const username=useAuthStore(s=>s.username);
  const { hash } = useLocation();
  const [data,setData]=useState(null); const [error,setError]=useState('');
  const load=()=>getTeamWorkspace(id).then(setData).catch(e=>setError(getApiError(e)));
  useEffect(()=>{load();},[id]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (data && hash === '#project-details') {
      const section = document.getElementById('project-details');
      section?.scrollIntoView({ block: 'start' });
      section?.focus({ preventScroll: true });
    }
  }, [data, hash]);
  if(!data)return <DashboardShell role={role} displayName={displayName}><div className="workspace-detail">{error||'Đang tải workspace…'}</div></DashboardShell>;
  const leader=data.team.leader?.username===username;
  const toggle=async(milestone)=>{try{setData(await setMilestoneDone(id,milestone.id,!data.completedMilestoneIds.includes(milestone.id)));}catch(e){setError(getApiError(e));}};
  return <DashboardShell role={role} displayName={displayName} pageTitle="Workspace nhóm"><div className="workspace-detail"><header><span>TEAM WORKSPACE</span><h1>{data.team.name}</h1><p><Users size={15}/> {data.team.members.length} thành viên · {data.project?.title||'Chưa chọn dự án'}</p><button className="open-whiteboard" onClick={()=>navigate(`/teams/${id}/whiteboard`)}><Palette size={17}/> Mở bảng vẽ nhóm</button><button className="open-whiteboard" onClick={()=>navigate(`/teams/${id}/kanban`)}>Mở Kanban và đóng góp</button><button className="open-whiteboard" onClick={()=>navigate(`/teams/${id}/document`)}>Mở tài liệu nhóm</button><button className="open-whiteboard" onClick={()=>navigate(`/teams/${id}/milestone-questions`)}>Câu hỏi cột mốc</button></header>{error&&<div className="workspace-error">{error}</div>}<section id="project-details" className="workspace-project-details" tabIndex={-1} aria-labelledby="project-details-title">
  <h2 id="project-details-title">Đề tài của nhóm</h2>
  {data.project ? <>
    <h3>{data.project.title}</h3>
    <p className="workspace-project-description">{data.project.description || 'Chưa có mô tả đề tài.'}</p>
    <h4>Mục tiêu</h4>
    {data.project.objectives?.length
      ? <ul>{data.project.objectives.map((objective, index) => <li key={index}>{objective}</li>)}</ul>
      : <p>Chưa có mục tiêu.</p>}
  </> : <p>Giảng viên chưa gán đề tài cho nhóm.</p>}
</section><section className="progress-box"><div><b>Tiến độ milestone</b><strong>{data.progressPercent}%</strong></div><i><span style={{width:`${data.progressPercent}%`}}/></i></section><section className="milestone-board"><h2><Flag size={19}/> Milestones</h2>{!data.project?<p>Lecturer cần chọn dự án cho nhóm.</p>:data.project.milestones.map(m=>{const done=data.completedMilestoneIds.includes(m.id);return <button key={m.id} className={done?'done':''} disabled={!leader} onClick={()=>toggle(m)}>{done?<CheckCircle2/>:<Circle/>}<div><b>{m.title}</b><p>{m.description||'Không có mô tả'}</p><small>Hạn tương đối: {m.dueOffsetDays} ngày</small></div></button>})}{role==='STUDENT'&&!leader&&<small>Chỉ trưởng nhóm được đánh dấu hoàn thành milestone.</small>}</section></div></DashboardShell>;
}

