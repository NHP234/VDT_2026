import React, { useState, useEffect, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { conversationsApi } from "../../api/conversationsApi";
import type { MessageView, ActivityView } from "../../types";
import { Send, AlertCircle, MessageSquare } from "lucide-react";
import { HttpError } from "../../api/httpClient";

interface ConversationDetailProps {
  selectedId?: string;
}

type TimelineItem =
  | { type: "message"; data: MessageView; timestamp: string }
  | { type: "activity"; data: ActivityView; timestamp: string };

export const ConversationDetail: React.FC<ConversationDetailProps> = ({
  selectedId,
}) => {
  const queryClient = useQueryClient();
  const [replyText, setReplyText] = useState("");
  const [sendError, setSendError] = useState<string | null>(null);
  const timelineEndRef = useRef<HTMLDivElement>(null);

  // Fetch conversation detail
  const {
    data: convo,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["conversation", selectedId],
    queryFn: () => (selectedId ? conversationsApi.get(selectedId) : null),
    enabled: !!selectedId,
    refetchInterval: 5000, // Auto-refresh fallback polling
  });

  // Reply Mutation
  const replyMutation = useMutation({
    mutationFn: (content: string) => {
      if (!selectedId) throw new Error("No conversation selected");
      return conversationsApi.sendReply(selectedId, content);
    },
    onSuccess: (data) => {
      setReplyText("");
      setSendError(null);
      // Update this conversation's detail view in cache
      queryClient.setQueryData(["conversation", selectedId], data);
      // Invalidate conversation list to update preview and positions
      queryClient.invalidateQueries({ queryKey: ["conversations"] });
    },
    onError: (err) => {
      if (err instanceof HttpError) {
        setSendError(
          err.problem.detail || err.problem.title || "Lỗi gửi câu trả lời",
        );
      } else {
        setSendError("Không thể kết nối máy chủ để gửi câu trả lời");
      }
    },
  });

  // Retry Mutation
  const retryMutation = useMutation({
    mutationFn: (messageId: string) => conversationsApi.retryMessage(messageId),
    onSuccess: () => {
      // Invalidate current conversation detail to fetch updated statuses
      queryClient.invalidateQueries({ queryKey: ["conversation", selectedId] });
      queryClient.invalidateQueries({ queryKey: ["conversations"] });
    },
  });

  // Scroll to bottom on load/new messages
  useEffect(() => {
    timelineEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [convo?.messages, convo?.activities]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!replyText.trim() || replyMutation.isPending) return;
    replyMutation.mutate(replyText.trim());
  };

  const getTimelineItems = (): TimelineItem[] => {
    if (!convo) return [];

    const items: TimelineItem[] = [];

    convo.messages.forEach((m) => {
      items.push({
        type: "message",
        data: m,
        timestamp: m.occurredAt || m.createdAt,
      });
    });

    convo.activities.forEach((a) => {
      items.push({
        type: "activity",
        data: a,
        timestamp: a.createdAt,
      });
    });

    // Sort chronologically
    return items.sort(
      (a, b) =>
        new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    );
  };

  const formatActivityText = (act: ActivityView) => {
    const actor = act.actorAgent ? act.actorAgent.displayName : "Hệ thống";
    const oldVal = act.oldValue || "Trống";
    const newVal = act.newValue || "Trống";

    switch (act.activityType) {
      case "STATUS_CHANGED":
        return `${actor} đã đổi trạng thái từ ${oldVal} sang ${newVal}`;
      case "ASSIGNMENT_CHANGED":
        return oldVal === "Unassigned" || !act.oldValue
          ? `${actor} đã nhận/phân công cho ${newVal}`
          : `${actor} đã chuyển phân công từ ${oldVal} sang ${newVal}`;
      case "MESSAGE_RECEIVED":
        return `Nhận tin nhắn mới từ khách hàng`;
      case "REPLY_QUEUED":
        return `Đã đưa câu trả lời của ${actor} vào hàng đợi`;
      case "DELIVERY_STATUS_CHANGED":
        return `Trạng thái truyền phát tin nhắn đã thay đổi từ ${oldVal} sang ${newVal}`;
      default:
        return `Lịch sử thao tác của hội thoại`;
    }
  };

  const formatTimestamp = (isoString: string) => {
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

  if (!selectedId) {
    return (
      <div className="pane-middle">
        <div className="empty-workspace-state">
          <MessageSquare size={48} className="empty-state-icon" />
          <h2>Chào mừng bạn quay lại</h2>
          <p>
            Chọn một cuộc hội thoại từ danh sách bên trái để bắt đầu hỗ trợ
            khách hàng.
          </p>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="pane-middle">
        <div className="empty-workspace-state">
          <div className="spinner" data-testid="spinner"></div>
          <p>Đang tải chi tiết cuộc hội thoại...</p>
        </div>
      </div>
    );
  }

  if (isError || !convo) {
    return (
      <div className="pane-middle">
        <div className="empty-workspace-state">
          <AlertCircle
            size={48}
            style={{ color: "var(--status-failed-text)" }}
          />
          <h2>Không tìm thấy cuộc hội thoại</h2>
          <p>
            {error instanceof Error
              ? error.message
              : "Đã có lỗi xảy ra khi truy vấn dữ liệu."}
          </p>
        </div>
      </div>
    );
  }

  const timelineItems = getTimelineItems();

  return (
    <div className="pane-middle" data-testid="conversation-detail-pane">
      <div className="chat-header">
        <div className="chat-header-customer">
          <span className="chat-header-name">{convo.customer.displayName}</span>
          <div className="chat-header-meta">
            <span
              className={`badge ${convo.channel === "FACEBOOK" ? "badge-channel-fb" : "badge-channel-email"}`}
            >
              {convo.channel} • {convo.sourceType}
            </span>
            <span className="convo-identity" style={{ margin: 0 }}>
              ID: {convo.channelIdentity.externalIdentityId}
            </span>
          </div>
        </div>
      </div>

      <div className="messages-timeline" data-testid="messages-timeline">
        {timelineItems.map((item, idx) => {
          if (item.type === "message") {
            const msg = item.data;
            const isInbound = msg.direction === "INBOUND";

            return (
              <div
                key={`msg-${msg.id}-${idx}`}
                className={`message-wrapper ${isInbound ? "inbound" : "outbound"}`}
                data-testid={`message-item-${msg.id}`}
              >
                <div
                  className="message-bubble"
                  data-testid={`message-content-${msg.id}`}
                >
                  {msg.content}
                </div>
                <div className="message-meta">
                  <span>{formatTimestamp(item.timestamp)}</span>
                  {!isInbound && (
                    <span
                      className={`delivery-status-badge ${
                        msg.deliveryStatus === "QUEUED"
                          ? "status-queued"
                          : msg.deliveryStatus === "SENT"
                            ? "status-sent"
                            : "status-failed"
                      }`}
                      data-testid={`delivery-status-${msg.id}`}
                    >
                      {msg.deliveryStatus === "QUEUED" && "Đang đợi"}
                      {msg.deliveryStatus === "SENT" && "Đã gửi"}
                      {msg.deliveryStatus === "FAILED" && "Thất bại"}
                    </span>
                  )}
                  {msg.deliveryStatus === "FAILED" && (
                    <button
                      className="retry-action-btn"
                      onClick={() => retryMutation.mutate(msg.id)}
                      disabled={retryMutation.isPending}
                      data-testid={`retry-button-${msg.id}`}
                    >
                      {retryMutation.isPending ? "Đang gửi..." : "Thử lại"}
                    </button>
                  )}
                </div>
              </div>
            );
          } else {
            const act = item.data;
            return (
              <div
                key={`act-${act.id}-${idx}`}
                className="activity-divider"
                data-testid={`activity-item-${act.id}`}
              >
                <span className="activity-divider-text">
                  <span>{formatActivityText(act)}</span>
                  <span className="activity-time">
                    {formatTimestamp(item.timestamp)}
                  </span>
                </span>
              </div>
            );
          }
        })}
        <div ref={timelineEndRef} />
      </div>

      <div className="reply-composer-container">
        {sendError && (
          <div className="error-banner" role="alert">
            <AlertCircle size={16} />
            <span>{sendError}</span>
          </div>
        )}
        <form className="reply-form" onSubmit={handleSend}>
          <textarea
            className="reply-textarea"
            placeholder="Nhập nội dung câu trả lời của bạn..."
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            disabled={replyMutation.isPending}
            data-testid="reply-textarea"
          />
          <div className="reply-actions-row">
            <span className="composer-hints">
              Gửi tin nhắn dưới dạng văn bản thuần an toàn. Original Channel:{" "}
              {convo.channel}.
            </span>
            <button
              type="submit"
              className="reply-submit-btn"
              disabled={!replyText.trim() || replyMutation.isPending}
              data-testid="reply-submit-button"
            >
              {replyMutation.isPending ? (
                <div
                  className="inline-loader"
                  data-testid="composer-spinner"
                ></div>
              ) : (
                <Send size={14} />
              )}
              <span>Gửi phản hồi</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
