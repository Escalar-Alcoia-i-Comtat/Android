package com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.CommentsActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage.MarkedCompletedData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.storage.MarkedProjectData
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.completions.holder.CommentsViewHolder
import com.arnyminerz.escalaralcoiaicomtat.shared.PROFILE_IMAGE_MAX_SIZE
import com.arnyminerz.escalaralcoiaicomtat.view.getColor
import com.arnyminerz.escalaralcoiaicomtat.view.getColorFromAttribute
import com.arnyminerz.escalaralcoiaicomtat.view.hide
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The RecyclerView adapter for showing the comments that people have published into a Path.
 * This is meant to be used inside [CommentsActivity].
 * It's assumed that [items] contain valid comments (with content), otherwise, [NullPointerException]
 * may be thrown.
 * @author Arnau Mora
 * @since 2021051
 * @param activity The activity that contains the RecyclerView to use.
 * @param items The [MarkedDataInt] elements to show.
 * @throws NullPointerException When an element doesn't have a valid comment
 */
class CommentsAdapter(
    private val activity: CommentsActivity,
    private val items: ArrayList<MarkedDataInt>
) : RecyclerView.Adapter<CommentsViewHolder>() {
    /**
     * Stores the [FirebaseStorage] reference to load the profile images.
     * @author Arnau Mora
     * @since 20210501
     */
    private val storage: FirebaseStorage = Firebase.storage

    /**
     * Stores the [FirebaseFirestore] reference to update likes and delete.
     * @author Arnau Mora
     * @since 20210501
     */
    private val firestore: FirebaseFirestore = Firebase.firestore

    /**
     * Stores the [FirebaseAuth] reference to load the profile images.
     * @author Arnau Mora
     * @since 20210501
     */
    private val auth: FirebaseAuth = Firebase.auth

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder =
        CommentsViewHolder(
            LayoutInflater.from(activity).inflate(R.layout.list_item_comment, parent, false)
        )

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val markedDataInt = items[position]
        val comment = markedDataInt.comment
            ?: throw NullPointerException("The comment #$position is null")
        val timestamp = markedDataInt.timestamp
        val userData = markedDataInt.user
        val likes = markedDataInt.likedBy
        val profileImage = userData.profileImagePath
        val profileName = userData.displayName
        val profileUid = userData.uid
        val documentPath = markedDataInt.documentPath

        val loggedUser = auth.currentUser
        val userLoggedIn = loggedUser != null
        val loggedUserUid = loggedUser?.uid

        Timber.v("Loading comment made by $profileName ($profileUid)...")
        Timber.v("Comment: $comment")

        storage.getReferenceFromUrl(profileImage)
            .getBytes(PROFILE_IMAGE_MAX_SIZE)
            .addOnSuccessListener { bytes ->
                Glide.with(activity)
                    .load(bytes)
                    .into(holder.profileImageView)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Could not load profile image ($profileImage).")
                toast(activity, R.string.toast_error_profile_image_load)
            }
        holder.profileNameTextView.text = userData.displayName
        holder.commentTextView.text = comment
        holder.dateTextView.text =
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(timestamp)

        holder.deleteTextView.visibility(userLoggedIn && loggedUserUid == profileUid)

        if (markedDataInt is MarkedProjectData)
            holder.gradeTextView.setText(R.string.comments_project)
        else if (markedDataInt is MarkedCompletedData) {
            val grade = markedDataInt.grade
            holder.gradeTextView.setText(
                Grade(grade).getSpannable(activity, 1),
                TextView.BufferType.SPANNABLE
            )
            holder.likesTextView.text = markedDataInt.likesCount.toString()
        }
        updateLikeStatus(holder, likes.contains(loggedUserUid), likes.size)

        if (userLoggedIn) {
            holder.likesTextView.setOnClickListener {
                Timber.v("Requested like change for $documentPath")
                holder.likesTextView.isEnabled = false
                markedDataInt.like(firestore, loggedUser!!)
                    .addOnCompleteListener {
                        holder.likesTextView.isEnabled = true
                    }
                    .addOnSuccessListener {
                        updateLikeStatus(holder, likes.contains(loggedUserUid), likes.size)
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Could not like comment.")
                        toast(activity, R.string.toast_error_like)
                    }
            }
            holder.deleteTextView.setOnClickListener {
                askForDeletion(holder, markedDataInt)
            }
        }
    }

    /**
     * Updates the [CommentsViewHolder.likesTextView] according to [liked].
     * @author Arnau Mora
     * @since 20210501
     * @param holder The view holder to update.
     * @param liked If the user has liked the comment.
     * @param likeCount The amount of likes the comment has
     */
    private fun updateLikeStatus(holder: CommentsViewHolder, liked: Boolean, likeCount: Int) {
        holder.likesTextView.text = likeCount.toString()
        holder.likesTextView.setCompoundDrawables(
            ResourcesCompat.getDrawable(
                activity.resources,
                if (liked)
                    R.drawable.ic_round_favorite_24
                else
                    R.drawable.ic_round_favorite_border_24,
                activity.theme
            )?.apply {
                DrawableCompat.setTint(
                    this,
                    if (liked)
                        getColor(activity, R.color.color_like)
                    else
                        getColorFromAttribute(activity, R.attr.colorControlNormal)
                )
            },
            null,
            null,
            null
        )
    }

    /**
     * Shows the user a prompt for comfirming a post deletion.
     * @author Arnau Mora
     * @since 20210528
     * @param holder The [CommentsViewHolder] for the element.
     * @param item The item to be deleted.
     */
    private fun askForDeletion(holder: CommentsViewHolder, item: MarkedDataInt) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.comments_delete_dialog_title)
            .setMessage(R.string.comments_delete_dialog_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                dialog.dismiss()
                holder.cardView.hide()
                item.delete(firestore)
                    .addOnSuccessListener {
                        Timber.v("Post deleted successfully.")
                        items.remove(item)
                        activity.notifyDeletion()
                    }
                    .addOnFailureListener { e ->
                        Timber.e(e, "Could not delete post.")
                        toast(activity, R.string.toast_error_delete_post)
                    }
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
