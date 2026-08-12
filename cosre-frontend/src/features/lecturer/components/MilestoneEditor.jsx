import React from "react";

/**
 * MilestoneEditor
 * Hiển thị danh sách mốc dự án (milestones). Mốc có thể do AI gợi ý
 * hoặc Giảng viên tự thêm/sửa/xóa tay trước khi lưu.
 *
 * props:
 *  - milestones: [{ title, description, dueOffsetDays }]
 *  - onChange: (nextMilestones) => void
 *  - onGenerateAI: () => Promise<void>   // gọi AI sinh mốc dựa trên đề cương
 *  - aiLoading: boolean
 *  - aiError: string | null
 */
export default function MilestoneEditor({
  milestones,
  onChange,
  onGenerateAI,
  aiLoading,
  aiError,
}) {
  const updateField = (index, field, value) => {
    const next = [...milestones];
    next[index] = { ...next[index], [field]: value };
    onChange(next);
  };

  const handleAdd = () => {
    onChange([
      ...milestones,
      { title: "", description: "", dueOffsetDays: 7 },
    ]);
  };

  const handleRemove = (index) => {
    onChange(milestones.filter((_, i) => i !== index));
  };

  return (
    <div className="milestone-editor">
      <div className="milestone-editor-header">
        <label className="field-label">Cột mốc dự án (Milestones)</label>
        <button
          type="button"
          className="btn-ai-generate"
          onClick={onGenerateAI}
          disabled={aiLoading}
        >
          {aiLoading ? "AI đang tạo mốc..." : "✨ Tạo mốc bằng AI"}
        </button>
      </div>

      {aiError && <p className="ai-error">{aiError}</p>}

      {milestones.length === 0 && !aiLoading && (
        <p className="empty-hint">
          Chưa có cột mốc nào. Bạn có thể dùng AI để gợi ý dựa trên đề cương,
          hoặc thêm mốc thủ công bên dưới.
        </p>
      )}

      <ol className="milestone-list">
        {milestones.map((milestone, index) => (
          <li className="milestone-card" key={index}>
            <div className="milestone-card-row">
              <input
                type="text"
                className="milestone-title-input"
                placeholder="Tên mốc, ví dụ: Hoàn thành phân tích yêu cầu"
                value={milestone.title}
                onChange={(e) => updateField(index, "title", e.target.value)}
              />
              <div className="milestone-due">
                <input
                  type="number"
                  min={1}
                  className="milestone-due-input"
                  value={milestone.dueOffsetDays}
                  onChange={(e) =>
                    updateField(
                      index,
                      "dueOffsetDays",
                      Number(e.target.value)
                    )
                  }
                />
                <span className="milestone-due-label">ngày kể từ khi giao dự án</span>
              </div>
              <button
                type="button"
                className="btn-icon-remove"
                onClick={() => handleRemove(index)}
                aria-label="Xóa mốc"
              >
                ✕
              </button>
            </div>
            <textarea
              className="milestone-description-input"
              placeholder="Mô tả chi tiết mốc này..."
              value={milestone.description}
              onChange={(e) =>
                updateField(index, "description", e.target.value)
              }
            />
          </li>
        ))}
      </ol>

      <button type="button" className="btn-add-milestone" onClick={handleAdd}>
        + Thêm mốc thủ công
      </button>
    </div>
  );
}
