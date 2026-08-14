"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Loader2, Lock, X } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { clearToken } from "@/lib/auth";

const INPUT_BASE =
  "w-full rounded-lg border bg-white px-3 py-2 text-sm outline-none transition focus:ring-4 dark:bg-slate-800 dark:text-slate-100";
const INPUT_OK =
  "border-slate-300 focus:border-indigo-500 focus:ring-indigo-500/10 dark:border-slate-700";
const INPUT_BAD =
  "border-red-400 focus:border-red-500 focus:ring-red-500/10 dark:border-red-700";

export default function ChangePasswordModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const router = useRouter();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState(false);

  if (!open) return null;

  const inputClass = (field: string) =>
    `${INPUT_BASE} ${fieldErrors[field] ? INPUT_BAD : INPUT_OK}`;

  function clearFieldError(field: string) {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }

  function close() {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setError(null);
    setFieldErrors({});
    setSuccess(false);
    onClose();
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    // Kiểm tại chỗ những thứ đáng phản hồi ngay, khỏi tốn một vòng gọi API.
    // Độ dài mật khẩu thì để backend quyết (@Size trong ChangePasswordRequestDTO)
    // cho khỏi có hai nguồn sự thật lệch nhau.
    if (newPassword !== confirmPassword) {
      setFieldErrors({ confirmPassword: "Mật khẩu xác nhận không khớp" });
      return;
    }
    if (currentPassword === newPassword) {
      setFieldErrors({ newPassword: "Mật khẩu mới phải khác mật khẩu hiện tại" });
      return;
    }

    setLoading(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      setSuccess(true);
      // Backend đã thu hồi token, phiên hiện tại chết rồi. Đưa thẳng về trang đăng nhập
      // thay vì để người dùng bấm tiếp rồi mới bị đá ra giữa chừng.
      setTimeout(() => {
        clearToken();
        router.push("/login");
      }, 2000);
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fields).length > 0) {
        setFieldErrors(e.fields);
      } else {
        setError(e instanceof Error ? e.message : "Có lỗi xảy ra");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4 backdrop-blur-sm"
      onClick={close}
    >
      <div
        className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl dark:border-slate-800 dark:bg-slate-900"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={close}
          className="absolute right-3 top-3 rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 dark:hover:bg-slate-800"
          aria-label="Đóng"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="mb-5 flex items-center gap-3">
          <div className="brand-gradient flex h-10 w-10 items-center justify-center rounded-xl shadow-md shadow-indigo-500/30">
            <Lock className="h-5 w-5 text-white" strokeWidth={2.25} />
          </div>
          <div>
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">
              Đổi mật khẩu
            </h2>
            <p className="text-xs text-slate-500">
              Mật khẩu mới tối thiểu 8 ký tự
            </p>
          </div>
        </div>

        {success ? (
          <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-950/30 dark:text-emerald-300">
            Đổi mật khẩu thành công! Mọi phiên đăng nhập cũ đã bị thu hồi, đang đưa
            bạn về trang đăng nhập...
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                Mật khẩu hiện tại
              </label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => {
                  setCurrentPassword(e.target.value);
                  clearFieldError("currentPassword");
                }}
                required
                autoFocus
                aria-invalid={!!fieldErrors.currentPassword}
                className={inputClass("currentPassword")}
              />
              {fieldErrors.currentPassword && (
                <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                  {fieldErrors.currentPassword}
                </p>
              )}
            </div>

            <div className="mb-3">
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                Mật khẩu mới
              </label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  clearFieldError("newPassword");
                }}
                required
                minLength={8}
                placeholder="Tối thiểu 8 ký tự"
                aria-invalid={!!fieldErrors.newPassword}
                className={inputClass("newPassword")}
              />
              {fieldErrors.newPassword && (
                <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                  {fieldErrors.newPassword}
                </p>
              )}
            </div>

            <div className="mb-4">
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                Xác nhận mật khẩu mới
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  clearFieldError("confirmPassword");
                }}
                required
                aria-invalid={!!fieldErrors.confirmPassword}
                className={inputClass("confirmPassword")}
              />
              {fieldErrors.confirmPassword && (
                <p className="mt-1.5 text-xs text-red-600 dark:text-red-400">
                  {fieldErrors.confirmPassword}
                </p>
              )}
            </div>

            {error && (
              <div className="mb-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950/30 dark:text-red-300">
                {error}
              </div>
            )}

            <div className="flex gap-2">
              <button
                type="button"
                onClick={close}
                disabled={loading}
                className="flex-1 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:opacity-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
              >
                Huỷ
              </button>
              <button
                type="submit"
                disabled={loading}
                className="brand-gradient flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold text-white shadow-md shadow-indigo-500/20 transition hover:shadow-lg hover:shadow-indigo-500/30 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                Đổi mật khẩu
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
