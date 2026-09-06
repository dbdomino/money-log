/**
 * Use-case orchestration for the member and authentication APIs.
 *
 * <p>Controllers call these services; services call repositories. A controller never
 * reaches a repository directly (constitution principle II).
 *
 * <p>Token hashing is confined to {@code MemberSessionService} so the algorithm has a
 * single place to change.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/plan.md">plan.md</a>
 */
package com.dbdomino.moneylog.backend.service;
