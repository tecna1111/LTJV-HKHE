import { useMemo, useState } from 'react';
import { Bot, Send, Sparkles, X } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { getApiError } from '../../../config/axios';
import { askAi } from '../aiService';
import './AIChatWidget.css';

const welcome = { role: 'assistant', text: 'Xin chào! Mình là COSRE AI. Bạn cần hỗ trợ ý tưởng, kế hoạch hay nội dung đồ án?' };

export default function AIChatWidget() {
  const location = useLocation();
  const teamId = useMemo(() => location.pathname.match(/^\/teams\/(\d+)/)?.[1] || null, [location.pathname]);
  const [open, setOpen] = useState(false);
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState([welcome]);
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    const value = prompt.trim();
    if (!value || loading) return;
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

  return <div className="ai-widget">
    {open && <section className="ai-widget-panel" aria-label="COSRE AI Chatbot">
      <header><span><Bot size={20} /></span><div><strong>COSRE AI</strong><small>{teamId ? `Đang hỗ trợ nhóm #${teamId}` : 'Trợ lý học tập'}</small></div><button onClick={() => setOpen(false)} aria-label="Đóng chatbot"><X size={18} /></button></header>
      <div className="ai-widget-messages">{messages.map((message, index) => <div key={`${message.role}-${index}`} className={`ai-message ${message.role}`}>{message.text}</div>)}{loading && <div className="ai-message assistant typing">Đang suy nghĩ…</div>}</div>
      <form onSubmit={submit}><textarea aria-label="Câu hỏi cho COSRE AI" maxLength={4000} rows={2} value={prompt} onChange={(event) => setPrompt(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) submit(event); }} placeholder="Nhập câu hỏi…"/><button disabled={!prompt.trim() || loading} aria-label="Gửi câu hỏi"><Send size={17}/></button></form>
    </section>}
    <button className="ai-widget-trigger" onClick={() => setOpen((value) => !value)} aria-label="Mở COSRE AI"><Sparkles size={21}/><span>Hỏi COSRE AI</span></button>
  </div>;
}
