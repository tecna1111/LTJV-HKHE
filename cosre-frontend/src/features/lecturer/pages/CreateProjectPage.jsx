import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import ObjectiveListEditor from "../components/ObjectiveListEditor";
import MilestoneEditor from "../components/MilestoneEditor";
import {
  fetchAssignedSubjects,
  fetchSyllabus,
  generateMilestonesWithAI,
  generateProjectDraftWithAI,
  createProject,
  submitProjectForApproval,
  updateProject,
} from "../api/projectApi";
import { getApiError } from "../../../config/axios";
import "../styles/CreateProjectPage.css";

export default function CreateProjectPage() {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState("");
  const [syllabus, setSyllabus] = useState(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [objectives, setObjectives] = useState([""]);
  const [milestones, setMilestones] = useState([]);

  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState(null);
  const [aiSuggestions, setAiSuggestions] = useState(null);
  const [projectSuggestion, setProjectSuggestion] = useState(null);
  const aiRequestVersion = useRef(0);

  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [savedProject, setSavedProject] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    fetchAssignedSubjects()
      .then(setSubjects)
      .catch(() => setSaveError("Không tải được danh sách môn học."));
  }, []);

  useEffect(() => {
    let cancelled = false;
    if (!selectedSubjectId) {
      return;
    }
    fetchSyllabus(selectedSubjectId)
      .then((data) => {
        if (cancelled) return;
        setSyllabus(data);
        if (objectives.length === 1 && objectives[0] === "" && data.objectives) {
          setObjectives(Array.isArray(data.objectives) ? data.objectives : data.objectives.split('\n').filter(Boolean));
        }
      })
      .catch(() => {
        if (cancelled) return;
        setSyllabus(null);
        setSaveError("Môn học này chưa có đề cương đang hoạt động. Staff cần tạo đề cương trước.");
      });
    return () => { cancelled = true; };
  }, [selectedSubjectId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGenerateMilestonesAI = async () => {
    if (!syllabus) {
      setAiError("Vui lòng chọn môn học trước khi dùng AI gợi ý mốc.");
      return;
    }
    const cleanObjectives = objectives.filter((o) => o.trim() !== "");
    if (cleanObjectives.length === 0) {
      setAiError("Vui lòng nhập ít nhất một mục tiêu trước khi dùng AI.");
      return;
    }
    setAiLoading(true);
    setAiError(null);
    setAiSuggestions(null);
    const version = ++aiRequestVersion.current;
    try {
      const suggested = await generateMilestonesWithAI({
        syllabusId: syllabus.id,
        objectives: cleanObjectives,
      });
      if (version === aiRequestVersion.current) setAiSuggestions(suggested);
    } catch (error) {
      if (version === aiRequestVersion.current) setAiError(getApiError(error, "AI không tạo được mốc lúc này. Vui lòng thử lại hoặc thêm mốc thủ công."));
    } finally {
      if (version === aiRequestVersion.current) setAiLoading(false);
    }
  };

  const handleGenerateProjectAI = async () => {
    if (!syllabus) { setAiError('Vui lòng chọn môn học có đề cương trước khi dùng AI.'); return; }
    setAiLoading(true);
    setAiError(null);
    setProjectSuggestion(null);
    const version = ++aiRequestVersion.current;
    try {
      const draft = await generateProjectDraftWithAI({ syllabusId: syllabus.id, topic: title.trim() });
      if (version === aiRequestVersion.current) setProjectSuggestion(draft);
    } catch (error) {
      if (version === aiRequestVersion.current) setAiError(getApiError(error, 'AI chưa tạo được gợi ý dự án.'));
    } finally {
      if (version === aiRequestVersion.current) setAiLoading(false);
    }
  };

  const buildPayload = () => ({
    title: title.trim(),
    description: description.trim(),
    subjectId: selectedSubjectId,
    syllabusId: syllabus?.id,
    objectives: objectives.filter((o) => o.trim() !== ""),
    milestones,
  });

  const validate = () => {
    if (!title.trim()) return "Vui lòng nhập tên dự án.";
    if (!selectedSubjectId) return "Vui lòng chọn môn học.";
    if (!syllabus) return "Môn học chưa có đề cương đang hoạt động.";
    if (objectives.filter((o) => o.trim() !== "").length === 0)
      return "Vui lòng nhập ít nhất một mục tiêu.";
    if (milestones.length === 0) return "Vui lòng có ít nhất một cột mốc.";
    for (const [index, milestone] of milestones.entries()) {
      if (!milestone.title?.trim()) return `Vui lòng nhập tên cột mốc ${index + 1}.`;
      if (!Number.isInteger(milestone.dueOffsetDays) || milestone.dueOffsetDays < 0)
        return `Số ngày của cột mốc ${index + 1} phải là số nguyên không âm.`;
    }
    return null;
  };

  const saveCurrentDraft = async () => {
    const project = savedProject
      ? await updateProject(savedProject.id, buildPayload())
      : await createProject(buildPayload());
    setSavedProject(project);
    return project;
  };

  const handleSaveDraft = async () => {
    const validationError = validate();
    if (validationError) {
      setSaveError(validationError);
      return;
    }
    setSaving(true);
    setSaveError(null);
    setSuccessMessage(null);
    try {
      await saveCurrentDraft();
      setSuccessMessage("Đã lưu bản nháp thành công.");
    } catch (error) {
      setSaveError(getApiError(error, "Lưu dự án thất bại. Vui lòng kiểm tra lại thông tin và thử lại."));
    } finally {
      setSaving(false);
    }
  };

  const handleSubmitForApproval = async () => {
    const validationError = validate();
    if (validationError) {
      setSaveError(validationError);
      return;
    }
    setSaving(true);
    setSaveError(null);
    setSuccessMessage(null);
    try {
      const project = await saveCurrentDraft();
      const submitted = await submitProjectForApproval(project.id);
      setSavedProject(submitted);
      setSuccessMessage("Đã gửi dự án tới Trưởng bộ môn để phê duyệt.");
    } catch (error) {
      setSaveError(getApiError(error, "Gửi duyệt thất bại. Vui lòng thử lại."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="create-project-page">
      <Link to="/workflow" className="create-project-back-link">
        <ArrowLeft size={18} aria-hidden="true" />
        Quay lại danh sách dự án
      </Link>
      <h1 className="page-title">Tạo dự án mới</h1>
      <p className="page-subtitle">
        Dựa trên đề cương môn học, tạo thông tin dự án và các cột mốc, sau đó
        gửi cho Trưởng bộ môn phê duyệt.
      </p>

      {successMessage && (
        <div className="status-banner">
          <strong>Thành công:</strong> {successMessage}
        </div>
      )}
      {saveError && <div className="error-banner">{saveError}</div>}

      <section className="form-section">
        <label className="field-label">Môn học</label>
        <select
          className="subject-select"
          value={selectedSubjectId}
          onChange={(e) => {
            aiRequestVersion.current += 1;
            setAiLoading(false);
            setAiSuggestions(null);
            setProjectSuggestion(null);
            setAiError(null);
            setSyllabus(null);
            setSelectedSubjectId(e.target.value);
          }}
        >
          <option value="">-- Chọn môn học --</option>
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>
              {s.code} - {s.name}
            </option>
          ))}
        </select>
      </section>

      <section className="form-section">
        <label className="field-label">Tên dự án</label>
        <input
          type="text"
          className="project-title-input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ví dụ: Xây dựng hệ thống quản lý thư viện"
        />
        <button type="button" className="btn-ai-generate" disabled={aiLoading || !syllabus}
          onClick={handleGenerateProjectAI}>
          {aiLoading ? 'AI đang gợi ý...' : '✨ AI gợi ý thông tin dự án'}
        </button>
        {aiError && <p className="ai-error" role="alert">{aiError}</p>}
      </section>

      <section className="form-section">
        <label className="field-label">Mô tả dự án</label>
        <textarea
          className="project-description-input"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Mô tả ngắn gọn phạm vi và bối cảnh của dự án..."
        />
      </section>

      <section className="form-section">
        <ObjectiveListEditor objectives={objectives} onChange={setObjectives} />
      </section>

      <section className="form-section">
        <MilestoneEditor
          milestones={milestones}
          onChange={setMilestones}
          onGenerateAI={handleGenerateMilestonesAI}
          aiLoading={aiLoading}
          aiError={aiError}
        />
      </section>

      {aiSuggestions && (
        <section className="form-section" aria-label="Xem trước đề xuất AI">
          <h2>Xem trước đề xuất AI</h2>
          <p>Bạn có thể chỉnh sửa trước khi áp dụng. Áp dụng sẽ thay danh sách mốc trong form; dự án chỉ được lưu khi bạn bấm Lưu nháp hoặc Gửi duyệt.</p>
          <MilestoneEditor milestones={aiSuggestions} onChange={setAiSuggestions} />
          <div className="action-bar">
            <button type="button" className="btn-secondary" onClick={() => setAiSuggestions(null)}>Hủy đề xuất</button>
            <button type="button" className="btn-primary" onClick={() => {
              setMilestones(aiSuggestions);
              setAiSuggestions(null);
            }}>Áp dụng đề xuất</button>
          </div>
        </section>
      )}

      {projectSuggestion && (
        <section className="form-section" aria-label="Xem trước dự án do AI gợi ý">
          <h2>Xem trước dự án do AI gợi ý</h2>
          <p>Gợi ý chỉ được đưa vào form khi bạn bấm Áp dụng. Bạn có thể sửa mọi thông tin trước khi lưu.</p>
          <h3>{projectSuggestion.title}</h3>
          <p>{projectSuggestion.description}</p>
          <ul>{projectSuggestion.objectives.map((objective, index) => <li key={index}>{objective}</li>)}</ul>
          <ol>{projectSuggestion.milestones.map((milestone, index) =>
            <li key={index}>{milestone.title} — {milestone.dueOffsetDays} ngày</li>)}</ol>
          <div className="action-bar">
            <button type="button" className="btn-secondary" onClick={() => setProjectSuggestion(null)}>Hủy gợi ý</button>
            <button type="button" className="btn-primary" onClick={() => {
              setTitle(projectSuggestion.title);
              setDescription(projectSuggestion.description);
              setObjectives(projectSuggestion.objectives);
              setMilestones(projectSuggestion.milestones);
              setProjectSuggestion(null);
            }}>Áp dụng vào dự án</button>
          </div>
        </section>
      )}

      <div className="action-bar">
        <button
          type="button"
          className="btn-secondary"
          onClick={handleSaveDraft}
          disabled={saving || (savedProject && savedProject.status !== "DRAFT")}
        >
          {saving ? "Đang lưu..." : "Lưu nháp"}
        </button>
        <button
          type="button"
          className="btn-primary"
          onClick={handleSubmitForApproval}
          disabled={saving || (savedProject && savedProject.status !== "DRAFT")}
        >
          {saving ? "Đang gửi..." : "Gửi Trưởng bộ môn duyệt"}
        </button>
      </div>
      {successMessage && (
        <div className="project-success-toast" role="status" aria-live="polite">
          <span>✓</span><div><strong>Thao tác thành công</strong><p>{successMessage}</p></div>
          <button type="button" onClick={() => setSuccessMessage(null)} aria-label="Đóng thông báo">×</button>
        </div>
      )}
    </div>
  );
}
