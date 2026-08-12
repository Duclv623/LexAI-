"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { getToken } from "@/lib/auth";

export default function RootRedirect() {
  const router = useRouter();

  useEffect(() => {
    const token = getToken();
    router.replace(token ? "/chat" : "/login");
  }, [router]);

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
