package com.arnyminerz.escalaralcoiaicomtat.data.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.work.*
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.climb.data.CompletedPath
import com.arnyminerz.escalaralcoiaicomtat.exception.*
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.UserNotAuthenticableException
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.UserNotFoundException
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.WrongPasswordException
import com.arnyminerz.escalaralcoiaicomtat.generic.*
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getBooleanFromString
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.getStringSafe
import com.arnyminerz.escalaralcoiaicomtat.network.base.ConnectivityProvider
import com.arnyminerz.escalaralcoiaicomtat.security.decrypt
import com.arnyminerz.escalaralcoiaicomtat.security.encrypt
import com.arnyminerz.escalaralcoiaicomtat.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toCollection
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.Serializable
import java.lang.Exception

@Suppress("unused")
data class UserData(
    val id: Int,
    val uid: String,
    val role: UserRole,
    val username: String,
    val email: String,
    private var profileImageUrl: String?
) : Serializable {
    /**
     * The FirebaseMessaging user's topic.
     * Alias to uid
     */
    val topic = uid

    var profileImage: String? = profileImageUrl
        private set

    @Suppress("unused")
    companion object {
        private const val QR_ENCRYPTION_KEY = "ZXT9KGwMJQpFNMEu"

        private val loadedUsers = hashMapOf<String, UserData>()

        /**
         * Gets an user's data from its UID
         * @author ArnyminerZ
         * @date 2020/05/29
         * @patch ArnyminerZ 2020/05/31
         * @patch ArnyminerZ 2020/06/06
         * @param networkState The status of the device's network on the function execution time.
         * @param uid The uid of the user to check
         * @returns The user Data
         * @throws NoInternetAccessException If no internet access was provided from networkState
         * @throws UserNotFoundException If the uid specified didn't match any stored user
         * @throws JSONResultException If there was an error loading the data
         */
        @Throws(
            NoInternetAccessException::class,
            UserNotFoundException::class,
            JSONResultException::class
        )
        suspend fun fromUID(
            networkState: ConnectivityProvider.NetworkState,
            uid: String
        ): UserData {
            if (loadedUsers.containsKey(uid)) {
                Timber.d("Returning user ($uid) from cache.")
                return loadedUsers[uid]!!
            } else {
                if (!networkState.hasInternet)
                    throw NoInternetAccessException()

                val json = jsonFromUrl("$EXTENDED_API_URL/user/%s".format(uid))

                if (json.has("error"))
                    if (json.getString("error") == "user_not_found")
                        throw UserNotFoundException()
                    else
                        throw JSONResultException(
                            json,
                            "Unhandled error happened while finding for user"
                        )
                else {
                    val userJson = json.getJSONArray("data").getJSONObject(0)
                    val userData = UserData(userJson)
                    loadedUsers[uid] = userData
                    return userData
                }
            }
        }

        /**
         * Requests User's data from its QR code
         * @patch ArnyminerZ 2020/06/06
         * @param content The content text of the QR
         * @param networkState The status of the device's network on the function execution time.
         * @returns The user Data
         * @throws NoInternetAccessException If no internet access was provided from networkState
         * @throws UserNotFoundException If the uid specified didn't match any stored user
         * @throws JSONResultException If there was an error loading the data
         */
        @Throws(
            NoInternetAccessException::class,
            UserNotFoundException::class,
            JSONResultException::class
        )
        suspend fun fromQR(
            networkState: ConnectivityProvider.NetworkState,
            content: String
        ): UserData =
            fromUID(networkState, JSONObject(content.decrypt(QR_ENCRYPTION_KEY)).getString("uid"))

        fun checkValidJson(json: JSONObject): Boolean =
            json.has("id") && json.has("uid") && json.has("username") && json.has("email") && json.has(
                "profileImage"
            )

        /**
         * Sends a friend request without the UserData being initialized
         * @patch ArnyminerZ 2020/06/06
         * @param networkState The status of the device's network on the function execution time.
         * @param from The uid of the user that the request's from
         * @param to The uid of the user to send the request
         * @throws NoInternetAccessException If no internet access was detected from the network state
         * @return If executed correctly
         */
        @Throws(NoInternetAccessException::class)
        suspend fun sendFriendRequest(
            networkState: ConnectivityProvider.NetworkState,
            from: String,
            to: String
        ): Boolean {
            if (!networkState.hasInternet)
                throw NoInternetAccessException()

            val result = jsonFromUrl("$EXTENDED_API_URL/user/${from}/friend/request/$to")
            return !result.has("error")
        }

        /**
         * Will login user, and if it doesn't exist, create it
         * @author ArnyminerZ
         * @date 2020/05/31
         * @patch ArnyminerZ 2020/06/06
         * @param context The context where the login was called from
         * @param username The username of the user
         * @param password The password for identifying the email
         * @throws WrongPasswordException When the introduced password is wrong
         * @throws UserNotFoundException When the user doesn't exist
         * @throws UserNotAuthenticableException When the user doesn't have a password
         * @returns a User Data
         */
        @ExperimentalUnsignedTypes
        @Throws(
            WrongPasswordException::class,
            UserNotFoundException::class,
            UserNotAuthenticableException::class
        )
        suspend fun login(
            context: Context,
            username: String,
            password: String
        ): UserData {
            val loginWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<LoginJob>()
                .setInputData(
                    workDataOf(
                        DATA_USERNAME to username,
                        DATA_PASSWORD to password
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("login")
                .build()
            val wm = WorkManager.getInstance(context)
            wm.enqueue(loginWorkRequest)
            Timber.d("Enqueued login")
            var workInfo: ListenableFuture<WorkInfo>
            var result: WorkInfo
            while (true){
                workInfo = wm.getWorkInfoById(loginWorkRequest.id)
                result = workInfo.await()
                if(result.state.isFinished)
                    break
            }

            if (result.state != WorkInfo.State.SUCCEEDED) {
                val error = result.outputData.getString(DATA_ERROR)
                if (error != null)
                    when(error){
                        "wrong-password" -> throw WrongPasswordException()
                        "user-not-found" -> throw UserNotFoundException()
                        "user-has-no-password" -> throw UserNotAuthenticableException()
                    }
                throw Exception("Could not log in. State: ${result.state}")
            }
            val resultData = result.outputData.getStringArray(RESULT_DATA)
            if (resultData != null)
                for (ln in resultData.indices)
                    Timber.v(resultData[ln])
            return UserData("")
        }
    }

    constructor(json: JSONObject) : this(
        json.getInt("id"),
        json.getString("uid"),
        UserRole.find(json.getInt("role"))!!,
        json.getString("username"),
        json.getString("email"),
        json.getStringSafe("profileImage")
    )

    constructor(json: String) : this(JSONObject(json))

    @Suppress("MemberVisibilityCanBePrivate")
    fun toJSON() =
        JSONObject().apply {
            put("id", id)
            put("uid", uid)
            put("role", role.key)
            put("username", username)
            put("email", email)
            put("profileImage", profileImage)
        }

    fun qrContent(encrypt: Boolean = true): String =
        toJSON().toString().let {
            if (encrypt)
                it.encrypt(QR_ENCRYPTION_KEY)
            else it
        }

    /**
     * Changes the user's profile image
     * @author ArnyminerZ
     * @date 2020/05/28
     * @patch ArnyminerZ 2020/06/06
     * @param url The new user's profile image URL
     * @param networkState The status of the device's network on the function execution time.
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @throws JSONResultException If an error occurred in the request
     * @returns The new image's URL
     */
    @Throws(NoInternetAccessException::class, JSONResultException::class)
    suspend fun updateProfileImage(
        networkState: ConnectivityProvider.NetworkState,
        url: String
    ): String {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val json = jsonFromUrl("$EXTENDED_API_URL/user/$uid/change_image/?url=$url")
        if (json.has("error"))
            throw JSONResultException(json.getError(), "Could not change image.")
        else {
            if (loadedUsers.containsKey(uid))
                loadedUsers[uid]!!.profileImage = url

            return url
        }
    }

    /**
     * Fetches all the paths the user has completed.
     * @author ArnyminerZ
     * @date 2020/05/28
     * @patch ArnyminerZ 2020/06/06
     * @param count The amount of paths to get
     * @param networkState The status of the device's network on the function execution time.
     * @param sort The order to sort the paths
     * @returns a flow of Completed Paths
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun completedPaths(
        networkState: ConnectivityProvider.NetworkState,
        count: Int? = null,
        sort: CompletedPathSortOrder? = null
    ): Flow<CompletedPath> = flow {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val json = jsonFromUrl(
            "$EXTENDED_API_URL/user/%s/completed_paths/?n=n%s%s".format( // ?n=n is for simplifying the change between ? and & for GET params
                uid,
                if (count == null) "" else "&max=$count",
                if (sort == null) "" else "&sort=${sort.tag}"
            )
        )
        val completedPaths = json.getJSONArray("data")

        for (o in 0 until completedPaths.length()) {
            val obj = completedPaths.getJSONObject(o)

            val completedPath = CompletedPath.fromDB(obj)
            if (completedPath == null)
                throw JSONException("Path could not be found. JSON: $obj")
            else
                emit(completedPath)
        }
    }

    /**
     * Fetches all the friends the user has.
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param count The amount of friends to get
     * @param networkState The status of the device's network on the function execution time.
     * @returns a flow of User Data
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun friends(
        networkState: ConnectivityProvider.NetworkState,
        count: Int = 5
    ): Flow<UserData> = flow {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val json = jsonFromUrl(
            "$EXTENDED_API_URL/user/%s/friends/?max=%d".format(
                uid,
                count
            )
        )
        val friendsArray = json.getJSONArray("data")

        for (f in 0 until friendsArray.length())
            emit(friendsArray.getJSONObject(f).getUserData("user"))
    }

    /**
     * Gets all the friend requests the user has
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @returns a flow of Friend Request
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun friendRequests(
        networkState: ConnectivityProvider.NetworkState
    ): Flow<FriendRequest> = flow {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val json = jsonFromUrl("$EXTENDED_API_URL/user/$uid/friend/requests")
        if (json.has("error")) throw JSONResultException(
            json.getError(),
            "Could not get friend requests"
        )

        val users = json.getJSONArray("data")
        val requestsCount = users.length()
        Timber.v("Got %d friend requests", requestsCount)
        if (users.isNotEmpty())
            for (c in 0 until requestsCount) {
                val userData = users.getJSONObject(c)
                val userUUID = userData.getString("from_user")
                val requestUUID = userData.getString("uuid")

                val user = fromUID(networkState, userUUID)
                emit(FriendRequest(requestUUID, user, this@UserData))
            }
    }

    /**
     * Checks if the user is friend with other user
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param user The uid of the user to check
     * @param networkState The status of the device's network on the function execution time.
     * @returns a FriendshipStatus
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @returns a LoadResult
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun friendWith(
        networkState: ConnectivityProvider.NetworkState,
        user: String
    ): FriendshipStatus {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val json = jsonFromUrl(
            "$EXTENDED_API_URL/user/%s/friend_with/%s".format(
                uid,
                user
            )
        )

        val friends = json.getBoolean("friends")
        val requested = json.getBoolean("requested")
        return if (friends) FriendshipStatus.FRIENDS else if (requested) FriendshipStatus.REQUESTED else FriendshipStatus.NOT_FRIENDS
    }

    /**
     * Checks if the user is friend with other user
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param user The user to check
     * @param networkState The status of the device's network on the function execution time.
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @returns a FriendshipStatus
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun friendWith(
        networkState: ConnectivityProvider.NetworkState,
        user: UserData
    ): FriendshipStatus = friendWith(networkState, user.uid)

    /**
     * Requests a user to be friend with
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @param fromUser The user that requests the friendship
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @returns if the task ran successfully
     */
    @Throws(NoInternetAccessException::class)
    suspend fun friendRequest(
        networkState: ConnectivityProvider.NetworkState,
        fromUser: UserData
    ): Boolean = friendRequest(networkState, fromUser.uid)

    /**
     * Requests a user to be friend with
     * @author ArnyminerZ
     * @date 2020/05/29
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @param fromUid The uid of the user that requests the friendship
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @returns if the task ran successfully
     */
    @Throws(NoInternetAccessException::class)
    suspend fun friendRequest(
        networkState: ConnectivityProvider.NetworkState,
        fromUid: String
    ): Boolean = sendFriendRequest(networkState, fromUid, uid)

    /**
     * Removes the relation of friends between the current user and another
     * @author ArnyminerZ
     * @date 2020/06/09
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @param friendUser The user to remove
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @return if the task ran successfully
     */
    @Throws(NoInternetAccessException::class)
    suspend fun removeFriend(
        networkState: ConnectivityProvider.NetworkState,
        friendUser: UserData
    ): Boolean {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val response = jsonFromUrl("$EXTENDED_API_URL/user/$uid/friend/delete/${friendUser.uid}")
        val data = response.getJSONObject("data")

        return data.getInt("affectedRows") > 0
    }

    /**
     * Changes the username of the user
     * @author ArnyminerZ
     * @date 2020/06/09
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @param username The new username
     * @throws NoInternetAccessException If no internet access was detected from network state
     * @return if the task san successfully
     */
    @Throws(NoInternetAccessException::class)
    suspend fun updateUsername(
        networkState: ConnectivityProvider.NetworkState,
        username: String
    ): Boolean {
        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        val response = jsonFromUrl("$EXTENDED_API_URL/user/$uid/change_username/$username")
        val data = response.getJSONObject("data")

        return data.getInt("affectedRows") > 0
    }

    /**
     * Retrieves the profile image of a user
     * @author ArnyminerZ
     * @date 2020/06/10
     * @patch ArnyminerZ 2020/06/06
     * @patch ArnyminerZ 2020/09/05
     * @param context The context to load from
     * @param networkState The status of the device's network on the function execution time.
     * @param callback The function that will be called when the bitmap is loaded
     * @return a LoadResult with a SerializableBitmap
     * @throws MissingDataException if there isn't any stored profile image
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(MissingDataException::class, NoInternetAccessException::class)
    fun profileImage(
        context: Context,
        networkState: ConnectivityProvider.NetworkState,
        callback: (bitmap: Bitmap) -> Unit
    ) {
        if (profileImage == null)
            throw MissingDataException("No profile image stored")

        if (!networkState.hasInternet)
            throw NoInternetAccessException()

        Glide.with(context)
            .asBitmap()
            .load(profileImage!!)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    /**
     * Retrieves the personal preferences of a user
     * @author ArnyminerZ
     * @date 2020/06/10
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The status of the device's network on the function execution time.
     * @return the User Preferences
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @ExperimentalUnsignedTypes
    @Throws(NoInternetAccessException::class)
    suspend fun preferences(
        networkState: ConnectivityProvider.NetworkState
    ): UserPreferences {
        if (!networkState.hasInternet) throw NoInternetAccessException()

        val json = jsonFromUrl("$EXTENDED_API_URL/user/$uid")
        val data = json.getJSONArray("data").getJSONObject(0)

        return UserPreferences(
            this@UserData,
            data.getInt("pref_friendsPublic") == 1,
            data.getInt("pref_profilePhotoPublic") == 1,
            data.getInt("pref_completedPublic") == 1
        )
    }

    /**
     * Checks if a user has liked a completed path
     * @author ArnyminerZ
     * @data 2020/06/24
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The current network state
     * @param completedPathId The completed path id
     * @return is the user has liked a completed path
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(NoInternetAccessException::class)
    suspend fun likedCompletedPath(
        networkState: ConnectivityProvider.NetworkState,
        completedPathId: Int
    ): Boolean {
        if (!networkState.hasInternet) throw NoInternetAccessException()

        val json =
            jsonFromUrl("$EXTENDED_API_URL/completed_paths/$completedPathId/liked/$uid")

        return json.getBooleanFromString("liked")
    }

    /**
     * Likes a path
     * @author ArnyminerZ
     * @data 2020/06/24
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The current network state
     * @param completedPathId The completed path id
     * @return if the task executed successfully
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(NoInternetAccessException::class)
    suspend fun likeCompletedPath(
        networkState: ConnectivityProvider.NetworkState,
        completedPathId: Int
    ): Boolean {
        if (!networkState.hasInternet) throw NoInternetAccessException()

        val json =
            jsonFromUrl("$EXTENDED_API_URL/completed_paths/$completedPathId/like/$uid")

        return !json.has("error")
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + uid.hashCode()
        result = 31 * result + role.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + (profileImageUrl?.hashCode() ?: 0)
        result = 31 * result + topic.hashCode()
        result = 31 * result + (profileImage?.hashCode() ?: 0)
        return result
    }
}

/**
 * Establishes de order for sorting a completed path list
 */
@Suppress("unused")
enum class CompletedPathSortOrder(val tag: String) {
    /**
     * Sorts by the timestamp ascending
     */
    DATE_ASC("date_asc"),

    /**
     * Sorts by the timestamp descending
     */
    DATE_DES("date_desc")
}

/**
 * Tells the friendship status of a user
 */
enum class FriendshipStatus(val index: Int) {
    /**
     * The users aren't friends
     */
    NOT_FRIENDS(0),

    /**
     * Some of the users has requested the other to be friends. (There's a friend request in course)
     */
    REQUESTED(1),

    /**
     * The users are friends
     */
    FRIENDS(2);

    val areFriends
        get() = this == FRIENDS
}

@ExperimentalUnsignedTypes
@Suppress("unused")
class UserPreferences(
    private val user: UserData,
    friendsPublic: Boolean,
    profileImagePublic: Boolean,
    completedPathsPublic: Boolean
) : Serializable {
    var friendsPublic: Boolean = friendsPublic
        private set
    var profileImagePublic: Boolean = profileImagePublic
        private set
    var completedPathsPublic: Boolean = completedPathsPublic
        private set

    /**
     * Updates the preference of a user
     * @author ArnyminerZ
     * @data 2020/06/10
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The current network state
     * @param public If the profile image should be public
     * @return if the preference was updated successfully
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(NoInternetAccessException::class)
    suspend fun updateProfileImagePublic(
        networkState: ConnectivityProvider.NetworkState,
        public: Boolean
    ): Boolean {
        profileImagePublic = public

        if (!networkState.hasInternet) throw NoInternetAccessException()

        val uid = user.uid
        val json =
            jsonFromUrl("$EXTENDED_API_URL/user/$uid/config/profilePhotoPublic/" + public.toInt())

        return when {
            json.has("error") -> false
            json.getJSONObject("data").getInt("affectedRows") > 0 -> true
            else -> throw JSONResultException(
                json.getError(),
                "Could not change profilePhotoPublic"
            )
        }
    }

    /**
     * Updates the preference of a user
     * @author ArnyminerZ
     * @data 2020/06/10
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The current network state
     * @param public If the user's completed paths should be public
     * @return if the preference was updated successfully
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(NoInternetAccessException::class)
    suspend fun updateCompletedPublic(
        networkState: ConnectivityProvider.NetworkState,
        public: Boolean
    ): Boolean {
        completedPathsPublic = public

        if (!networkState.hasInternet) throw NoInternetAccessException()

        val uid = user.uid
        val json =
            jsonFromUrl("$EXTENDED_API_URL/user/$uid/config/completedPublic/" + public.toInt())

        return when {
            json.has("error") -> false
            json.getJSONObject("data").getInt("affectedRows") > 0 -> true
            else -> throw JSONResultException(json.getError(), "Could not change completedPublic")
        }
    }

    /**
     * Updates the preference of a user
     * @author ArnyminerZ
     * @data 2020/06/10
     * @patch ArnyminerZ 2020/06/06
     * @param networkState The current network state
     * @param public If the user's friends should be public
     * @return if the preference was updated successfully
     * @throws NoInternetAccessException If no internet access was detected from network state
     */
    @Throws(NoInternetAccessException::class)
    suspend fun updateFriendsPublic(
        networkState: ConnectivityProvider.NetworkState,
        public: Boolean
    ): Boolean {
        friendsPublic = public

        if (!networkState.hasInternet) throw NoInternetAccessException()

        val uid = user.uid
        val json =
            jsonFromUrl("$EXTENDED_API_URL/user/$uid/config/friendsPublic/" + public.toInt())

        return when {
            json.has("error") -> false
            json.getJSONObject("data").getInt("affectedRows") > 0 -> true
            else -> throw JSONResultException(json.getError(), "Could not change friendsPublic")
        }
    }

    /**
     * If friends should be shown
     * @author ArnyminerZ
     * @date 2020/06/17
     * @patch ArnyminerZ 2020/06/06
     * @param otherUser The user that will see the friends.
     * @param networkState The current network state
     * @return If the friends should be shown
     */
    @Throws(NoInternetAccessException::class)
    suspend fun shouldShowFriends(
        otherUser: UserData,
        networkState: ConnectivityProvider.NetworkState
    ): Boolean {
        if (!networkState.hasInternet) throw NoInternetAccessException()

        val friendsFlow = otherUser.friends(networkState)
        val friends = arrayListOf<UserData>()
        friendsFlow.toCollection(friends)

        return friendsPublic || friends.contains(otherUser)
    }

    fun shouldShowProfileImage(): Boolean = profileImagePublic
    fun shouldShowCompletedPaths(): Boolean = completedPathsPublic
}