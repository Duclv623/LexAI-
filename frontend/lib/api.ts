import type {
  AuthResponse,
  ChatMessage,
  ChatSession,
  RetrievedChunk,
  SendMessageResponse,
  User,
} from "./types";
import { getToken, clearToken } from "./auth";

// Trỏ vào API gateway (Spring), không gọi thẳng từng service.
// Gateway lo verify token, rate limit và CORS trước khi chuyển tiếp.
const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

/**
 * Lỗi từ API, mang theo status và map fields do backend trả về.
 *
 * fields chỉ có ở lỗi validate, dạng {"email": "Email không đúng định dạng"} —
 * nhờ nó form biết ô nào sai để tô đỏ, thay vì chỉ hiện một dòng chung chung.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly fields: Record<string, string>;

  constructor(status: number, message: string, fields?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fields = fields ?? {};
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...((init?.headers as Record<string, string>) || {}),
  };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...init, headers });

  // Chỉ đá về /login khi request CÓ mang token mà vẫn bị từ chối — đó mới là phiên hết
  // hạn hoặc token bị thu hồi. Không có token thì 401 nghĩa là đăng nhập sai, phải để
  // form tự hiện thông báo; tải lại trang ở đây sẽ xoá sạch form và nuốt mất lỗi.
  if (res.status === 401 && token) {
    clearToken();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new ApiError(401, "Phiên đăng nhập đã hết hạn");
  }

  if (!res.ok) {
    let msg = `API ${res.status}`;
    let fields: Record<string, string> | undefined;
    try {
      const j = await res.json();
      msg = j.message || j.error || msg;
      if (j.fields && typeof j.fields === "object") fields = j.fields;
    } catch {
      // thân rỗng hoặc không phải JSON
    }
    throw new ApiError(res.status, typeof msg === "string" ? msg : JSON.stringify(msg), fields);
  }

  // DELETE trả 204 không có thân. res.json() trên thân rỗng sẽ ném lỗi.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * Backend bọc mọi response nghiệp vụ trong CustomResponse {data, message, success}.
 * UI chỉ cần phần data, nên bóc vỏ ngay tại đây thay vì rải res.data khắp nơi.
 *
 * Không áp dụng cho DELETE (204 không có thân) và các endpoint hạ tầng
 * (/health, /.well-known/jwks.json) — chúng vẫn trả JSON trần.
 */
interface CustomResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

async function requestData<T>(path: string, init?: RequestInit): Promise<T> {
  return (await request<CustomResponse<T>>(path, init)).data;
}

/* ------------------------------------------------------------------------- *
 * Shape phía server (Spring) — KHÁC với shape mà UI đang dùng.
 *
 * Toàn bộ việc quy đổi gom vào file này. Backend giữ ngôn ngữ miền của nó
 * (conversation, role viết hoa), còn UI giữ nguyên tên cũ (session, role
 * viết thường) nên không phải sửa component nào.
 * ------------------------------------------------------------------------- */

interface SpringAuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  account: User;
}

interface ServerMessage {
  id: number;
  conversationId: number;
  role: "USER" | "ASSISTANT";
  content: string;
  citations?: unknown;
  latencyMs?: number | null;
  createdAt: string;
}

interface ServerConversation {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
  messages?: ServerMessage[];
}

interface ServerTurn {
  userMessage: ServerMessage;
  assistantMessage: ServerMessage;
  retrievedChunks?: unknown;
}

export interface Pagination {
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
}

interface ServerConversationList {
  conversations: ServerConversation[];
  pagination: Pagination;
}

function toAuthResponse(res: SpringAuthResponse): AuthResponse {
  return { token: res.accessToken, user: res.account };
}

function toMessage(m: ServerMessage): ChatMessage {
  return {
    id: m.id,
    sessionId: m.conversationId,
    role: m.role.toLowerCase() as ChatMessage["role"],
    content: m.content,
    citations: (m.citations as ChatMessage["citations"]) ?? null,
    createdAt: m.createdAt,
    latencyMs: m.latencyMs ?? null,
  };
}

function toSession(c: ServerConversation): ChatSession {
  return {
    id: c.id,
    title: c.title,
    createdAt: c.createdAt,
    updatedAt: c.updatedAt,
    messageCount: c.messageCount,
    messages: c.messages?.map(toMessage),
  };
}

export const api = {
  // ----- Auth -----
  register: async (email: string, password: string, fullName?: string) =>
    toAuthResponse(
      await requestData<SpringAuthResponse>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, fullName }),
      })
    ),

  login: async (email: string, password: string) =>
    toAuthResponse(
      await requestData<SpringAuthResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      })
    ),

  me: () => requestData<User>("/api/auth/me"),

  changePassword: (currentPassword: string, newPassword: string) =>
    request<CustomResponse<void>>("/api/auth/password", {
      method: "PATCH",
      body: JSON.stringify({ currentPassword, newPassword }),
    }),

  // Xoá token khỏi localStorage là chưa đủ: token vẫn hợp lệ tới khi hết hạn.
  // Endpoint này xoá nó khỏi Redis nên gateway từ chối ngay lập tức.
  logout: () =>
    request<CustomResponse<void>>("/api/auth/logout", { method: "POST" }),

  // ----- Chat -----
  /**
   * Chưa có phiên thì tạo hội thoại trước rồi mới gửi — hai request thay vì một.
   * Đây là cái giá của việc API REST tách bạch "tạo hội thoại" và "gửi tin nhắn",
   * đổi lại mỗi endpoint làm đúng một việc.
   */
  sendMessage: async (
    question: string,
    sessionId?: number
  ): Promise<SendMessageResponse> => {
    const id =
      sessionId ??
      (
        await requestData<ServerConversation>("/api/chat/conversations", {
          method: "POST",
          body: JSON.stringify({}),
        })
      ).id;

    const turn = await requestData<ServerTurn>(`/api/chat/conversations/${id}/messages`, {
      method: "POST",
      body: JSON.stringify({ content: question }),
    });

    return {
      sessionId: id,
      userMessage: toMessage(turn.userMessage),
      assistantMessage: toMessage(turn.assistantMessage),
      retrievedChunks: (turn.retrievedChunks ?? []) as RetrievedChunk[],
    };
  },

  // ----- Sessions (bên server gọi là conversations) -----
  // Trả kèm pagination để UI biết còn trang sau hay không (totalPages > currentPage + 1).
  listSessions: async (page = 0, size = 20) => {
    const res = await requestData<ServerConversationList>(
      `/api/chat/conversations?page=${page}&size=${size}`
    );
    return { sessions: res.conversations.map(toSession), pagination: res.pagination };
  },

  getSession: async (id: number) =>
    toSession(await requestData<ServerConversation>(`/api/chat/conversations/${id}`)),

  deleteSession: async (id: number) => {
    await request<void>(`/api/chat/conversations/${id}`, { method: "DELETE" });
    return { deleted: true };
  },
};
