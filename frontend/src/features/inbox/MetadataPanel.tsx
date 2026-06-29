import React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { conversationsApi } from "../../api/conversationsApi";
import { agentsApi } from "../../api/agentsApi";
import type { ConversationStatus, ActivityView } from "../../types";

interface MetadataPanelProps {
  selectedId?: string;
  onStatusOrAssigneeChange?: () => void;
}

export const MetadataPanel: React.FC<MetadataPanelProps> = ({
  selectedId,
  onStatusOrAssigneeChange,
}) => {
  const queryClient = useQueryClient();

  // Fetch current conversation detail from cache/query
  const { data: convo } = useQuery({
    queryKey: ["conversation", selectedId],
    queryFn: () => (selectedId ? conversationsApi.get(selectedId) : null),
    enabled: !!selectedId,
  });

  // Fetch agents list
  const { data: agents = [] } = useQuery({
    queryKey: ["agents"],
    queryFn: agentsApi.list,
  });

  // Status Change Mutation
  const statusMutation = useMutation({
    mutationFn: (status: ConversationStatus) => {
      if (!selectedId) throw new Error("No conversation selected");
      return conversationsApi.changeStatus(selectedId, status);
    },
    onSuccess: (data) => {
      // Invalidate queries to refresh list and detail views
      queryClient.setQueryData(["conversation", selectedId], data);
      queryClient.invalidateQueries({ queryKey: ["conversations"] });
      if (onStatusOrAssigneeChange) onStatusOrAssigneeChange();
    },
  });

  // Assignee Change Mutation
  const assigneeMutation = useMutation({
    mutationFn: (assignedAgentId: string | null) => {
      if (!selectedId) throw new Error("No conversation selected");
      return conversationsApi.changeAssignee(selectedId, assignedAgentId);
    },
    onSuccess: (data) => {
      // Invalidate queries to refresh list and detail views
      queryClient.setQueryData(["conversation", selectedId], data);
      queryClient.invalidateQueries({ queryKey: ["conversations"] });
      if (onStatusOrAssigneeChange) onStatusOrAssigneeChange();
    },
  });

  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const statusVal = e.target.value as ConversationStatus;
    statusMutation.mutate(statusVal);
  };

  const handleAssigneeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    const assignedId = val === "" ? null : val;
    assigneeMutation.mutate(assignedId);
  };

  const formatActivityText = (act: ActivityView) => {
    const actor = act.actorAgent ? act.actorAgent.displayName : "Hệ thống";
    const oldVal = act.oldValue || "Trống";
    const newVal = act.newValue || "Trống";

    switch (act.activityType) {
      case "STATUS_CHANGED":
        return `${actor} đổi trạng thái từ ${oldVal} sang ${newVal}`;
      case "ASSIGNMENT_CHANGED":
        return oldVal === "Unassigned" || !act.oldValue
          ? `${actor} phân công cho ${newVal}`
          : `${actor} chuyển gán từ ${oldVal} sang ${newVal}`;
      case "MESSAGE_RECEIVED":
        return `Khách hàng gửi tin nhắn mới`;
      case "REPLY_QUEUED":
        return `${actor} đã nháp/gửi tin nhắn mới`;
      case "DELIVERY_STATUS_CHANGED":
        return `Đổi trạng thái tin nhắn từ ${oldVal} sang ${newVal}`;
      default:
        return `Hội thoại có hoạt động mới`;
    }
  };

  const formatTime = (isoString: string) => {
    try {
      const d = new Date(isoString);
      return (
        d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) +
        " " +
        d.toLocaleDateString([], { month: "2-digit", day: "2-digit" })
      );
    } catch {
      return "";
    }
  };

  if (!selectedId || !convo) {
    return (
      <div className="pane-right" data-testid="metadata-panel-pane">
        <div
          className="right-section"
          style={{
            borderBottom: "none",
            color: "var(--color-text-muted)",
            textAlign: "center",
            marginTop: "40px",
          }}
        >
          <i>Chọn cuộc hội thoại để xem thông tin chi tiết.</i>
        </div>
      </div>
    );
  }

  return (
    <div className="pane-right" data-testid="metadata-panel-pane">
      {/* 1. Status and Assignment Control */}
      <div className="right-section">
        <h3 className="right-section-title">Quản lý trạng thái</h3>
        <div className="meta-field-group">
          <div className="meta-field">
            <label className="meta-label" htmlFor="status-select">
              Trạng thái cuộc gọi/inbox
            </label>
            <select
              id="status-select"
              className="control-select"
              value={convo.status}
              onChange={handleStatusChange}
              disabled={statusMutation.isPending}
              data-testid="status-select"
            >
              <option value="OPEN">Open (Mở)</option>
              <option value="PENDING">Pending (Chờ)</option>
              <option value="RESOLVED">Resolved (Đã giải quyết)</option>
            </select>
          </div>

          <div className="meta-field">
            <label className="meta-label" htmlFor="assignee-select">
              Nhân viên xử lý
            </label>
            <select
              id="assignee-select"
              className="control-select"
              value={convo.assignedAgent ? convo.assignedAgent.id : ""}
              onChange={handleAssigneeChange}
              disabled={assigneeMutation.isPending}
              data-testid="assignee-select"
            >
              <option value="">Chưa phân công</option>
              {agents.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.displayName}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* 2. Customer Profile */}
      <div className="right-section">
        <h3 className="right-section-title">Khách hàng</h3>
        <div className="meta-field-group">
          <div className="meta-field">
            <span className="meta-label">Họ và tên</span>
            <span className="meta-value" data-testid="customer-name">
              {convo.customer.displayName}
            </span>
          </div>
          <div className="meta-field">
            <span className="meta-label">ID hệ thống</span>
            <span className="meta-value monospace">{convo.customer.id}</span>
          </div>
        </div>
      </div>

      {/* 3. Channel Identity */}
      <div className="right-section">
        <h3 className="right-section-title">Danh tính kênh truyền</h3>
        <div className="meta-field-group">
          <div className="meta-field">
            <span className="meta-label">Kênh kết nối</span>
            <span className="meta-value monospace">{convo.channel}</span>
          </div>
          <div className="meta-field">
            <span className="meta-label">ID tài khoản Page</span>
            <span className="meta-value monospace">
              {convo.channelIdentity.providerAccountId}
            </span>
          </div>
          <div className="meta-field">
            <span className="meta-label">Định danh khách (Email/PSID)</span>
            <span
              className="meta-value monospace"
              data-testid="channel-identity-id"
            >
              {convo.channelIdentity.externalIdentityId}
            </span>
          </div>
        </div>
      </div>

      {/* 4. Audit Trail Summary */}
      <div className="right-section" style={{ borderBottom: "none" }}>
        <h3 className="right-section-title">Lịch sử hoạt động</h3>
        <div className="activity-list" data-testid="activity-summary-list">
          {convo.activities.length === 0 ? (
            <span className="meta-label">Chưa có lịch sử hoạt động</span>
          ) : (
            convo.activities.map((act) => (
              <div key={act.id} className="activity-item">
                <div className="activity-bullet"></div>
                <div className="activity-content">
                  <span className="activity-text">
                    {formatActivityText(act)}
                  </span>
                  <span className="activity-time">
                    {formatTime(act.createdAt)}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
