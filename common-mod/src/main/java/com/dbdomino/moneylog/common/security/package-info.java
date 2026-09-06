/**
 * JWT signing and parsing.
 *
 * <p>This package knows nothing about the database. Only the parts of authentication
 * that operate on strings live here; everything that reads or writes
 * {@code tbl_user_session} lives in {@code money-backend-app} instead
 * ({@code MemberSessionService}, {@code TokenAuthenticationFilter}).
 *
 * <p>Putting the session code here would create a {@code common-mod -> data-mod}
 * dependency. {@code common-mod} is the lowest shared module and the front end depends
 * on it too, so JPA arriving here would hand the front end a database layer — exactly
 * what constitution principle I exists to prevent.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md</a>
 */
package com.dbdomino.moneylog.common.security;
