package com.arnyminerz.escalaralcoiaicomtat.data.user

import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.exception.NoInternetAccessException
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider

data class FriendRequest(val uuid: String, val fromUser: UserData, val toUser: UserData) {
    /**
     * Accepts or rejects a friend request
     * @author ArnyminerZ
     * @date 2020/05/29
     * @param networkState The network state
     * @param accept If the request should be accepted (true) or denied (false)
     * @returns if the token could be consumed
     */
    suspend fun consume(
        networkState: ConnectivityProvider.NetworkState,
        accept: Boolean
    ): Boolean {
        if (!networkState.hasInternet) throw NoInternetAccessException()

        val json =
            jsonFromUrl("$EXTENDED_API_URL/user/friend/$uuid/${if (accept) "accept" else "deny"}")
        return json.has("result")
    }
}