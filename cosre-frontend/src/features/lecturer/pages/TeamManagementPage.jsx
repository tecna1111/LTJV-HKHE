import React, { useEffect, useState } from "react";
import TeamCard from "../components/TeamCard";
import ContributionTable from "../components/ContributionTable";
import {
  fetchAssignedClasses,
  fetchTeamsByClass,
  fetchTeamContributions,
} from "../api/teamApi";
import "../styles/TeamManagementPage.css";

/**
 * TeamManagementPage
 * Màn hình Giảng viên: chọn lớp -> xem danh sách nhóm & tiến độ -> mở rộng
 * 1 nhóm để xem chi tiết đóng góp từng thành viên.
 */
export default function TeamManagementPage() {
  const [classes, setClasses] = useState([]);
  const [selectedClassId, setSelectedClassId] = useState("");

  const [teams, setTeams] = useState([]);
  const [teamsLoading, setTeamsLoading] = useState(false);
  const [teamsError, setTeamsError] = useState(null);

  const [expandedTeamId, setExpandedTeamId] = useState(null);
  const [contributionsByTeam, setContributionsByTeam] = useState({});
  const [contributionLoading, setContributionLoading] = useState(false);
  const [contributionError, setContributionError] = useState(null);

  // Tải danh sách lớp được phân công khi vào trang
  useEffect(() => {
    fetchAssignedClasses()
      .then((data) => {
        setClasses(data);
        if (data.length > 0) setSelectedClassId(data[0].id);
      })
      .catch(() => setTeamsError("Không tải được danh sách lớp."));
  }, []);

  // Tải danh sách nhóm mỗi khi đổi lớp
  useEffect(() => {
    if (!selectedClassId) {
      setTeams([]);
      return;
    }
    setTeamsLoading(true);
    setTeamsError(null);
    fetchTeamsByClass(selectedClassId)
      .then(setTeams)
      .catch(() => setTeamsError("Không tải được danh sách nhóm của lớp này."))
      .finally(() => setTeamsLoading(false));
  }, [selectedClassId]);

  const handleToggleTeam = async (team) => {
    const isClosing = expandedTeamId === team.id;
    setExpandedTeamId(isClosing ? null : team.id);
    if (isClosing) return;

    // Chỉ gọi API đóng góp nếu chưa từng tải cho nhóm này (cache theo teamId)
    if (contributionsByTeam[team.id]) return;

    setContributionLoading(true);
    setContributionError(null);
    try {
      const data = await fetchTeamContributions(team.id);
      setContributionsByTeam((prev) => ({ ...prev, [team.id]: data }));
    } catch (err) {
      setContributionError("Không tải được dữ liệu đóng góp của nhóm này.");
    } finally {
      setContributionLoading(false);
    }
  };

  return (
    <div className="team-management-page">
      <h1 className="page-title">Quản lý nhóm &amp; theo dõi tiến độ</h1>
      <p className="page-subtitle">
        Theo dõi tiến độ từng nhóm và tỷ lệ đóng góp của từng thành viên.
      </p>

      <div className="class-select-row">
        <label className="field-label" htmlFor="class-select">
          Lớp học
        </label>
        <select
          id="class-select"
          className="class-select"
          value={selectedClassId}
          onChange={(e) => setSelectedClassId(e.target.value)}
        >
          {classes.length === 0 && <option value="">-- Chưa có lớp --</option>}
          {classes.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} - {c.name}
            </option>
          ))}
        </select>
      </div>

      {teamsError && <div className="error-banner">{teamsError}</div>}
      {teamsLoading && <p className="loading-hint">Đang tải danh sách nhóm...</p>}

      {!teamsLoading && teams.length === 0 && !teamsError && (
        <p className="empty-hint">Lớp này chưa có nhóm nào.</p>
      )}

      <div className="team-list">
        {teams.map((team) => (
          <TeamCard
            key={team.id}
            team={team}
            expanded={expandedTeamId === team.id}
            onToggle={() => handleToggleTeam(team)}
          >
            <ContributionTable
              loading={contributionLoading && expandedTeamId === team.id}
              error={expandedTeamId === team.id ? contributionError : null}
              contributions={contributionsByTeam[team.id]}
            />
          </TeamCard>
        ))}
      </div>
    </div>
  );
}
