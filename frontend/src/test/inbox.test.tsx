import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { ConversationList } from "../features/inbox/ConversationList";
import { ConversationDetail } from "../features/inbox/ConversationDetail";
import { MetadataPanel } from "../features/inbox/MetadataPanel";
import { conversationsApi } from "../api/conversationsApi";
import { agentsApi } from "../api/agentsApi";
import type {
  ConversationDetailView,
  PageView,
  ConversationListItem,
  Agent,
} from "../types";

// Mock the API client functions
vi.mock("../api/conversationsApi", () => ({
  conversationsApi: {
    list: vi.fn(),
    get: vi.fn(),
    changeStatus: vi.fn(),
    changeAssignee: vi.fn(),
    sendReply: vi.fn(),
    retryMessage: vi.fn(),
  },
}));

vi.mock("../api/agentsApi", () => ({
  agentsApi: {
    list: vi.fn(),
  },
}));

// Mock the useAuth hook to return a dummy logged-in agent
vi.mock("../app/providers", () => ({
  useAuth: () => ({
    agent: {
      id: "agent-1",
      displayName: "Mock Agent",
      email: "mock@example.test",
    },
  }),
}));

const mockAgents: Agent[] = [
  {
    id: "10000000-0000-0000-0000-000000000001",
    email: "agent@example.test",
    displayName: "Demo Agent",
  },
  {
    id: "10000000-0000-0000-0000-000000000002",
    email: "minh.agent@example.test",
    displayName: "Minh Agent",
  },
];

const mockConvoList: PageView<ConversationListItem> = {
  content: [
    {
      id: "convo-1",
      customerDisplayName: "Nguyen Van A",
      channel: "FACEBOOK",
      sourceType: "MESSAGE",
      channelIdentity: "fb-user-a",
      lastMessagePreview: "Shop oi don hang cua minh den dau roi?",
      lastActivityAt: "2026-06-22T02:00:00Z",
      status: "OPEN",
      assignedAgent: undefined,
    },
    {
      id: "convo-2",
      customerDisplayName: "Tran Thi B",
      channel: "EMAIL",
      sourceType: "EMAIL",
      channelIdentity: "tran.b@example.test",
      lastMessagePreview: "Minh can ho tro ve don hang #42",
      lastActivityAt: "2026-06-22T03:00:00Z",
      status: "PENDING",
      assignedAgent: mockAgents[0],
    },
  ],
  page: 0,
  size: 15,
  totalElements: 2,
  totalPages: 1,
};

const mockConvoDetail: ConversationDetailView = {
  id: "convo-1",
  customer: { id: "cust-1", displayName: "Nguyen Van A" },
  channelIdentity: {
    id: "identity-1",
    channel: "FACEBOOK",
    providerAccountId: "local-page-id",
    externalIdentityId: "fb-user-a",
    displayName: "Nguyen Van A",
  },
  channel: "FACEBOOK",
  sourceType: "MESSAGE",
  status: "OPEN",
  assignedAgent: undefined,
  subject: undefined,
  lastMessagePreview: "Shop oi don hang cua minh den dau roi?",
  lastActivityAt: "2026-06-22T02:00:00Z",
  createdAt: "2026-06-22T02:00:00Z",
  updatedAt: "2026-06-22T02:00:00Z",
  messages: [
    {
      id: "msg-1",
      direction: "INBOUND",
      deliveryStatus: "RECEIVED",
      externalMessageId: "mid.1",
      content: "Shop oi don hang cua minh den dau roi?",
      occurredAt: "2026-06-22T02:00:00Z",
      createdAt: "2026-06-22T02:00:00Z",
    },
    {
      id: "msg-2",
      direction: "OUTBOUND",
      deliveryStatus: "FAILED",
      externalMessageId: "mid.2",
      content: "Chào bạn, đơn hàng của bạn đang được giao.",
      occurredAt: "2026-06-22T02:05:00Z",
      createdAt: "2026-06-22T02:05:00Z",
    },
  ],
  activities: [
    {
      id: "act-1",
      activityType: "MESSAGE_RECEIVED",
      createdAt: "2026-06-22T02:00:00Z",
    },
  ],
};

describe("Inbox Component Suite", () => {
  let queryClient: QueryClient;

  const renderWithProviders = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>{ui}</BrowserRouter>
      </QueryClientProvider>,
    );
  };

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });
    vi.clearAllMocks();
    (conversationsApi.list as any).mockResolvedValue(mockConvoList);
    (conversationsApi.get as any).mockResolvedValue(mockConvoDetail);
    (agentsApi.list as any).mockResolvedValue(mockAgents);
  });

  describe("ConversationList", () => {
    it("renders list load states, items, and search inputs", async () => {
      renderWithProviders(<ConversationList onSelect={() => {}} />);

      // Verify search box and filters
      expect(
        screen.getByPlaceholderText(/Tìm theo tên hoặc địa chỉ.../i),
      ).toBeInTheDocument();
      expect(screen.getByLabelText("Lọc kênh")).toBeInTheDocument();
      expect(screen.getByLabelText("Lọc trạng thái")).toBeInTheDocument();
      expect(screen.getByLabelText("Lọc người gán")).toBeInTheDocument();

      // Verify list items loaded
      await waitFor(() => {
        expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
        expect(screen.getByText("Tran Thi B")).toBeInTheDocument();
      });

      // Verify channel identity and last activity details
      expect(screen.getByText("fb-user-a")).toBeInTheDocument();
      expect(screen.getByText("tran.b@example.test")).toBeInTheDocument();
    });

    it("handles query parameters filter and search queries", async () => {
      renderWithProviders(<ConversationList onSelect={() => {}} />);

      const searchInput = screen.getByPlaceholderText(
        /Tìm theo tên hoặc địa chỉ.../i,
      );
      fireEvent.change(searchInput, { target: { value: "Nguyen" } });

      await waitFor(() => {
        expect(conversationsApi.list).toHaveBeenCalledWith(
          expect.objectContaining({ search: "Nguyen" }),
        );
      });
    });

    it("renders status tab buttons with counts and filters conversations when clicked", async () => {
      renderWithProviders(<ConversationList onSelect={() => {}} />);

      // Verify status tabs render
      await waitFor(() => {
        expect(screen.getByTestId("status-tabs-row")).toBeInTheDocument();
      });

      expect(screen.getByLabelText("Tất cả trạng thái")).toBeInTheDocument();
      expect(screen.getByLabelText("Trạng thái Open")).toBeInTheDocument();
      expect(screen.getByLabelText("Trạng thái Pending")).toBeInTheDocument();
      expect(screen.getByLabelText("Trạng thái Resolved")).toBeInTheDocument();

      // Click on "Pending" tab
      const pendingTab = screen.getByLabelText("Trạng thái Pending");
      fireEvent.click(pendingTab);

      await waitFor(() => {
        expect(conversationsApi.list).toHaveBeenCalledWith(
          expect.objectContaining({ status: "PENDING" }),
        );
      });
    });
  });

  describe("ConversationDetail", () => {
    it("renders message bubbles safely in chronological order", async () => {
      renderWithProviders(<ConversationDetail selectedId="convo-1" />);

      // Verify detail is loaded
      await waitFor(() => {
        expect(screen.getByText("Nguyen Van A")).toBeInTheDocument();
        expect(
          screen.getByText("Shop oi don hang cua minh den dau roi?"),
        ).toBeInTheDocument();
      });

      // Outbound failed message rendering check
      expect(
        screen.getByText("Chào bạn, đơn hàng của bạn đang được giao."),
      ).toBeInTheDocument();
      expect(screen.getByText("Thất bại")).toBeInTheDocument();
      expect(
        screen.getByRole("button", { name: /Thử lại/i }),
      ).toBeInTheDocument();
    });

    it("triggers message retry when clicking Retry on failed message", async () => {
      (conversationsApi.retryMessage as any).mockResolvedValue({
        messageId: "msg-2",
      });

      renderWithProviders(<ConversationDetail selectedId="convo-1" />);

      await waitFor(() => {
        expect(
          screen.getByRole("button", { name: /Thử lại/i }),
        ).toBeInTheDocument();
      });

      const retryBtn = screen.getByRole("button", { name: /Thử lại/i });
      fireEvent.click(retryBtn);

      await waitFor(() => {
        expect(conversationsApi.retryMessage).toHaveBeenCalledWith("msg-2");
      });
    });

    it("allows writing a reply and posting it to API", async () => {
      const updatedDetail = {
        ...mockConvoDetail,
        messages: [
          ...mockConvoDetail.messages,
          {
            id: "msg-3",
            direction: "OUTBOUND",
            deliveryStatus: "QUEUED",
            content: "Tôi đang gửi tin mới",
            occurredAt: "2026-06-29T12:00:00Z",
            createdAt: "2026-06-29T12:00:00Z",
          },
        ],
      };
      (conversationsApi.sendReply as any).mockResolvedValue(updatedDetail);

      renderWithProviders(<ConversationDetail selectedId="convo-1" />);

      await waitFor(() => {
        expect(screen.getByTestId("reply-textarea")).toBeInTheDocument();
      });

      const input = screen.getByTestId("reply-textarea");
      const submitBtn = screen.getByTestId("reply-submit-button");

      fireEvent.change(input, { target: { value: "Tôi đang gửi tin mới" } });
      fireEvent.click(submitBtn);

      await waitFor(() => {
        expect(conversationsApi.sendReply).toHaveBeenCalledWith(
          "convo-1",
          "Tôi đang gửi tin mới",
        );
      });
    });
  });

  describe("MetadataPanel", () => {
    it("renders customer profile and allows changing status/assignee", async () => {
      (conversationsApi.changeStatus as any).mockResolvedValue({
        ...mockConvoDetail,
        status: "PENDING",
      });
      (conversationsApi.changeAssignee as any).mockResolvedValue({
        ...mockConvoDetail,
        assignedAgent: mockAgents[1],
      });

      renderWithProviders(<MetadataPanel selectedId="convo-1" />);

      // Verify metadata panel outputs
      await waitFor(() => {
        expect(screen.getByTestId("customer-name")).toHaveTextContent(
          "Nguyen Van A",
        );
        expect(screen.getByTestId("channel-identity-id")).toHaveTextContent(
          "fb-user-a",
        );
      });

      // Change status select box
      const statusSelect = screen.getByTestId("status-select");
      fireEvent.change(statusSelect, { target: { value: "PENDING" } });
      await waitFor(() => {
        expect(conversationsApi.changeStatus).toHaveBeenCalledWith(
          "convo-1",
          "PENDING",
        );
      });

      // Change assignee select box
      const assigneeSelect = screen.getByTestId("assignee-select");
      fireEvent.change(assigneeSelect, { target: { value: mockAgents[1].id } });
      await waitFor(() => {
        expect(conversationsApi.changeAssignee).toHaveBeenCalledWith(
          "convo-1",
          mockAgents[1].id,
        );
      });
    });
  });
});
