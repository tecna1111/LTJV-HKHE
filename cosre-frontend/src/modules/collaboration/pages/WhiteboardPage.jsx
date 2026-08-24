import { useEffect, useRef, useState } from 'react';
import { Canvas, PencilBrush } from 'fabric';
import { ArrowLeft, Brush, Download, Eraser, MousePointer2, Redo2, RotateCcw, Save, Trash2, Undo2 } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { DashboardShell } from '../../dashboard/pages/DashboardPage';
import useAuthStore from '../../../store/useAuthStore';
import { getApiError } from '../../../config/axios';
import { getWhiteboard, saveWhiteboard } from '../collaborationService';
import './WhiteboardPage.css';

const COLORS = ['#0f172a', '#2563eb', '#dc2626', '#16a34a', '#9333ea', '#f59e0b'];

export default function WhiteboardPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const role = useAuthStore((state) => state.role);
  const displayName = useAuthStore((state) => state.fullName || state.username);
  const elementRef = useRef(null);
  const canvasRef = useRef(null);
  const undoRef = useRef([]);
  const redoRef = useRef([]);
  const restoringRef = useRef(false);
  const [color, setColor] = useState(COLORS[0]);
  const [width, setWidth] = useState(4);
  const [mode, setMode] = useState('draw');
  const [version, setVersion] = useState(0);
  const [status, setStatus] = useState('Đang tải bảng vẽ…');
  const [dirty, setDirty] = useState(false);

  const snapshot = (canvas) => JSON.stringify(canvas.toJSON());
  const remember = (canvas) => {
    if (restoringRef.current) return;
    undoRef.current.push(snapshot(canvas));
    if (undoRef.current.length > 50) undoRef.current.shift();
    redoRef.current = [];
    setDirty(true);
  };

  useEffect(() => {
    const canvas = new Canvas(elementRef.current, { isDrawingMode: true, backgroundColor: '#ffffff', preserveObjectStacking: true });
    canvas.setDimensions({ width: Math.max(760, Math.min(1280, window.innerWidth - 330)), height: 620 });
    canvas.freeDrawingBrush = new PencilBrush(canvas);
    canvas.freeDrawingBrush.color = COLORS[0];
    canvas.freeDrawingBrush.width = 4;
    canvas.on('path:created', () => remember(canvas));
    canvas.on('object:modified', () => remember(canvas));
    canvas.on('object:removed', () => remember(canvas));
    canvasRef.current = canvas;

    getWhiteboard(id).then(async (data) => {
      restoringRef.current = true;
      await canvas.loadFromJSON(data.canvasData);
      canvas.backgroundColor = '#ffffff';
      canvas.renderAll();
      setVersion(data.version);
      undoRef.current = [snapshot(canvas)];
      setStatus(data.updatedAt ? `Đã tải phiên bản ${data.version}` : 'Bảng vẽ mới');
      setDirty(false);
      restoringRef.current = false;
    }).catch((error) => setStatus(getApiError(error, 'Không thể tải bảng vẽ.')));

    return () => canvas.dispose();
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas?.freeDrawingBrush) return;
    canvas.freeDrawingBrush.color = color;
    canvas.freeDrawingBrush.width = Number(width);
  }, [color, width]);

  useEffect(() => {
    if (canvasRef.current) canvasRef.current.isDrawingMode = mode === 'draw';
  }, [mode]);

  const restore = async (json) => {
    const canvas = canvasRef.current;
    restoringRef.current = true;
    await canvas.loadFromJSON(json);
    canvas.backgroundColor = '#ffffff';
    canvas.renderAll();
    restoringRef.current = false;
    setDirty(true);
  };

  const undo = async () => {
    if (undoRef.current.length <= 1) return;
    redoRef.current.push(undoRef.current.pop());
    await restore(undoRef.current.at(-1));
  };
  const redo = async () => {
    const next = redoRef.current.pop();
    if (!next) return;
    undoRef.current.push(next);
    await restore(next);
  };
  const eraseSelection = () => {
    const canvas = canvasRef.current;
    const selected = canvas.getActiveObjects();
    if (!selected.length) return;
    selected.forEach((object) => canvas.remove(object));
    canvas.discardActiveObject();
    canvas.renderAll();
  };
  const clear = () => {
    const canvas = canvasRef.current;
    if (!window.confirm('Xóa toàn bộ nội dung trên bảng vẽ?')) return;
    canvas.getObjects().forEach((object) => canvas.remove(object));
    canvas.renderAll();
  };
  const save = async () => {
    const canvas = canvasRef.current;
    setStatus('Đang lưu…');
    try {
      const data = await saveWhiteboard(id, snapshot(canvas), version);
      setVersion(data.version);
      setDirty(false);
      setStatus(`Đã lưu phiên bản ${data.version}`);
    } catch (error) {
      setStatus(error.response?.status === 409 ? 'Bảng đã được cập nhật ở nơi khác. Hãy tải lại trang.' : getApiError(error, 'Lưu bảng vẽ thất bại.'));
    }
  };
  const download = () => {
    const link = document.createElement('a');
    link.download = `cosre-whiteboard-team-${id}.png`;
    link.href = canvasRef.current.toDataURL({ format: 'png', multiplier: 2 });
    link.click();
  };

  return <DashboardShell role={role} displayName={displayName} pageTitle="Bảng vẽ nhóm" activePath="">
    <div className="whiteboard-page">
      <header><div><button className="whiteboard-back" onClick={() => navigate(`/teams/${id}/workspace`)}><ArrowLeft size={17}/> Workspace</button><span>COLLABORATION</span><h1>Bảng vẽ nhóm #{id}</h1><p>Phác thảo ý tưởng, sơ đồ và kế hoạch trực quan.</p></div><div className={dirty ? 'save-state dirty' : 'save-state'}>{dirty ? 'Có thay đổi chưa lưu' : status}</div></header>
      <section className="whiteboard-toolbar">
        <div className="mode-buttons"><button className={mode === 'draw' ? 'active' : ''} onClick={() => setMode('draw')}><Brush size={17}/> Vẽ</button><button className={mode === 'select' ? 'active' : ''} onClick={() => setMode('select')}><MousePointer2 size={17}/> Chọn</button></div>
        <div className="color-list">{COLORS.map((item) => <button key={item} aria-label={`Màu ${item}`} className={color === item ? 'active' : ''} style={{background:item}} onClick={() => setColor(item)}/>)}</div>
        <label>Độ dày <input type="range" min="1" max="24" value={width} onChange={(event) => setWidth(event.target.value)}/><b>{width}px</b></label>
        <div className="toolbar-actions"><button onClick={undo} title="Hoàn tác"><Undo2 size={17}/></button><button onClick={redo} title="Làm lại"><Redo2 size={17}/></button><button onClick={eraseSelection} title="Xóa đối tượng đã chọn"><Eraser size={17}/></button><button onClick={clear} title="Xóa toàn bộ"><Trash2 size={17}/></button><button onClick={download} title="Tải PNG"><Download size={17}/></button><button className="save-button" onClick={save}><Save size={17}/> Lưu bảng</button></div>
      </section>
      <div className="canvas-shell"><canvas ref={elementRef}/></div>
      <footer><RotateCcw size={15}/> Chọn nét vẽ để di chuyển; dùng nút tẩy để xóa đối tượng đang chọn.</footer>
    </div>
  </DashboardShell>;
}
