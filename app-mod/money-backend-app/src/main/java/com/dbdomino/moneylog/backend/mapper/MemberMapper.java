package com.dbdomino.moneylog.backend.mapper;

import com.dbdomino.moneylog.backend.dto.response.MemberResponse;
import com.dbdomino.moneylog.backend.dto.response.SignupResponse;
import com.dbdomino.moneylog.data.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code User} Entity ↔ 응답 DTO 변환.
 *
 * <p>변환만 한다. 비즈니스 규칙은 서비스에 둔다(헌장 원칙 II).
 *
 * <p>Entity 의 {@code userId} 가 API 의 {@code memberId} 다. 이름이 다른 이유는
 * 저장 구조가 소유자 키를 {@code id_key} 로 부르고 로그인 아이디를 {@code user_id} 로
 * 구분하기 때문이며, 그 구분은 API 밖으로 나가지 않는다.
 *
 * <p>{@code pw} 를 매핑하지 않는다 — 응답 타입에 그 필드가 아예 없어서 매핑할 대상도 없다.
 * MapStruct 는 대상에 없는 원본 필드를 조용히 버리므로, 비밀번호가 새어 나가려면 응답
 * 타입에 필드를 <b>일부러 추가</b>해야 한다.
 */
@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "memberId", source = "userId")
    MemberResponse toMemberResponse(User user);

    @Mapping(target = "memberId", source = "userId")
    SignupResponse toSignupResponse(User user);
}
