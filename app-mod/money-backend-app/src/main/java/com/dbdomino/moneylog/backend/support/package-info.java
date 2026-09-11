/**
 * Value objects shared across services.
 *
 * <p>No Spring beans and no repository access. A type belongs here when the same
 * rule is needed by more than one service and getting it wrong in one of them
 * would fail silently.
 */
package com.dbdomino.moneylog.backend.support;
