import { useEffect, useMemo, useState } from 'react';
import { Bot, Send, Sparkles, X } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { getApiError } from '../../../config/axios';
import { askAi, getAiHistory } from '../aiService';
import useAuthStore from '../../../store/useAuthStore';
import './AIChatWidget.css';

const welcome = { role: 'assistant', text: 'Xin chào! Mình là COSRE AI. Bạn cần hỗ trợ ý tưởng, kế hoạch hay nội dung đồ án?' };

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
    {open && <section className="cosre-ai-widget-panel" aria-label="COSRE AI Chatbot">
      <header><span><Bot size={20} /></span><div><strong>COSRE AI</strong><small>{teamId ? `Đang hỗ trợ nhóm #${teamId}` : 'Trợ lý học tập'}</small></div><button onClick={() => setOpen(false)} aria-label="Đóng chatbot"><X size={18} /></button></header>
      <div className="cosre-ai-widget-messages">{historyLoading && <div className="cosre-ai-message assistant">Đang tải lịch sử…</div>}{messages.map((message, index) => <div key={`${message.role}-${index}`} className={`cosre-ai-message ${message.role}`}>{message.text}</div>)}{loading && <div className="cosre-ai-message assistant typing">Đang suy nghĩ…</div>}</div>
      <form onSubmit={submit}><textarea aria-label="Câu hỏi cho COSRE AI" maxLength={4000} rows={2} value={prompt} onChange={(event) => setPrompt(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) submit(event); }} placeholder="Nhập câu hỏi…"/><button disabled={!prompt.trim() || loading || historyLoading} aria-label="Gửi câu hỏi"><Send size={17}/></button></form>
    </section>}
    <button className="cosre-ai-widget-trigger" onClick={() => setOpen((value) => !value)} aria-label="Mở COSRE AI"><Sparkles size={21}/><span>Hỏi COSRE AI</span></button>
  </div>;
}
