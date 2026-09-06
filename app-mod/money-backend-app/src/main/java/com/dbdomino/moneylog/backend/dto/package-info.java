/**
 * Request and response shapes for the API boundary.
 *
 * <p>Entities never cross this boundary (constitution principle II). In particular no
 * response type carries a password field — SC-107 is enforced by the shape of these
 * classes, not by a check performed later.
 *
 * @see <a href="../../../../../../../../specs/002-backend-member-auth/contracts/api-contract.md">api-contract.md</a>
 */
package com.dbdomino.moneylog.backend.dto;
