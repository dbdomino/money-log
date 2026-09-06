/**
 * Request/response AOP logging and sensitive value masking.
 *
 * <p>This package knows nothing about the database. Constitution principle IV requires
 * the backend to log the whole request-to-response span through AOP rather than through
 * per-controller entry/exit statements, and to mask passwords and tokens.
 *
 * <p>The aspect receives already-deserialized DTOs, so it does not need to buffer the
 * servlet request body the way a filter-based implementation would.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/api-contract.md">api-contract.md</a>
 */
package com.dbdomino.moneylog.common.logging;
