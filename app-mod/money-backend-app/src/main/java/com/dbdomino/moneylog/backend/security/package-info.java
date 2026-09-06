/**
 * Token verification against the database.
 *
 * <p>Every piece of authentication that reads {@code tbl_user_session} lives here:
 * the request filter that runs the ten-step check, the principal placed into the
 * {@code SecurityContext}, and the entry point / denied handler that reshape Spring
 * Security's default 401 and 403 bodies into {@code { resCode, data }}.
 *
 * <p>JWT signing and parsing alone live in {@code common-mod} — that code touches no
 * database. The split keeps {@code common-mod} free of {@code data-mod}.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/auth-pipeline.md">auth-pipeline.md</a>
 */
package com.dbdomino.moneylog.backend.security;
