/**
 * File-system storage for expend-group icons.
 *
 * <p>Icons are the only resource in this project that lives outside the database. The
 * database stores just the file name; the lookup path is prefixed when a response is
 * built, so changing the base URL does not require rewriting rows.
 *
 * @see <a href="../../../../../../../../specs/003-backend-payment-expend-group/contracts/icon-storage.md">icon-storage.md</a>
 */
package com.dbdomino.moneylog.backend.storage;
