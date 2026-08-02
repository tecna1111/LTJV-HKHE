import React from "react";

/**
 * ProgressBar
 * Thanh tiến độ dùng chung, tô màu theo mức độ hoàn thành:
 * đỏ (<30%), vàng (30-70%), xanh (>70%).
 */
export default function ProgressBar({ percent }) {
  const clamped = Math.max(0, Math.min(100, percent ?? 0));
  const level = clamped < 30 ? "low" : clamped < 70 ? "mid" : "high";

  return (
    <div className="progress-bar-track" aria-label={`Tiến độ ${clamped}%`}>
      <div
        className={`progress-bar-fill progress-bar-${level}`}
        style={{ width: `${clamped}%` }}
      />
      <span className="progress-bar-label">{clamped}%</span>
    </div>
  );
}
