export default function CollaborationStatus({ session }) {
  return <div className="collaboration-status" aria-live="polite">
    <span>{session.connected ? '● Đã kết nối realtime' : '○ Đang kết nối lại · tự đồng bộ mỗi 5 giây'}</span>
    <span>{session.pending ? `${session.pending} thao tác chờ lưu · bản nháp được giữ trong tab này` : session.ready ? `Đã lưu · phiên bản ${session.revision}` : 'Đang tải nội dung…'}</span>
    {session.error && <div role="alert" className="collaboration-error">{session.error}
      <button onClick={session.retry}>Thử lại</button>
      {session.pending > 0 && <><button onClick={session.exportDraft}>Tải bản nháp</button><button onClick={session.discard}>Bỏ bản nháp và tải bản nhóm</button></>}
    </div>}
  </div>;
}
