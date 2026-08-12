import { AlertTriangle, CheckCircle2, Inbox, Mail, MessageSquareWarning, ShieldCheck } from 'lucide-react';
import AdminLayout from '../../../components/admin/AdminLayout';
import './SystemReportsPage.css';

function SystemReportsPage() {
  return (
    <AdminLayout activeTab="reports" title="Báo cáo sự cố">
      <section className="reports-page-heading">
        <div><span>SYSTEM REPORTS</span><h2>Báo cáo sự cố</h2><p>Theo dõi phản ánh của người dùng và trạng thái xử lý sự cố hệ thống.</p></div>
        <div className="reports-channel"><Mail size={17} /><span><strong>Email intake</strong><small>Chờ cấu hình backend</small></span></div>
      </section>

      <section className="report-metric-grid">
        <article><span className="blue"><Inbox size={19} /></span><div><strong>—</strong><p>Tổng báo cáo</p></div></article>
        <article><span className="amber"><AlertTriangle size={19} /></span><div><strong>—</strong><p>Đang chờ xử lý</p></div></article>
        <article><span className="green"><CheckCircle2 size={19} /></span><div><strong>—</strong><p>Đã xử lý</p></div></article>
      </section>

      <section className="reports-panel">
        <header><div><h3>Hộp thư báo cáo hệ thống</h3><p>Báo cáo gửi từ người dùng sẽ xuất hiện tại đây sau khi kết nối dịch vụ email/report.</p></div></header>
        <div className="reports-empty">
          <span><MessageSquareWarning size={27} /></span>
          <h3>Chưa có nguồn dữ liệu báo cáo</h3>
          <p>Frontend đã có màn hình theo thiết kế Admin trong ZIP. Backend hiện chưa cung cấp Report API hoặc cấu hình email nên hệ thống không hiển thị dữ liệu mẫu để tránh nhầm với dữ liệu thật.</p>
          <div><ShieldCheck size={15} /> Cần bổ sung Report API và SMTP trước khi kích hoạt chức năng này.</div>
        </div>
      </section>
    </AdminLayout>
  );
}

export default SystemReportsPage;
