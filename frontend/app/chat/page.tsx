"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import Sidebar from "@/components/Sidebar";
import TopBar from "@/components/TopBar";
import ChatWindow from "@/components/ChatWindow";
import ChatInput from "@/components/ChatInput";
import { api } from "@/lib/api";
import { getToken, getStoredUser, setStoredUser, clearToken } from "@/lib/auth";
import type { ChatMessage, ChatSession, Citation, User } from "@/lib/types";

function parseCitations(raw: unknown): Citation[] {
  if (Array.isArray(raw)) return raw as Citation[];
  if (typeof raw === "string") {
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
}

export default function ChatPage() {
  const router = useRouter();

  const [authChecking, setAuthChecking] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      router.replace("/login");
      return;
    }
    setUser(getStoredUser());
    api
      .me()
      .then((u) => {
        setUser(u);
        setStoredUser(u);
        setAuthChecking(false);
      })
      .catch(() => {
        clearToken();
        router.replace("/login");
      });
  }, [router]);

  const refreshSessions = useCallback(async () => {
    try {
      const list = await api.listSessions();
      setSessions(list);
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    if (!authChecking) refreshSessions();
  }, [authChecking, refreshSessions]);

  async function handleSelect(id: number) {
    if (id === currentSessionId) return;
    setLoading(true);
    setError(null);
    try {
      const session = await api.getSession(id);
      const msgs = (session.messages || []).map((m) => ({
        ...m,
        citations: parseCitations(m.citations),
      }));
      setMessages(msgs);
      setCurrentSessionId(id);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function handleNew() {
    setCurrentSessionId(null);
    setMessages([]);
    setError(null);
  }

  async function handleDelete(id: number) {
    try {
      await api.deleteSession(id);
      if (id === currentSessionId) handleNew();
      await refreshSessions();
    } catch (e: any) {
      setError(e.message);
    }
  }

  async function handleSend(question: string) {
    setLoading(true);
    setError(null);

    const tempUserMsg: ChatMessage = {
      id: -Date.now(),
      sessionId: currentSessionId ?? 0,
      role: "user",
      content: question,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);

    try {
      const res = await api.sendMessage(question, currentSessionId || undefined);
      setCurrentSessionId(res.sessionId);
      setMessages((prev) => [
        ...prev.filter((m) => m.id !== tempUserMsg.id),
        res.userMessage,
        {
          ...res.assistantMessage,
          citations: parseCitations(res.assistantMessage.citations),
        },
      ]);
      await refreshSessions();
    } catch (e: any) {
      setError(e.message);
      setMessages((prev) => prev.filter((m) => m.id !== tempUserMsg.id));
    } finally {
      setLoading(false);
    }
  }

  if (authChecking) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-50 dark:bg-slate-950">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-7 w-7 animate-spin text-indigo-500" />
          <p className="text-xs font-medium tracking-wider text-slate-400 uppercase">
            Đang tải LexAI...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen w-full flex-col overflow-hidden bg-slate-50 dark:bg-slate-950">
      <TopBar user={user} />

      <div className="flex min-h-0 flex-1">
        <Sidebar
          sessions={sessions}
          currentId={currentSessionId}
          onSelect={handleSelect}
          onNew={handleNew}
          onDelete={handleDelete}
        />

        <main className="relative flex min-w-0 flex-1 flex-col overflow-hidden bg-gradient-to-br from-slate-100 via-indigo-50/60 to-violet-100/50 dark:from-slate-900 dark:via-indigo-950/40 dark:to-violet-950/30">
          {error && (
            <div className="flex items-center justify-between gap-3 border-b border-red-200 bg-red-50 px-4 py-2 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300">
              <span className="truncate">{error}</span>
              <button
                onClick={() => setError(null)}
                className="shrink-0 rounded px-1.5 text-xs font-semibold hover:bg-red-100 dark:hover:bg-red-900/40"
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
          )}
          <ChatWindow messages={messages} loading={loading} />
          <ChatInput onSend={handleSend} disabled={loading} />
        </main>
      </div>
    </div>
  );
}
