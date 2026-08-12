export interface User {
  id: number;
  email: string;
  fullName: string | null;
  role: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export type MessageRole = "user" | "assistant";

export interface Citation {
  law_name: string;
  chapter: string;
  section: string;
  article: string;
  source_file: string;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: MessageRole;
  content: string;
  citations?: Citation[] | null;
  createdAt: string;
  latencyMs?: number | null;
}

export interface RetrievedChunk {
  content: string;
  metadata: Record<string, any>;
}

export interface ChatSession {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
  messages?: ChatMessage[];
}

export interface SendMessageResponse {
  sessionId: number;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  retrievedChunks: RetrievedChunk[];
}
