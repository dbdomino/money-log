package com.dbdomino.moneylog.backend.dto.response;

/**
 * 돌려줄 것이 안내 문구뿐인 성공 응답. 1.6 로그아웃이 쓴다.
 *
 * <p>실패 응답의 {@code data.message} 와 형태가 같지만 성격이 다르다 — 그쪽은
 * {@code RestResponseDto.fail} 이 만드는 고정 형식이고, 이쪽은 성공 {@code data} 다.
 *
 * @param message 사용자에게 보여 줄 문구
 */
public record MessageResponse(String message) {
}
