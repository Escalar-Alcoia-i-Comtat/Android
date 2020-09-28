package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.data.user.FriendRequest
import com.arnyminerz.escalaralcoiaicomtat.fragment.ProfileFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.holder.FriendRequestViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalUnsignedTypes
class FriendRequestAdapter(private val profileFragment: ProfileFragment, private val requests: ArrayList<FriendRequest>): RecyclerView.Adapter<FriendRequestViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder =
        FriendRequestViewHolder(LayoutInflater.from(profileFragment.requireContext()).inflate(R.layout.list_item_friend_request, parent, false))

    override fun getItemCount(): Int = requests.size

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val request = requests[position]
        holder.textView.text = request.fromUser.username

        visibility(holder.progressBar, false)

        holder.acceptImageButton.setOnClickListener {
            visibility(holder.progressBar, true)
            holder.acceptImageButton.isEnabled = false
            holder.denyImageButton.isEnabled = false

            GlobalScope.launch {
                try {
                    request.consume(profileFragment.networkState, true)

                    Timber.v("Accepted friend request!")
                    profileFragment.runOnUiThread {
                        profileFragment.updateProfileUI()
                        requests.removeAt(position)
                        notifyDataSetChanged()
                    }
                } catch (error: Exception) {
                    Timber.e(error, "Could not consume friend request (Accept).")
                }
            }
        }

        holder.denyImageButton.setOnClickListener {
            visibility(holder.progressBar, true)
            holder.acceptImageButton.isEnabled = false
            holder.denyImageButton.isEnabled = false

            GlobalScope.launch {
                try {
                    request.consume(profileFragment.networkState, false)

                    Timber.v("Denied friend request!")
                    profileFragment.runOnUiThread {
                        profileFragment.updateProfileUI()
                        requests.removeAt(position)
                        notifyDataSetChanged()
                    }
                } catch (error: Exception) {
                    Timber.e(error, "Could not consume friend request (Accept).")
                }
            }
        }
    }
}