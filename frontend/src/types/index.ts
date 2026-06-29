export type Channel = "FACEBOOK" | "EMAIL";

export type ConversationSourceType = "MESSAGE" | "COMMENT" | "EMAIL";

export type ConversationStatus = "OPEN" | "PENDING" | "RESOLVED";

export type MessageDirection = "INBOUND" | "OUTBOUND";

export type DeliveryStatus = "RECEIVED" | "QUEUED" | "SENT" | "FAILED";

export interface Agent {
  id: string;
  email: string;
  displayName: string;
}

export interface CustomerSummary {
  id: string;
  displayName: string;
}

export interface ChannelIdentitySummary {
  id: string;
  channel: Channel;
  providerAccountId: string;
  externalIdentityId: string;
  displayName: string;
}

export interface MessageView {
  id: string;
  direction: MessageDirection;
  deliveryStatus: DeliveryStatus;
  externalMessageId?: string;
  content: string;
  occurredAt: string;
  createdAt: string;
}

export type ConversationActivityType =
  | "STATUS_CHANGED"
  | "ASSIGNMENT_CHANGED"
  | "MESSAGE_RECEIVED"
  | "REPLY_QUEUED"
  | "DELIVERY_STATUS_CHANGED";

export interface ActivityView {
  id: string;
  activityType: ConversationActivityType;
  actorAgent?: Agent;
  oldValue?: string;
  newValue?: string;
  createdAt: string;
}

export interface ConversationListItem {
  id: string;
  customerDisplayName: string;
  channel: Channel;
  sourceType: ConversationSourceType;
  channelIdentity: string;
  lastMessagePreview?: string;
  lastActivityAt: string;
  status: ConversationStatus;
  assignedAgent?: Agent;
}

export interface ConversationDetailView {
  id: string;
  customer: CustomerSummary;
  channelIdentity: ChannelIdentitySummary;
  channel: Channel;
  sourceType: ConversationSourceType;
  status: ConversationStatus;
  assignedAgent?: Agent;
  subject?: string;
  lastMessagePreview?: string;
  lastActivityAt: string;
  createdAt: string;
  updatedAt: string;
  messages: MessageView[];
  activities: ActivityView[];
}

export interface PageView<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  agent: Agent;
}

export interface ProblemDetails {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string[]>;
}
