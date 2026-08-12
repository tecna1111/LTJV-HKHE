import React from "react";

/**
 * ObjectiveListEditor
 * Cho phép Giảng viên nhập danh sách mục tiêu (objectives) của dự án.
 * Mỗi mục tiêu là một dòng text, có thể thêm dòng mới hoặc xóa dòng.
 */
export default function ObjectiveListEditor({ objectives, onChange }) {
  const handleTextChange = (index, value) => {
    const next = [...objectives];
    next[index] = value;
    onChange(next);
  };

  const handleAdd = () => {
    onChange([...objectives, ""]);
  };

  const handleRemove = (index) => {
    onChange(objectives.filter((_, i) => i !== index));
  };

  return (
    <div className="objective-editor">
      <label className="field-label">Mục tiêu dự án</label>
      {objectives.length === 0 && (
        <p className="empty-hint">Chưa có mục tiêu nào. Thêm ít nhất một mục tiêu.</p>
      )}
      {objectives.map((objective, index) => (
        <div className="objective-row" key={index}>
          <span className="objective-index">{index + 1}.</span>
          <input
            type="text"
            className="objective-input"
            value={objective}
            placeholder="Ví dụ: Sinh viên vận dụng được kiến trúc client-server"
            onChange={(e) => handleTextChange(index, e.target.value)}
          />
          <button
            type="button"
            className="btn-icon-remove"
            onClick={() => handleRemove(index)}
            aria-label="Xóa mục tiêu"
          >
            ✕
          </button>
        </div>
      ))}
      <button type="button" className="btn-add-objective" onClick={handleAdd}>
        + Thêm mục tiêu
      </button>
    </div>
  );
}
