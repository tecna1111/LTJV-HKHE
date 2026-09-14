import { NavLink } from 'react-router-dom';
import useAuthStore from '../../store/useAuthStore';
import BrandLogo from '../BrandLogo';
import './modules.css';
export default function ModuleLayout({ title, description, children }) {
  const { role, fullName, username } = useAuthStore();
  return <main className="dm-shell"><aside><BrandLogo /><nav aria-label="Chức năng">
    <NavLink to="/dashboard">Tổng quan</NavLink>
    {role === 'LECTURER' && <><NavLink to="/evaluations/criteria">Quản lý tiêu chí</NavLink><NavLink to="/evaluations/summary">Tổng hợp đánh giá chéo</NavLink></>}
    {role === 'STUDENT' && <NavLink to="/peer-evaluations">Đánh giá chéo</NavLink>}
    {['LECTURER','STUDENT'].includes(role) && <><NavLink to="/evaluations/final">Đánh giá cuối dự án</NavLink><NavLink to="/evaluations/feedback">Phản hồi câu trả lời</NavLink></>}
    {role !== 'HEAD_DEPT' && <NavLink to="/resources">Tài nguyên</NavLink>}
    <NavLink to={role === 'ADMIN' ? '/admin/reports' : '/incidents'}>Báo cáo sự cố</NavLink>
  </nav></aside><section className="dm-main"><header><span>COLLABSPHERE</span><strong>{fullName || username}</strong></header>
  <div className="dm-content"><h1>{title}</h1><p className="dm-intro">{description}</p>{children}</div></section></main>;
}
