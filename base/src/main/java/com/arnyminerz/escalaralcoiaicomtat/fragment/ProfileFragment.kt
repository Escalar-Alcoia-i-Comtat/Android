package com.arnyminerz.escalaralcoiaicomtat.fragment

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.FriendSearchActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.arnyminerz.escalaralcoiaicomtat.activity.ProfileActivity.Companion.BUNDLE_EXTRA_USER_UID
import com.arnyminerz.escalaralcoiaicomtat.connection.ftp.FTP_PASSWORD
import com.arnyminerz.escalaralcoiaicomtat.connection.ftp.FTP_SERVER
import com.arnyminerz.escalaralcoiaicomtat.connection.ftp.FTP_USER
import com.arnyminerz.escalaralcoiaicomtat.connection.ftp.ftpConnect
import com.arnyminerz.escalaralcoiaicomtat.data.user.CompletedPathSortOrder
import com.arnyminerz.escalaralcoiaicomtat.data.user.FriendRequest
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.ChangeNameDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.CompletedPathsDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.dialog.QRCodeDialog
import com.arnyminerz.escalaralcoiaicomtat.fragment.model.NetworkChangeListenerFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.getMimeType
import com.arnyminerz.escalaralcoiaicomtat.generic.getSerializable
import com.arnyminerz.escalaralcoiaicomtat.generic.runOnUiThread
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.FriendRequestAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.FriendsListAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.PathsAdapter
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.ProfilePathAdapter
import com.arnyminerz.escalaralcoiaicomtat.view.getColor
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.FileInputStream

private const val BUNDLE_EXTRA_ORIGINAL_USER = "original_user"

@ExperimentalUnsignedTypes
class ProfileFragment : NetworkChangeListenerFragment() {
    companion object {
        fun newInstance(showUser: UserData): ProfileFragment =
            ProfileFragment().apply {
                arguments = Bundle(1).apply {
                    putSerializable(BUNDLE_EXTRA_ORIGINAL_USER, showUser)
                }
            }

        private const val INTENT_CHANGE_IMAGE = 1
    }

    private var originalUser: UserData? = null

    var user: UserData? = null
        private set

    var changedUser: Boolean = false
        private set

    fun update(user: UserData?) {
        this.user = user
        changedUser = user != originalUser
        updateProfileUI()
    }

    fun restore() = update(originalUser)

    fun updateProfileUI(vw: View? = view) {
        if (!MainActivity.loggedIn() || vw == null || !isResumed)
            return

        GlobalScope.launch {
            val preferences = user!!.preferences(networkState)

            runOnUiThread {
                logout_button.setOnClickListener {
                    MainActivity.auth.signOut()
                }

                val role = user!!.role

                visibility(profileLoading_progressBar, true)
                visibility(imageLoading_progressBar, false)
                visibility(lastCompletedPaths_cardView, false)
                visibility(friend_requests_cardView, false)
                visibility(friends_cardView, false)
                visibility(role_chip, role.showChip())

                val isSelf = user!!.equals(MainActivity.user())
                var areFriends = false

                visibility(shareProfile_imageButton, isSelf)

                var showRequests = false
                var showFriends = false
                var showPaths = false

                var finishedLoadingRequests = false
                var finishedLoadingPaths = false
                var finishedLoadingFriends = false

                fun finishCallback() {
                    if (finishedLoadingFriends && finishedLoadingPaths && finishedLoadingRequests) {
                        Timber.v("Called finishCallback. Everything finished loading from user \"${user!!.uid}\".")
                        runOnUiThread {
                            visibility(profileLoading_progressBar, false)
                            visibility(lastCompletedPaths_cardView, showPaths)
                            visibility(friends_cardView, showFriends)
                            visibility(friend_requests_cardView, showRequests)
                        }
                    } else Timber.v("Called finishCallback for user \"${user!!.uid}\" but some things hasn't been completed. (finishedLoadingFriends: $finishedLoadingFriends, finishedLoadingPaths: $finishedLoadingPaths, finishedLoadingRequests: $finishedLoadingRequests)")
                }

                if (isSelf) GlobalScope.launch {
                    Timber.v("Getting requests")
                    try {
                        val requestsFlow = user!!.friendRequests(networkState)
                        val requests = arrayListOf<FriendRequest>()
                        requestsFlow.collect {
                            showRequests = true
                            requests.add(it)
                        }
                        runOnUiThread {
                            friend_requests_recyclerView.layoutManager =
                                LinearLayoutManager(context)
                            friend_requests_recyclerView.adapter =
                                FriendRequestAdapter(this@ProfileFragment, requests)

                            finishedLoadingRequests = true
                            finishCallback()
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex, "Could not load friend requests")
                        finishedLoadingRequests = true
                    }

                    runOnUiThread {
                        user_name_textView.setOnClickListener {
                            ChangeNameDialog(requireContext())
                                .apply {
                                    input(defaultText = user!!.username)
                                    title(R.string.dialog_change_name_title)
                                    positiveButton(R.string.action_change) { dialog: DialogInterface, _: Int ->
                                        val newUsername = this.getText()

                                        GlobalScope.launch {
                                            val updated =
                                                user!!.updateUsername(networkState, newUsername!!)
                                            runOnUiThread {
                                                if (updated) {
                                                    toast(R.string.toast_changed_name)

                                                    user_name_textView.text = newUsername
                                                } else {
                                                    toast(R.string.toast_error_change_name)

                                                    Timber.e("Could not update username")
                                                }
                                            }
                                        }

                                        dialog.dismiss()
                                    }
                                    negativeButton(R.string.action_cancel) { dialog: DialogInterface, _: Int ->
                                        dialog.dismiss()
                                    }
                                }.show()
                        }
                        user_profile_imageButton.setOnClickListener {
                            val intent = Intent()
                            intent.type = "image/*"
                            intent.action = Intent.ACTION_GET_CONTENT
                            intent.putExtra(
                                Intent.EXTRA_MIME_TYPES,
                                arrayOf("image/jpeg", "image/png")
                            )
                            startActivityForResult(
                                Intent.createChooser(intent, "Select Picture"),
                                INTENT_CHANGE_IMAGE
                            )
                        }

                        shareProfile_imageButton.setOnClickListener {
                            QRCodeDialog(requireActivity(), user!!.qrContent()).show()
                        }
                    }
                } else {
                    Timber.v("Checking if logged user is friend with current. Skipping requests load.")
                    finishedLoadingRequests = true

                    GlobalScope.launch {
                        val friends = AuthFragment.user!!.friendWith(networkState, user!!)
                        areFriends = friends.areFriends

                        runOnUiThread {
                            visibility(add_friend_button, !areFriends)
                        }
                    }

                    user_name_textView.setOnClickListener { }
                    user_profile_imageButton.setOnClickListener { }
                }

                if (role.showChip())
                    role_chip.apply {
                        text = role.displayName
                        role.color?.let {
                            setChipBackgroundColorResource(it)
                            this.profileImage_cardView?.strokeWidth = 3
                            this.profileImage_cardView?.strokeColor = getColor(context, it)
                        }
                    }

                showPaths = preferences.shouldShowCompletedPaths()

                GlobalScope.launch {
                    val c = requireContext()

                    Timber.d("Getting paths for uid \"${user!!.uid}\"...")
                    val completedPathsFlow = user!!.completedPaths(
                        networkState,
                        5,
                        sort = CompletedPathSortOrder.DATE_DES
                    )
                    val completedPaths = completedPathsFlow.toCollection(arrayListOf())

                    Timber.d("Getting friends list...")
                    val friendsFlow = user!!.friends(networkState, 100)
                    val friends = friendsFlow.toCollection(arrayListOf())

                    Timber.d("Got ${completedPaths.size} completed paths for uid \"${user!!.uid}\".")
                    Timber.d("Got ${friends.size} friends for uid \"${user!!.uid}\".")

                    showFriends = preferences.shouldShowFriends(originalUser!!, networkState)

                    runOnUiThread {
                        lastCompletedPaths_recyclerView.layoutManager = LinearLayoutManager(c)
                        lastCompletedPaths_recyclerView.adapter =
                            ProfilePathAdapter(c, user!!, completedPaths)

                        friends_recyclerView.layoutManager =
                            LinearLayoutManager(c, LinearLayoutManager.HORIZONTAL, false)
                        friends_recyclerView.adapter =
                            FriendsListAdapter(
                                c as MainActivity,
                                this@ProfileFragment,
                                isSelf,
                                friends
                            )

                        visibility(noCompletedPaths_textView, completedPaths.size <= 0)
                        visibility(lastCompletedPaths_recyclerView, completedPaths.size > 0)

                        finishedLoadingPaths = true
                        finishedLoadingFriends = true
                        finishCallback()
                    }
                }

                visibility(logout_button, isSelf)
                visibility(change_profile_textView, isSelf)
                if (isSelf)
                    visibility(add_friend_button, true)

                user_name_textView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    if (isSelf) requireContext().getDrawable(R.drawable.pencil) else null,
                    null
                )
                user_name_textView.text = user!!.username

                if (user!!.profileImage != null)
                    Glide.with(requireContext())
                        .load(if (preferences.shouldShowProfileImage()) user!!.profileImage else "https://api.adorable.io/avatars/96/${Math.random()}")
                        .centerCrop()
                        .into(user_profile_imageButton)

                lastCompletedPaths_recyclerView.layoutManager =
                    LinearLayoutManager(requireContext())
                lastCompletedPaths_recyclerView.adapter = PathsAdapter(
                    arrayListOf(),
                    requireActivity() as MainActivity
                )

                view_more_button.setOnClickListener {
                    CompletedPathsDialog(
                        user!!,
                        networkState
                    ).show(parentFragmentManager, "CompletedPathsDialog")
                }

                add_friend_button.setOnClickListener {
                    if (isSelf) {
                        startActivity(
                            Intent(requireContext(), FriendSearchActivity::class.java)
                                .putExtra(BUNDLE_EXTRA_USER_UID, user!!.uid)
                        )
                    } else if (!areFriends) GlobalScope.launch {
                        val requested =
                            user!!.friendRequest(networkState, AuthFragment.user!!)
                        runOnUiThread {
                            if (requested) {
                                Timber.d("Requested correctly")
                                context?.toast(R.string.toast_friend_request_sent)
                            } else {
                                Timber.e("Could not request")
                                context?.toast(R.string.toast_error_request)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            Timber.e("Arguments are null")
            return
        }

        originalUser = requireArguments().getSerializable<UserData>(BUNDLE_EXTRA_ORIGINAL_USER)
        user = originalUser
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == INTENT_CHANGE_IMAGE) {
            if (data == null || MainActivity.user() == null) return

            val uri = data.data ?: return

            val resolver = requireContext().contentResolver
            doAsync {
                resolver.openInputStream(uri).use { stream ->
                    runOnUiThread {
                        visibility(imageLoading_progressBar, true)
                    }
                    val uploadPath =
                        "/app/users/img/${MainActivity.user()!!.uid}.${
                            getMimeType(
                                requireContext(),
                                uri
                            )
                        }"
                    Timber.v("File Upload Path: $uploadPath")
                    val ftp = ftpConnect(FTP_SERVER, FTP_USER, FTP_PASSWORD)

                    val fileInputStream = stream as FileInputStream
                    val uploaded = ftp.uploadImage(fileInputStream, uploadPath)

                    if (uploaded) GlobalScope.launch {
                        try {
                            val newProfileImage = user!!.updateProfileImage(
                                networkState,
                                "https://escalaralcoiaicomtat.centrexcursionistalcoi.org$uploadPath"
                            )
                            runOnUiThread {
                                Timber.v("User profile updated.")
                                visibility(imageLoading_progressBar, false)
                                toast("Image changed correctly!")
                                Glide.with(requireContext())
                                    .load(newProfileImage)
                                    .centerCrop()
                                    .into(user_profile_imageButton)
                            }
                        } catch (error: Exception) {
                            runOnUiThread {
                                toast(R.string.toast_error_image)
                                Timber.e(error, "Could not update profile image:")
                            }
                        }
                    } else
                        requireContext().runOnUiThread { toast("Could not upload") }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
    }
}