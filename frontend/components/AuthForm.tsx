"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { Scale, Loader2, BookOpen, Sparkles, ShieldCheck } from "lucide-react";
import { ApiError } from "@/lib/api";

interface Props {
  mode: "login" | "register";
  onSubmit: (data: { email: string; password: string; fullName?: string }) => Promise<void>;
}

const INPUT_BASE =
  "w-full rounded-lg border bg-white px-3.5 py-2.5 text-sm outline-none transition focus:ring-4 dark:bg-slate-800 dark:text-slate-100";
const INPUT_OK =
  "border-slate-300 focus:border-indigo-500 focus:ring-indigo-500/10 dark:border-slate-700 dark:focus:border-indigo-500";
const INPUT_BAD =
  "border-red-400 focus:border-red-500 focus:ring-red-500/10 dark:border-red-700 dark:focus:border-red-500";

export default function AuthForm({ mode, onSubmit }: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const isRegister = mode === "register";

  const inputClass = (field: string) =>
    `${INPUT_BASE} ${fieldErrors[field] ? INPUT_BAD : INPUT_OK}`;

  // Xoá lỗi của riêng ô đang gõ, để người dùng thấy nó biến mất ngay khi sửa đúng.
  function clearFieldError(field: string) {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);
    try {
      await onSubmit({
        email: email.trim(),
        password,
        fullName: isRegister ? fullName.trim() || undefined : undefined,
      });
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fields).length > 0) {
        // Backend đã chỉ rõ ô nào sai -> gắn vào từng ô, khỏi hiện dòng chung.
        setFieldErrors(err.fields);
      } else {
        setError(err instanceof Error ? err.message : "Có lỗi xảy ra");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Left brand panel */}
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-indigo-600 via-violet-600 to-purple-700 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="pointer-events-none absolute -left-24 -top-24 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 bottom-0 h-96 w-96 rounded-full bg-amber-400/20 blur-3xl" />

        <div className="relative flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/15 backdrop-blur">
            <Scale className="h-6 w-6" strokeWidth={2.25} />
          </div>
          <div>
            <p className="text-xl font-bold tracking-tight">LexAI</p>
            <p className="text-[11px] font-medium uppercase tracking-widest text-white/70">
              Trợ lý Pháp luật
            </p>
          </div>
        </div>

        <div className="relative">
          <h2 className="text-4xl font-bold leading-tight tracking-tight">
            Pháp luật <br />
            <span className="bg-gradient-to-r from-amber-200 to-amber-400 bg-clip-text text-transparent">
              chính xác & nhanh chóng
            </span>
          </h2>
          <p className="mt-4 max-w-md text-white/80">
            Hỏi đáp văn bản pháp luật Việt Nam với câu trả lời luôn kèm trích dẫn nguồn — Luật, Nghị định, Thông tư.
          </p>

          <ul className="mt-8 space-y-3 text-sm text-white/90">
            <li className="flex items-center gap-3">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/15 backdrop-blur">
                <BookOpen className="h-3.5 w-3.5" />
              </span>
              Dẫn nguồn từng Điều, Chương, Mục
            </li>
            <li className="flex items-center gap-3">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/15 backdrop-blur">
                <Sparkles className="h-3.5 w-3.5" />
              </span>
              AI hiểu ngữ cảnh hội thoại tiếng Việt
            </li>
            <li className="flex items-center gap-3">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/15 backdrop-blur">
                <ShieldCheck className="h-3.5 w-3.5" />
              </span>
              Chống bịa nguồn — chỉ trích văn bản đã nạp
            </li>
          </ul>
        </div>

        <p className="relative text-xs text-white/60">
          © {new Date().getFullYear()} LexAI · Trợ lý tham khảo, không thay thế tư vấn luật sư.
        </p>
      </div>

      {/* Right form panel */}
      <div className="relative flex items-center justify-center bg-slate-50 px-4 py-10 dark:bg-slate-950">
        <div className="pointer-events-none absolute -top-20 right-1/4 h-72 w-72 rounded-full bg-indigo-400/20 blur-3xl lg:hidden dark:bg-indigo-500/10" />

        <div className="relative w-full max-w-md">
          {/* Mobile brand */}
          <div className="mb-6 text-center lg:hidden">
            <div className="brand-gradient mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl shadow-lg shadow-indigo-500/30">
              <Scale className="h-7 w-7 text-white" strokeWidth={2.25} />
            </div>
            <h1 className="brand-text text-2xl font-bold tracking-tight">LexAI</h1>
            <p className="text-xs text-slate-500">Trợ lý Pháp luật Việt Nam</p>
          </div>

          <div className="mb-6">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
              {isRegister ? "Tạo tài khoản mới" : "Chào mừng trở lại"}
            </h1>
            <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
              {isRegister
                ? "Đăng ký để bắt đầu trò chuyện với LexAI"
                : "Đăng nhập để tiếp tục sử dụng LexAI"}
            </p>
          </div>

          <form
            onSubmit={handleSubmit}
            className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-800 dark:bg-slate-900"
          >
            {isRegister && (
              <div className="mb-4">
                <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Họ và tên
                </label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => {
                    setFullName(e.target.value);
                    clearFieldError("fullName");
                  }}
                  placeholder="Nguyễn Văn A"
                  aria-invalid={!!fieldErrors.fullName}
                  className={inputClass("fullName")}
                />
                {fieldErrors.fullName && (
                  <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                    {fieldErrors.fullName}
                  </p>
                )}
              </div>
            )}

            <div className="mb-4">
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  clearFieldError("email");
                }}
                required
                placeholder="email@example.com"
                aria-invalid={!!fieldErrors.email}
                className={inputClass("email")}
              />
              {fieldErrors.email && (
                <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                  {fieldErrors.email}
                </p>
              )}
            </div>

            <div className="mb-4">
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                Mật khẩu
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  clearFieldError("password");
                }}
                required
                minLength={6}
                placeholder={isRegister ? "Tối thiểu 6 ký tự" : "••••••"}
                aria-invalid={!!fieldErrors.password}
                className={inputClass("password")}
              />
              {fieldErrors.password && (
                <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            {error && (
              <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-300">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="brand-gradient flex w-full items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-indigo-500/20 transition hover:shadow-lg hover:shadow-indigo-500/30 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60 disabled:shadow-none"
            >
              {loading && <Loader2 className="h-4 w-4 animate-spin" />}
              {isRegister ? "Tạo tài khoản" : "Đăng nhập"}
            </button>

            <p className="mt-5 text-center text-sm text-slate-600 dark:text-slate-400">
              {isRegister ? (
                <>
                  Đã có tài khoản?{" "}
                  <Link
                    href="/login"
                    className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
                  >
                    Đăng nhập
                  </Link>
                </>
              ) : (
                <>
                  Chưa có tài khoản?{" "}
                  <Link
                    href="/register"
                    className="font-semibold text-indigo-600 hover:underline dark:text-indigo-400"
                  >
                    Đăng ký
                  </Link>
                </>
              )}
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}
