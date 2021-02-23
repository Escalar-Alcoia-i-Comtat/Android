package com.arnyminerz.escalaralcoiaicomtat.data.user

import com.arnyminerz.escalaralcoiaicomtat.exception.auth.token.RenovationNotAllowedException

const val TOKEN_EXPIRATION_TIME = 10 * 60 * 1000

class AuthToken(token: String, private val creationTime: Long = System.currentTimeMillis()) {
    var token: String = token
        private set

    /**
     * Checks if the token has expired.
     * @return Whether or not the token has expired
     */
    val isExpired: Boolean
        get() {
            return System.currentTimeMillis() - creationTime >= TOKEN_EXPIRATION_TIME
        }

    /**
     * Renovates the auth token. Only authorised for expired tokens.
     * @throws RenovationNotAllowedException When trying to renovate a non-expired token.
     */
    @Throws(RenovationNotAllowedException::class)
    suspend fun renovate(): AuthToken {
        if (!isExpired)
            throw RenovationNotAllowedException("Token not expired")
        throw NotImplementedError()
    }
}