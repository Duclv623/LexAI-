"""
Xác thực JWT theo mô hình zero-trust (Trường phái B).

Dù ai-service đứng sau api-gateway (gateway đã verify 1 lần), nó vẫn TỰ verify
token bằng public key lấy từ JWKS của auth-service — không tin tưởng traffic nội bộ,
và không giữ bất kỳ secret nào.
"""

import logging

import jwt
from jwt import PyJWKClient
from jwt.exceptions import PyJWKClientError, PyJWTError
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from config.config import JWKS_URL, JWT_ISSUER

logger = logging.getLogger("ai-service.auth")

# PyJWKClient tự fetch + cache public key theo kid, tự refetch khi key xoay.
_jwks_client = PyJWKClient(JWKS_URL)

# Lấy token từ header "Authorization: Bearer <token>".
# auto_error=False để tự trả 401 (mặc định HTTPBearer trả 403 khi thiếu header).
_bearer = HTTPBearer(auto_error=False)


def require_user(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
) -> dict:
    """
    Dependency bắt buộc token hợp lệ. Trả về identity đã xác thực.
    Gắn vào endpoint qua: Depends(require_user).
    """
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Thiếu bearer token",
        )
    token = credentials.credentials

    # 1. Lấy đúng public key (theo kid trong header token) từ JWKS.
    try:
        signing_key = _jwks_client.get_signing_key_from_jwt(token)
    except PyJWKClientError as e:
        logger.warning("Không lấy được key từ JWKS: %s", e)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Không xác thực được token (JWKS không khả dụng)",
        )

    # 2. Verify chữ ký RS256 + hạn (exp) + issuer.
    try:
        claims = jwt.decode(
            token,
            signing_key.key,
            algorithms=["RS256"],          # chỉ chấp nhận RS256, chặn tấn công alg=none
            issuer=JWT_ISSUER,
            options={"require": ["exp", "sub"]},
        )
    except PyJWTError as e:
        logger.info("Token không hợp lệ: %s", e)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token không hợp lệ",
        )

    return {
        "id": claims["sub"],
        "email": claims.get("email"),
        "role": claims.get("role"),
    }
