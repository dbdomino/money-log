/**
 * Error codes, the business exception, and the global exception handler.
 *
 * <p>This package knows nothing about the database. It lives in {@code common-mod}
 * because the front end ({@code money-app}) must be able to read the same error code
 * list, and because {@code common-mod} must never depend on {@code data-mod} — that
 * direction is the reverse of the dependency rule in constitution principle I.
 *
 * <p>Business and validation failures are thrown as
 * {@link com.dbdomino.moneylog.common.error.BusinessException} carrying an
 * {@link com.dbdomino.moneylog.common.error.ErrorCode}. A bare {@code RuntimeException}
 * with a string message is not allowed (principle III).
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/api-contract.md">api-contract.md</a>
 */
package com.dbdomino.moneylog.common.error;
