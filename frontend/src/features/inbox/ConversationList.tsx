import React, { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { conversationsApi } from "../../api/conversationsApi";
import { agentsApi } from "../../api/agentsApi";
import type { Channel, ConversationStatus } from "../../types";
import { Search, RotateCw } from "lucide-react";

interface ConversationListProps {
  selectedId?: string;
  onSelect: (id: string | undefined) => void;
}

export const ConversationList: React.FC<ConversationListProps> = ({
  selectedId,
  onSelect,
}) => {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [channel, setChannel] = useState<Channel | "">("");
  const [status, setStatus] = useState<ConversationStatus | "">("");
  const [assignee, setAssignee] = useState<string>("");
  const [page, setPage] = useState(0);
  const size = 15;

  // Search input debouncer
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0); // Reset page on search change
    }, 400);

    return () => clearTimeout(handler);
  }, [search]);

  // Fetch agents for filter dropdown
  const { data: agents = [] } = useQuery({
    queryKey: ["agents"],
    queryFn: agentsApi.list,
  });

  // Fetch conversations based on state
  const {
    data: convoPage,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: [
      "conversations",
      { page, channel, status, assignee, search: debouncedSearch },
    ],
    queryFn: () =>
      conversationsApi.list({
        page,
        size,
        channel: channel || undefined,
        status: status || undefined,
        assignee: assignee || undefined,
        search: debouncedSearch || undefined,
      }),
  });

  const handleFilterChange = <T extends string>(
    setter: React.Dispatch<React.SetStateAction<T>>,
    val: T,
  ) => {
    setter(val);
    setPage(0); // Reset to first page when changing filters
  };

  const formatTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return (
        date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) +
        " " +
        date.toLocaleDateString([], { month: "2-digit", day: "2-digit" })
      );
    } catch {
      return "";
    }
  };

  const getStatusBadgeClass = (statusVal: ConversationStatus) => {
    switch (statusVal) {
      case "OPEN":
        return "badge-status-open";
      case "PENDING":
        return "badge-status-pending";
      case "RESOLVED":
        return "badge-status-resolved";
      default:
        return "";
    }
  };

  return (
    <div className="pane-left" data-testid="conversation-list-pane">
      <div className="search-filter-box">
        <div className="search-input-wrapper">
          <input
            type="text"
            className="search-input"
            placeholder="Tìm theo tên hoặc địa chỉ..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Search size={16} className="search-icon" />
        </div>

        <div className="filters-row">
          <select
            className="filter-select"
            value={channel}
            onChange={(e) =>
              handleFilterChange(setChannel, e.target.value as Channel | "")
            }
            aria-label="Lọc kênh"
          >
            <option value="">Kênh: Tất cả</option>
            <option value="FACEBOOK">Facebook</option>
            <option value="EMAIL">Email</option>
          </select>

          <select
            className="filter-select"
            value={status}
            onChange={(e) =>
              handleFilterChange(
                setStatus,
                e.target.value as ConversationStatus | "",
              )
            }
            aria-label="Lọc trạng thái"
          >
            <option value="">Trạng thái: Tất cả</option>
            <option value="OPEN">Open</option>
            <option value="PENDING">Pending</option>
            <option value="RESOLVED">Resolved</option>
          </select>

          <select
            className="filter-select"
            value={assignee}
            onChange={(e) => handleFilterChange(setAssignee, e.target.value)}
            aria-label="Lọc người gán"
          >
            <option value="">Gán cho: Tất cả</option>
            <option value="unassigned">Chưa gán</option>
            <option value="me">Gán cho tôi</option>
            {agents.map((a) => (
              <option key={a.id} value={a.id}>
                {a.displayName}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="conversation-list-container">
        {isLoading ? (
          <div className="empty-workspace-state">
            <div className="spinner"></div>
            <p>Đang tải danh sách cuộc hội thoại...</p>
          </div>
        ) : isError ? (
          <div className="empty-workspace-state">
            <p
              className="text-error"
              style={{ color: "var(--status-failed-text)" }}
            >
              Đã xảy ra lỗi khi tải danh sách.
            </p>
            <button
              className="logout-button"
              onClick={() => refetch()}
              style={{ marginTop: "8px" }}
            >
              <RotateCw size={12} /> Thử lại
            </button>
          </div>
        ) : !convoPage || convoPage.content.length === 0 ? (
          <div className="empty-workspace-state">
            <p>Không tìm thấy cuộc hội thoại nào.</p>
          </div>
        ) : (
          convoPage.content.map((item) => (
            <div
              key={item.id}
              className={`conversation-item ${selectedId === item.id ? "active" : ""}`}
              onClick={() => onSelect(item.id)}
              data-testid={`convo-item-${item.id}`}
            >
              <div className="convo-row-top">
                <span className="convo-customer">
                  {item.customerDisplayName}
                </span>
                <span className="convo-time">
                  {formatTime(item.lastActivityAt)}
                </span>
              </div>

              <div className="convo-row-mid">
                <span
                  className={`badge ${item.channel === "FACEBOOK" ? "badge-channel-fb" : "badge-channel-email"}`}
                >
                  {item.channel} • {item.sourceType}
                </span>
                <span className={`badge ${getStatusBadgeClass(item.status)}`}>
                  {item.status}
                </span>
              </div>

              <p className="convo-preview">
                {item.lastMessagePreview || <i>Không có nội dung tin nhắn</i>}
              </p>

              <div className="convo-row-bot">
                <span className="convo-identity">{item.channelIdentity}</span>
                <span className="convo-assignee">
                  {item.assignedAgent
                    ? item.assignedAgent.displayName
                    : "Chưa phân công"}
                </span>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="list-pagination-footer">
        <span className="pagination-info">
          Trang {convoPage ? convoPage.page + 1 : 1} /{" "}
          {convoPage ? convoPage.totalPages || 1 : 1}
        </span>
        <div className="pagination-buttons">
          <button
            className="pagination-btn"
            disabled={page === 0 || isFetching}
            onClick={() => setPage((p) => p - 1)}
          >
            Trước
          </button>
          <button
            className="pagination-btn"
            disabled={
              !convoPage || page >= convoPage.totalPages - 1 || isFetching
            }
            onClick={() => setPage((p) => p + 1)}
          >
            Sau
          </button>
        </div>
      </div>
    </div>
  );
};
