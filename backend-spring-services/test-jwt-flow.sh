#!/usr/bin/env bash
# Test luồng JWT RS256 + JWKS (Trường phái B - zero-trust).
# Yêu cầu: docker (postgres+redis) đang chạy, và 3 service đã start:
#   auth-service :8081  |  api-gateway :8080  |  chat-service :8082
set -euo pipefail

GW=http://localhost:8080

echo "== 1. JWKS endpoint (public key) =="
curl -s http://localhost:8081/.well-known/jwks.json | head -c 300; echo; echo

echo "== 2. Register =="
curl -s -X POST "$GW/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@chatbox.ai","password":"secret123","fullName":"Demo"}' | tee /tmp/reg.json; echo

TOKEN=$(sed -E 's/.*"accessToken":"([^"]+)".*/\1/' /tmp/reg.json)
echo "TOKEN=${TOKEN:0:40}..."; echo

echo "== 3. /me qua gateway (token hợp lệ -> 200) =="
curl -s -o /dev/null -w "status=%{http_code}\n" "$GW/api/auth/me" -H "Authorization: Bearer $TOKEN"

echo "== 4. /me KHÔNG token (-> 401) =="
curl -s -o /dev/null -w "status=%{http_code}\n" "$GW/api/auth/me"

echo "== 5. Chống spoof: header X-User-Id giả + KHÔNG token, vào endpoint cần auth (-> 401) =="
echo "   -> gateway phải bỏ qua header giả, không cho giả danh user 999"
curl -s -o /dev/null -w "status=%{http_code}\n" "$GW/api/auth/me" -H "X-User-Id: 999" -H "X-User-Role: ADMIN"

echo "== 5b. Token hợp lệ + X-User-Id giả=999 -> /me phải trả id THẬT của token, không phải 999 =="
curl -s "$GW/api/auth/me" -H "Authorization: Bearer $TOKEN" -H "X-User-Id: 999" | sed -E 's/.*("id":[0-9]+).*/   \1 (da strip header gia)/'

echo "== 6. Token rác (-> 401) =="
curl -s -o /dev/null -w "status=%{http_code}\n" "$GW/api/auth/me" -H "Authorization: Bearer abc.def.ghi"

echo "== 7. ai-service zero-trust: gọi /api/ai/rag KHÔNG token (-> 401, không chạy LLM) =="
curl -s -o /dev/null -w "status=%{http_code}\n" -X POST "$GW/api/ai/rag" \
  -H 'Content-Type: application/json' -d '{"question":"test"}'

echo "== 8. ai-service /health public (-> 200) =="
curl -s -o /dev/null -w "status=%{http_code}\n" "$GW/api/ai/health"
