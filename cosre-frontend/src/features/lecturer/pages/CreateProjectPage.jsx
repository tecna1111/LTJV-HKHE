import React, { useEffect, useState } from "react";
import ObjectiveListEditor from "../components/ObjectiveListEditor";
import MilestoneEditor from "../components/MilestoneEditor";
import {
  fetchAssignedSubjects,
  fetchSyllabus,
  generateMilestonesWithAI,
  createProject,
  submitProjectForApproval,
} from "../api/projectApi";
import "../styles/CreateProjectPage.css";

/**
 * CreateProjectPage
 * Màn hình Giảng viên: Tạo dự án dựa trên đề cương môn học, dùng AI
 * để gợi ý cột mốc, rồi lưu nháp hoặc gửi Trưởng bộ môn phê duyệt.
 */
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

  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);
  const [savedProject, setSavedProject] = useState(null);

  // Tải danh sách môn học được phân công khi vào trang
  useEffect(() => {
    fetchAssignedSubjects()
      .then(setSubjects)
      .catch(() => setSaveError("Không tải được danh sách môn học."));
  }, []);

  // Khi chọn môn học, tải đề cương tương ứng để dùng cho AI sinh mốc
  useEffect(() => {
    if (!selectedSubjectId) {
      setSyllabus(null);
      return;
    }
    fetchSyllabus(selectedSubjectId)
      .then((data) => {
        setSyllabus(data);
        // Gợi ý mục tiêu từ đề cương nếu Giảng viên chưa nhập gì
        if (objectives.length === 1 && objectives[0] === "" && data.objectives) {
          setObjectives(data.objectives);
        }
      })
      .catch(() => setSyllabus(null));
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
    try {
      const suggested = await generateMilestonesWithAI({
        syllabusId: syllabus.id,
        objectives: cleanObjectives,
      });
      setMilestones(suggested);
    } catch (err) {
      setAiError("AI không tạo được mốc lúc này. Vui lòng thử lại hoặc thêm mốc thủ công.");
    } finally {
      setAiLoading(false);
    }
  };

  const buildPayload = () => ({
    title: title.trim(),
    description: description.trim(),
    subjectId: selectedSubjectId,
    objectives: objectives.filter((o) => o.trim() !== ""),
    milestones,
  });

  const validate = () => {
    if (!title.trim()) return "Vui lòng nhập tên dự án.";
    if (!selectedSubjectId) return "Vui lòng chọn môn học.";
    if (objectives.filter((o) => o.trim() !== "").length === 0)
      return "Vui lòng nhập ít nhất một mục tiêu.";
    if (milestones.length === 0) return "Vui lòng có ít nhất một cột mốc.";
    return null;
  };

  const handleSaveDraft = async () => {
    const validationError = validate();
    if (validationError) {
      setSaveError(validationError);
      return;
    }
    setSaving(true);
    setSaveError(null);
    try {
      const project = await createProject(buildPayload());
      setSavedProject(project);
    } catch (err) {
      setSaveError("Lưu dự án thất bại. Vui lòng kiểm tra lại thông tin và thử lại.");
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
    try {
      let project = savedProject;
      if (!project) {
        project = await createProject(buildPayload());
      }
      const submitted = await submitProjectForApproval(project.id);
      setSavedProject(submitted);
    } catch (err) {
      setSaveError("Gửi duyệt thất bại. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="create-project-page">
      <h1 className="page-title">Tạo dự án mới</h1>
      <p className="page-subtitle">
        Dựa trên đề cương môn học, tạo thông tin dự án và các cột mốc, sau đó
        gửi cho Trưởng bộ môn phê duyệt.
      </p>

      {savedProject && (
        <div className="status-banner">
          Dự án <strong>{savedProject.title || title}</strong> đã được lưu với
          trạng thái <strong>{savedProject.status}</strong>.
        </div>
      )}
      {saveError && <div className="error-banner">{saveError}</div>}

      <section className="form-section">
        <label className="field-label">Môn học</label>
        <select
          className="subject-select"
          value={selectedSubjectId}
          onChange={(e) => setSelectedSubjectId(e.target.value)}
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

      <div className="action-bar">
        <button
          type="button"
          className="btn-secondary"
          onClick={handleSaveDraft}
          disabled={saving}
        >
          {saving ? "Đang lưu..." : "Lưu nháp"}
        </button>
        <button
          type="button"
          className="btn-primary"
          onClick={handleSubmitForApproval}
          disabled={saving}
        >
          {saving ? "Đang gửi..." : "Gửi Trưởng bộ môn duyệt"}
        </button>
      </div>
    </div>
  );
}
