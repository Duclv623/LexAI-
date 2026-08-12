"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronDown,
  KeyRound,
  LogOut,
  Scale,
  Sparkles,
  User as UserIcon,
} from "lucide-react";
import type { User } from "@/lib/types";
import { clearToken } from "@/lib/auth";
import ChangePasswordModal from "./ChangePasswordModal";

const MODEL_LABEL = "Gemini 2.5 Flash";

export default function TopBar({ user }: { user: User | null }) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [pwdOpen, setPwdOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    function onDown(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  function handleLogout() {
    clearToken();
    router.push("/login");
  }

  const displayName = user?.fullName || user?.email?.split("@")[0] || "Guest";
  const initial = (user?.fullName || user?.email || "U").slice(0, 1).toUpperCase();

  return (
    <>
      <header className="z-30 flex h-14 shrink-0 items-center justify-between border-b border-slate-200/70 bg-white/80 px-4 backdrop-blur-md dark:border-slate-800 dark:bg-slate-950/80">
        {/* Brand */}
        <div className="flex items-center gap-2.5">
          <div className="brand-gradient flex h-8 w-8 items-center justify-center rounded-lg shadow-md shadow-indigo-500/30">
            <Scale className="h-4 w-4 text-white" strokeWidth={2.5} />
          </div>
          <div className="leading-tight">
            <p className="brand-text text-base font-bold tracking-tight">LexAI</p>
            <p className="text-[9px] font-semibold uppercase tracking-widest text-slate-400 dark:text-slate-500">
              Trợ lý Pháp luật
            </p>
          </div>
        </div>

        {/* Nhãn tĩnh, không phải bộ chọn: hệ thống chỉ dùng một mô hình duy nhất. */}
        <div className="hidden items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-indigo-700 shadow-sm dark:border-slate-700 dark:bg-slate-900 dark:text-indigo-300 sm:flex">
          <Sparkles className="h-3.5 w-3.5" />
          {MODEL_LABEL}
        </div>

        {/* Right side */}
        <div className="flex items-center gap-2">
          {/* Avatar dropdown */}
          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setOpen((v) => !v)}
              className="flex items-center gap-2 rounded-full border border-slate-200 bg-white py-1 pl-1 pr-2.5 text-sm transition hover:border-indigo-300 hover:shadow-sm dark:border-slate-700 dark:bg-slate-900 dark:hover:border-indigo-700"
              aria-haspopup="menu"
              aria-expanded={open}
            >
              <span className="brand-gradient flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold text-white">
                {initial}
              </span>
              <span className="hidden max-w-[120px] truncate text-slate-700 sm:inline dark:text-slate-200">
                {displayName}
              </span>
              <ChevronDown
                className={`h-3.5 w-3.5 text-slate-400 transition ${open ? "rotate-180" : ""}`}
              />
            </button>

            {open && (
              <div
                role="menu"
                className="animate-fade-up absolute right-0 mt-2 w-64 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-800 dark:bg-slate-900"
              >
                {/* User info */}
                <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-800">
                  <div className="flex items-center gap-2.5">
                    <span className="brand-gradient flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white">
                      {initial}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100">
                        {displayName}
                      </p>
                      <p className="truncate text-xs text-slate-500 dark:text-slate-400">
                        {user?.email}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Menu items */}
                <div className="py-1">
                  <button
                    role="menuitem"
                    disabled
                    className="flex w-full items-center gap-2.5 px-4 py-2 text-left text-sm text-slate-400 dark:text-slate-500"
                  >
                    <UserIcon className="h-4 w-4" />
                    Thông tin tài khoản
                    <span className="ml-auto text-[10px] text-slate-400"></span>
                  </button>
                  <button
                    role="menuitem"
                    onClick={() => {
                      setOpen(false);
                      setPwdOpen(true);
                    }}
                    className="flex w-full items-center gap-2.5 px-4 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50 dark:text-slate-200 dark:hover:bg-slate-800"
                  >
                    <KeyRound className="h-4 w-4" />
                    Đổi mật khẩu
                  </button>
                </div>

                <div className="border-t border-slate-200 py-1 dark:border-slate-800">
                  <button
                    role="menuitem"
                    onClick={() => {
                      setOpen(false);
                      handleLogout();
                    }}
                    className="flex w-full items-center gap-2.5 px-4 py-2 text-left text-sm text-red-600 transition hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/30"
                  >
                    <LogOut className="h-4 w-4" />
                    Đăng xuất
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <ChangePasswordModal open={pwdOpen} onClose={() => setPwdOpen(false)} />
    </>
  );
}
