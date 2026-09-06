/**
 * Backend application configuration.
 *
 * <p>Holds the security filter chain, JWT property binding, and the auditor that feeds
 * {@code created_by}/{@code updated_by}. Datasource and JPA settings are NOT here — they
 * belong to {@code data-mod/src/main/resources/application-postgresql.yml}, which both
 * the application and the {@code data-mod} schema tests share.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/plan.md">plan.md</a>
 */
package com.dbdomino.moneylog.backend.config;
