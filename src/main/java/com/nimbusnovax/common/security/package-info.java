/**
 * BFF against the real NimbusAuth (Spring Authorization Server): SecurityConfig has two chains —
 * stateless resource server for /api/** (JWT via JWKS) and stateful oauth2Login for /bff/**
 * (session cookie, CSRF). See the bff subpackage for the login/me/logout endpoints.
 */
package com.nimbusnovax.common.security;
