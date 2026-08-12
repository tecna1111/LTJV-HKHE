import React from "react";
import ProgressBar from "./ProgressBar";

/**
 * TeamCard
 * Hiển thị tổng quan 1 nhóm: tên nhóm, dự án, tiến độ, mốc hiện tại.
 * Click vào card để mở/đóng bảng đóng góp thành viên (do component cha quản lý).
 */
export default function TeamCard({ team, expanded, onToggle, children }) {
  return (
    <div className={`team-card ${expanded ? "team-card-expanded" : ""}`}>
      <button
        type="button"
        className="team-card-header"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        <div className="team-card-main">
          <h3 className="team-card-name">{team.name}</h3>
          <p className="team-card-project">{team.projectTitle}</p>
        </div>
        <div className="team-card-meta">
          <span className="team-card-members">{team.memberCount} thành viên</span>
          {team.currentMilestone && (
            <span className="team-card-milestone">
              Đang ở: {team.currentMilestone}
            </span>
          )}
        </div>
        <div className="team-card-progress">
          <ProgressBar percent={team.progressPercent} />
        </div>
        <span className="team-card-chevron">{expanded ? "▲" : "▼"}</span>
      </button>
      {expanded && <div className="team-card-body">{children}</div>}
    </div>
  );
}
