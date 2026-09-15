import { useEffect, useMemo, useRef, useState } from 'react';
import { Bot, Minus, Plus, Send, Sparkles, X } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { getApiError } from '../../../config/axios';
import { askAi, getAiHistory } from '../aiService';
import useAuthStore from '../../../store/useAuthStore';
import './AIChatWidget.css';

const welcome = { role: 'assistant', text: 'Xin chào! Mình là COSRE AI. Bạn cần hỗ trợ ý tưởng, kế hoạch hay nội dung đồ án?' };
const MARGIN = 12;
const MIN_WIDTH = 280;
const MIN_HEIGHT = 240;
const clamp = (value, min, max) => Math.min(Math.max(value, min), max);
const initialFrame = () => {
  const width = Math.min(380, window.innerWidth - MARGIN * 2);
  const height = Math.max(Math.min(MIN_HEIGHT, window.innerHeight - MARGIN * 2),
    Math.min(560, window.innerHeight - 120));
  return { left: Math.max(MARGIN, window.innerWidth - width - 24),
    top: Math.max(MARGIN, window.innerHeight - height - 90), width, height };
};

export default function AIChatWidget() {
  const location = useLocation();
  const teamId = useMemo(() => location.pathname.match(/^\/teams\/(\d+)/)?.[1] || null, [location.pathname]);
  const username = useAuthStore(state => state.username);
  return <ChatConversation key={`${username}:${teamId || 'personal'}`} teamId={teamId} />;
}

function ChatConversation({ teamId }) {
  const [open, setOpen] = useState(false);
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState([welcome]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [frame, setFrame] = useState(initialFrame);
  const frameRef = useRef(frame);
  const gestureRef = useRef(null);
  const updateFrame = next => { frameRef.current = next; setFrame(next); };
  const fitFrame = next => {
    const width = clamp(next.width, Math.min(MIN_WIDTH, window.innerWidth - MARGIN * 2), window.innerWidth - MARGIN * 2);
    const height = clamp(next.height, Math.min(MIN_HEIGHT, window.innerHeight - MARGIN * 2), window.innerHeight - MARGIN * 2);
    return { width, height,
      left: clamp(next.left, MARGIN, window.innerWidth - width - MARGIN),
      top: clamp(next.top, MARGIN, window.innerHeight - height - MARGIN) };
  };
  useEffect(() => {
    const onResize = () => updateFrame(fitFrame(frameRef.current));
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);
  const startGesture = (event, mode) => {
    if (mode === 'drag' && event.target.closest('button')) return;
    event.preventDefault();
    gestureRef.current = { mode, x: event.clientX, y: event.clientY, frame: frameRef.current };
    event.currentTarget.setPointerCapture(event.pointerId);
  };
  const moveGesture = event => {
    const gesture = gestureRef.current;
    if (!gesture) return;
    const dx = event.clientX - gesture.x;
    const dy = event.clientY - gesture.y;
    updateFrame(fitFrame(gesture.mode === 'drag'
      ? { ...gesture.frame, left: gesture.frame.left + dx, top: gesture.frame.top + dy }
      : { ...gesture.frame, width: gesture.frame.width + dx, height: gesture.frame.height + dy }));
  };
  const changeSize = amount => updateFrame(fitFrame({ ...frameRef.current,
    width: frameRef.current.width + amount, height: frameRef.current.height + amount }));
  useEffect(() => {
    let active = true;
    getAiHistory(teamId).then(entries => {
      if (active) setMessages([welcome, ...entries.flatMap(entry => [
        { role: 'user', text: entry.prompt }, { role: 'assistant', text: entry.answer },
      ])]);
    }).catch(error => {
      if (active) setMessages([welcome, { role: 'error', text: getApiError(error, 'Không thể tải lịch sử chat.') }]);
    }).finally(() => { if (active) setHistoryLoading(false); });
    return () => { active = false; };
  }, [teamId]);

  const submit = async (event) => {
    event.preventDefault();
    const value = prompt.trim();
    if (!value || loading || historyLoading) return;
    setPrompt('');
    setMessages((current) => [...current, { role: 'user', text: value }]);
    setLoading(true);
    try {
      const response = await askAi(value, teamId ? Number(teamId) : null);
      setMessages((current) => [...current, { role: 'assistant', text: response.answer }]);
    } catch (error) {
      setMessages((current) => [...current, { role: 'error', text: getApiError(error, 'Không thể kết nối COSRE AI lúc này.') }]);
    } finally {
      setLoading(false);
    }
  };

  return <div className="cosre-ai-widget">
    {open && <section className="cosre-ai-widget-panel" aria-label="COSRE AI Chatbot"
      style={{ left: frame.left, top: frame.top, width: frame.width, height: frame.height }}>
      <header onPointerDown={event => startGesture(event, 'drag')} onPointerMove={moveGesture}
        onPointerUp={() => { gestureRef.current = null; }} onPointerCancel={() => { gestureRef.current = null; }}
        title="Kéo để di chuyển COSRE AI"><span><Bot size={20} /></span><div><strong>COSRE AI</strong><small>{teamId ? `Đang hỗ trợ nhóm #${teamId}` : 'Trợ lý học tập'} · Kéo để di chuyển</small></div>
        <button type="button" onClick={() => changeSize(-80)} aria-label="Thu nhỏ khung AI"><Minus size={17} /></button>
        <button type="button" onClick={() => changeSize(80)} aria-label="Phóng to khung AI"><Plus size={17} /></button>
        <button type="button" onClick={() => setOpen(false)} aria-label="Đóng chatbot"><X size={18} /></button></header>
      <div className="cosre-ai-widget-messages">{historyLoading && <div className="cosre-ai-message assistant">Đang tải lịch sử…</div>}{messages.map((message, index) => <div key={`${message.role}-${index}`} className={`cosre-ai-message ${message.role}`}>{message.text}</div>)}{loading && <div className="cosre-ai-message assistant typing">Đang suy nghĩ…</div>}</div>
      <form onSubmit={submit}><textarea aria-label="Câu hỏi cho COSRE AI" maxLength={4000} rows={2} value={prompt} onChange={(event) => setPrompt(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) submit(event); }} placeholder="Nhập câu hỏi…"/><button disabled={!prompt.trim() || loading || historyLoading} aria-label="Gửi câu hỏi"><Send size={17}/></button></form>
      <div className="cosre-ai-widget-resize" role="button" tabIndex={0} aria-label="Kéo để đổi kích thước khung AI"
        onPointerDown={event => startGesture(event, 'resize')} onPointerMove={moveGesture}
        onPointerUp={() => { gestureRef.current = null; }} onPointerCancel={() => { gestureRef.current = null; }}
        onKeyDown={event => { if (event.key === 'ArrowUp' || event.key === 'ArrowLeft') changeSize(-40);
          if (event.key === 'ArrowDown' || event.key === 'ArrowRight') changeSize(40); }} />
    </section>}
    <button className="cosre-ai-widget-trigger" onClick={() => setOpen((value) => !value)} aria-label="Mở COSRE AI"><Sparkles size={21}/><span>Hỏi COSRE AI</span></button>
  </div>;
}
