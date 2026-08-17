import { useEffect, useState } from 'react';
import { CheckCircle2, Circle, Flag, Users } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getTeamWorkspace, setMilestoneDone } from '../teamService';
import './TeamWorkspacePage.css';

export default function TeamWorkspacePage(){
  const { id }=useParams(); const role=useAuthStore(s=>s.role); const displayName=useAuthStore(s=>s.fullName||s.username); const username=useAuthStore(s=>s.username);
  const [data,setData]=useState(null); const [error,setError]=useState('');
  const load=()=>getTeamWorkspace(id).then(setData).catch(e=>setError(getApiError(e)));
  useEffect(()=>{load();},[id]); // eslint-disable-line react-hooks/exhaustive-deps
  if(!data)return <DashboardShell role={role} displayName={displayName}><div className="workspace-detail">{error||'Đang tải workspace…'}</div></DashboardShell>;
  const leader=data.team.leader?.username===username;
  const toggle=async(milestone)=>{try{setData(await setMilestoneDone(id,milestone.id,!data.completedMilestoneIds.includes(milestone.id)));}catch(e){setError(getApiError(e));}};
  return <DashboardShell role={role} displayName={displayName} pageTitle="Workspace nhóm"><div className="workspace-detail"><header><span>TEAM WORKSPACE</span><h1>{data.team.name}</h1><p><Users size={15}/> {data.team.members.length} thành viên · {data.project?.title||'Chưa chọn dự án'}</p></header>{error&&<div className="workspace-error">{error}</div>}<section className="progress-box"><div><b>Tiến độ milestone</b><strong>{data.progressPercent}%</strong></div><i><span style={{width:`${data.progressPercent}%`}}/></i></section><section className="milestone-board"><h2><Flag size={19}/> Milestones</h2>{!data.project?<p>Lecturer cần chọn dự án cho nhóm.</p>:data.project.milestones.map(m=>{const done=data.completedMilestoneIds.includes(m.id);return <button key={m.id} className={done?'done':''} disabled={!leader} onClick={()=>toggle(m)}>{done?<CheckCircle2/>:<Circle/>}<div><b>{m.title}</b><p>{m.description||'Không có mô tả'}</p><small>Hạn tương đối: {m.dueOffsetDays} ngày</small></div></button>})}{role==='STUDENT'&&!leader&&<small>Chỉ trưởng nhóm được đánh dấu hoàn thành milestone.</small>}</section></div></DashboardShell>;
}
