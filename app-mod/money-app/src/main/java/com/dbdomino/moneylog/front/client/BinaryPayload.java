package com.dbdomino.moneylog.front.client;

/**
 * 본문이 파일 그 자체인 응답. 지출유형 아이콘 조회(003 의 2.10)와 엑셀 양식 다운로드
 * (004 의 3.11) 둘만 이 형태로 온다.
 *
 * <p>이 둘은 봉투로 감싸이지 않는다. 무조건 봉투로 읽으면 이미지 바이트를 JSON 으로
 * 파싱하려다 깨진다.
 *
 * @param bytes 응답 본문 바이트
 * @param contentType 백엔드가 준 Content-Type. 브라우저로 그대로 넘긴다
 * @param filename Content-Disposition 에서 뽑은 파일명. 없으면 {@code null}
 */
public record BinaryPayload(byte[] bytes, String contentType, String filename) {
}
