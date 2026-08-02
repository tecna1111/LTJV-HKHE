import React from "react";
import ProgressBar from "./ProgressBar";

/**
 * ContributionTable
 * Hiển thị tỷ lệ đóng góp (%) và số task hoàn thành của từng thành viên
 * trong 1 nhóm. Dữ liệu lấy từ fetchTeamContributions(teamId).
 */
export default function ContributionTable({ loading, error, contributions }) {
  if (loading) {
    return <p className="contribution-hint">Đang tải dữ liệu đóng góp...</p>;
  }
  if (error) {
    return <p className="contribution-error">{error}</p>;
  }
  if (!contributions || contributions.length === 0) {
    return <p className="contribution-hint">Chưa có dữ liệu đóng góp.</p>;
  }

  return (
    <table className="contribution-table">
      <thead>
        <tr>
          <th>Thành viên</th>
          <th>Đóng góp</th>
          <th>Task hoàn thành</th>
        </tr>
      </thead>
      <tbody>
        {contributions.map((member) => (
          <tr key={member.studentId}>
            <td>{member.studentName}</td>
            <td className="contribution-progress-cell">
              <ProgressBar percent={member.contributionPercent} />
            </td>
            <td>
              {member.completedTasks}/{member.totalTasks}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
