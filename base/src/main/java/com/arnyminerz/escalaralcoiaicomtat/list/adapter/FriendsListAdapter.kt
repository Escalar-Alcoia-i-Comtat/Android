package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.BuildConfig
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.ProfileActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.ProfileActivity.Companion.BUNDLE_EXTRA_ADDING_FRIEND
import com.arnyminerz.escalaralcoiaicomtat.activity.ProfileActivity.Companion.BUNDLE_EXTRA_USER_UID
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.fragment.AuthFragment
import com.arnyminerz.escalaralcoiaicomtat.fragment.ProfileFragment
import com.arnyminerz.escalaralcoiaicomtat.list.holder.FriendsListViewHolder
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber

@ExperimentalUnsignedTypes
class FriendsListAdapter(
    private val mainActivity: MainActivity,
    private val profileFragment: ProfileFragment?,
    private val isSelf: Boolean,
    private val friends: ArrayList<UserData>
) :
    RecyclerView.Adapter<FriendsListViewHolder>() {
    override fun getItemCount(): Int = friends.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsListViewHolder {
        return FriendsListViewHolder(
            LayoutInflater.from(mainActivity).inflate(
                R.layout.list_item_friend, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FriendsListViewHolder, position: Int) {
        val friend = friends[position]

        holder.textView.text = friend.username
        Glide.with(mainActivity)
            .load(friend.profileImage)
            .thumbnail(.5f)
            .centerCrop()
            .into(holder.imageButton)

        holder.imageButton.setOnClickListener {
            if (profileFragment != null)
                profileFragment.update(friend)
            else
                mainActivity.startActivity(
                    Intent(
                        mainActivity,
                        ProfileActivity::class.java
                    ).apply {
                        putExtra(BUNDLE_EXTRA_USER_UID, friend.uid)
                        putExtra(BUNDLE_EXTRA_ADDING_FRIEND, BuildConfig.DEBUG)
                    })
        }
        if (isSelf)
            holder.imageButton.setOnLongClickListener {
                MaterialAlertDialogBuilder(mainActivity)
                    .setTitle(R.string.dialog_remove_friend_title)
                    .setMessage(
                        mainActivity.getString(
                            R.string.dialog_remove_friend_message,
                            friend.username
                        )
                    )
                    .setPositiveButton(R.string.action_remove) { dialog, _ ->
                        GlobalScope.launch {
                            try {
                                val removed = AuthFragment.user?.removeFriend(
                                    mainActivity.networkState,
                                    friend
                                )

                                if (removed == true)
                                    mainActivity.runOnUiThread {
                                        friends.removeAt(position)
                                        notifyDataSetChanged()
                                        dialog.dismiss()
                                    }
                                else {
                                    mainActivity.toast(R.string.toast_error_internal)
                                    Timber.e("AuthFragment:user is null!")
                                }
                            } catch (error: Exception) {
                                mainActivity.runOnUiThread {
                                    mainActivity.toast(R.string.toast_error_internal)
                                    Timber.e(error)
                                }
                            }
                        }
                    }
                    .show()

                true
            }
    }
}