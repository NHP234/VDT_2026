import { httpClient } from "./httpClient";
import type {
  ConversationListItem,
  ConversationDetailView,
  PageView,
  Channel,
  ConversationStatus,
} from "../types";

export interface ListConversationsParams {
  page?: number;
  size?: number;
  channel?: Channel;
  status?: ConversationStatus;
  assignee?: string;
  search?: string;
}

export const conversationsApi = {
  list: async (
    params: ListConversationsParams,
  ): Promise<PageView<ConversationListItem>> => {
    return httpClient<PageView<ConversationListItem>>("/api/v1/conversations", {
      method: "GET",
      params: params as Record<string, string | number | boolean>,
    });
  },

  get: async (id: string): Promise<ConversationDetailView> => {
    return httpClient<ConversationDetailView>(`/api/v1/conversations/${id}`, {
      method: "GET",
    });
  },

  changeStatus: async (
    id: string,
    status: ConversationStatus,
  ): Promise<ConversationDetailView> => {
    return httpClient<ConversationDetailView>(
      `/api/v1/conversations/${id}/status`,
      {
        method: "PATCH",
        body: JSON.stringify({ status }),
      },
    );
  },

  changeAssignee: async (
    id: string,
    assignedAgentId: string | null,
  ): Promise<ConversationDetailView> => {
    return httpClient<ConversationDetailView>(
      `/api/v1/conversations/${id}/assignee`,
      {
        method: "PATCH",
        body: JSON.stringify({ assignedAgentId }),
      },
    );
  },

  sendReply: async (
    id: string,
    content: string,
  ): Promise<ConversationDetailView> => {
    return httpClient<ConversationDetailView>(
      `/api/v1/conversations/${id}/replies`,
      {
        method: "POST",
        body: JSON.stringify({ content }),
      },
    );
  },

  retryMessage: async (messageId: string): Promise<{ messageId: string }> => {
    return httpClient<{ messageId: string }>(
      `/api/v1/messages/${messageId}/retry`,
      {
        method: "POST",
      },
    );
  },
};
