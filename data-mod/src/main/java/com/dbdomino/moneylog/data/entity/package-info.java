/**
 * JPA entities for the backend API persistence layer.
 *
 * <p>Table naming: {@code tbl_user} plus {@code tbl_user_*} for every member-owned
 * storage unit. Legacy {@code money-app} table names are never reused.
 *
 * <p>Primary key: {@code idx} on every entity, except {@code tbl_user} which uses
 * {@code id_key}. Member-owned entities reference the member through {@code id_key}
 * only — never the login id ({@code user_id}).
 *
 * <p>All entities extend {@link com.dbdomino.moneylog.data.entity.BaseAuditEntity}
 * for the four audit columns.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md</a>
 */
package com.dbdomino.moneylog.data.entity;
