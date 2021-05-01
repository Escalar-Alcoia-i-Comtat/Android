package com.arnyminerz.escalaralcoiaicomtat.list.completions.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.profile.CommentsActivity
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.Grade
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkedCompletedData
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkedDataInt
import com.arnyminerz.escalaralcoiaicomtat.data.climb.path.MarkedProjectData
import com.arnyminerz.escalaralcoiaicomtat.generic.MEGABYTE
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.completions.holder.CommentsViewHolder
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import timber.log.Timber

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
    private val items: List<MarkedDataInt>
) : RecyclerView.Adapter<CommentsViewHolder>() {
    /**
     * Stores the [FirebaseStorage] reference to load the profile images.
     * @author Arnau Mora
     * @since 20210501
     */
    private val storage: FirebaseStorage = Firebase.storage

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
        val userData = markedDataInt.user
        val profileImage = userData.profileImagePath
        val profileName = userData.displayName
        val profileUid = userData.uid

        val loggedUser = auth.currentUser
        val userLoggedIn = loggedUser != null
        val loggedUserUid = loggedUser?.uid

        Timber.v("Loading comment made by $profileName ($profileUid)...")
        Timber.v("Comment: $comment")

        storage.getReferenceFromUrl(profileImage)
            .getBytes(MEGABYTE * 5)
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

        // TODO: Like tap listener and liker
        // TODO: Delete tap listener and deleter
    }
}
