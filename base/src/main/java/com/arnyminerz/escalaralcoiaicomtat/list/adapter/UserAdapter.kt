package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.FriendSearchActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.FriendshipStatus
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.list.holder.UserHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber

@ExperimentalUnsignedTypes
class UserAdapter(
    private val friendSearchActivity: FriendSearchActivity,
    private val loggedUser: UserData,
    private val users: List<UserData>
) : RecyclerView.Adapter<UserHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder =
        UserHolder(
            LayoutInflater.from(friendSearchActivity)
                .inflate(R.layout.list_item_user, parent, false)
        )

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val user = users[position]

        holder.addFriendButton.isEnabled = false
        holder.textView.text = user.username

        GlobalScope.launch {
            try {
                val friendshipStatus =
                    loggedUser.friendWith(friendSearchActivity.networkState, user)
                friendSearchActivity.runOnUiThread {
                    visibility(holder.addingProgressBar, false)
                    when (friendshipStatus) {
                        FriendshipStatus.FRIENDS -> {
                            holder.addFriendButton.setText(R.string.status_friend)
                            holder.addFriendButton.isEnabled = false
                        }
                        FriendshipStatus.REQUESTED -> {
                            holder.addFriendButton.setText(R.string.status_requested)
                            holder.addFriendButton.isEnabled = false
                        }
                        FriendshipStatus.NOT_FRIENDS -> {
                            holder.addFriendButton.isEnabled = true
                            holder.addFriendButton.setOnClickListener {
                                GlobalScope.launch {
                                    try {
                                        user.friendRequest(
                                            friendSearchActivity.networkState,
                                            loggedUser
                                        )

                                        friendSearchActivity.runOnUiThread {
                                            visibility(holder.addingProgressBar, false)
                                            friendSearchActivity.toast(R.string.toast_friend_request_sent)
                                            holder.addFriendButton.setText(R.string.status_sent)
                                            holder.addFriendButton.isEnabled = false
                                        }
                                    } catch (error: Exception) {
                                        friendSearchActivity.runOnUiThread {
                                            visibility(holder.addingProgressBar, false)
                                            holder.addFriendButton.isEnabled = false
                                            Timber.e(
                                                error,
                                                "Could not check if users are friend"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                friendSearchActivity.runOnUiThread {
                    visibility(holder.addingProgressBar, false)
                    friendSearchActivity.toast(R.string.toast_error_internal)
                    Timber.e(error, "Could not check if users are friend")
                }
            }
        }
    }
}