/**
 * Spring Data JPA repositories for the backend API persistence layer.
 *
 * <p>Repositories are reached only through the service layer of
 * {@code money-backend-app} — controllers never call them directly.
 *
 * <p>Scanned via {@code @EnableJpaRepositories} on the backend application class;
 * this package sits outside the application's {@code scanBasePackages} root.
 *
 * @see <a href="../../../../../../../../specs/001-backend-db-schema/data-model.md">data-model.md</a>
 */
package com.dbdomino.moneylog.data.repository;
