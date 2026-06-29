import React from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../../app/providers";
import { ConversationList } from "./ConversationList";
import { ConversationDetail } from "./ConversationDetail";
import { MetadataPanel } from "./MetadataPanel";
import { LogOut } from "lucide-react";

export const UnifiedInbox: React.FC = () => {
  const { agent, logout } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedConvoId = searchParams.get("id") || undefined;

  const handleSelectConvo = (id: string | undefined) => {
    if (id) {
      setSearchParams({ id });
    } else {
      searchParams.delete("id");
      setSearchParams(searchParams);
    }
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="app-title-group">
          <div className="app-logo">
            <span>OmniCare.</span>
          </div>
          <span className="app-title-badge">Viettel Workspace</span>
        </div>

        {agent && (
          <div className="agent-profile">
            <div className="agent-info">
              <span className="agent-name">{agent.displayName}</span>
              <span className="agent-email">{agent.email}</span>
            </div>
            <button className="logout-button" onClick={logout}>
              <LogOut size={14} />
              <span>Đăng xuất</span>
            </button>
          </div>
        )}
      </header>

      <main className="workspace-grid">
        <ConversationList
          selectedId={selectedConvoId}
          onSelect={handleSelectConvo}
        />

        <ConversationDetail selectedId={selectedConvoId} />

        <MetadataPanel
          selectedId={selectedConvoId}
          onStatusOrAssigneeChange={() => {
            // Handled inside components via React Query invalidation,
            // but this callback is provided if parent component needs sync.
          }}
        />
      </main>
    </div>
  );
};
