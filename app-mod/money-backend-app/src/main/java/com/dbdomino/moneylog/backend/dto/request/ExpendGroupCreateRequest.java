package com.dbdomino.moneylog.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 2.7 지출유형 등록의 폼 필드.
 *
 * <p>요청이 {@code multipart/form-data} 라 JSON Body 가 아니다 — 아이콘을 파트로 함께
 * 받기 때문이다. {@code iconFile} 은 여기 넣지 않는다: 파일 파트는 Controller 가
 * {@code MultipartFile} 로 따로 받고, 이 레코드는 값 필드만 담는다.
 *
 * <p><b>{@code defaultGroup} 을 받지 않는다.</b> 받으면 사용자가 임의로 기본 유형을 만들어
 * {@code 3107}(삭제 불가)·{@code 3105}(이름 변경 불가) 보호를 스스로에게 걸 수 있다.
 * 기본 10종은 002 의 가입 흐름만 만든다(api-contract.md §6).
 *
 * <p>소유자도 받지 않는다. 유형의 주인은 <b>토큰이 지시하는 {@code id_key}</b> 이며 요청이
 * 지정할 수 없다(FR-201).
 *
 * @param name  유형 이름. 같은 회원 안에서 유일해야 하며 중복은 {@code 3101} 이다
 * @param inUse 사용 여부. {@code false} 면 등록은 되지만 사용 중 목록(2.13)에서 빠진다
 */
public record ExpendGroupCreateRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull Boolean inUse) {
}
