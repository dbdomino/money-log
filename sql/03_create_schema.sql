-- 스키마 생성 : moneylog (소유자 moneyloguser, moneylogdb로 로그인 후 수행한다.)
-- moneylogdb 에 접속한 상태에서 실행한다.
CREATE SCHEMA IF NOT EXISTS moneylog AUTHORIZATION moneyloguser;

-- moneyloguser 가 moneylogdb 에 접속할 때의 기본 검색 경로.
-- public 은 확장(extension) 객체 참조용으로 뒤에 남겨둔다.
ALTER ROLE moneyloguser IN DATABASE moneylogdb SET search_path TO moneylog, public;
