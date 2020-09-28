package com.arnyminerz.escalaralcoiaicomtat.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity.Companion.loggedIn
import com.arnyminerz.escalaralcoiaicomtat.activity.ProfileActivity.Companion.BUNDLE_EXTRA_USER_UID
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.async.EXTENDED_API_URL
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.fragment.AuthFragment
import com.arnyminerz.escalaralcoiaicomtat.generic.jsonFromUrl
import com.arnyminerz.escalaralcoiaicomtat.list.adapter.UserAdapter
import com.arnyminerz.escalaralcoiaicomtat.view.visibility
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_friend_search.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import timber.log.Timber


@ExperimentalUnsignedTypes
class FriendSearchActivity : NetworkChangeListenerActivity() {
    private var textViewText = ""
    private var lastSearch = ""
    private var userUid = ""

    private val searchThreadProto = object : Thread() {
        override fun run() {
            GlobalScope.launch {
                while (true) {
                    if (lastSearch != textViewText)
                        if (textViewText.isNotEmpty()) {
                            val json = jsonFromUrl("$EXTENDED_API_URL/user/search/$textViewText")
                            if (json["result"] == "ok") {
                                val data = json.getJSONArray("data")

                                val users = arrayListOf<UserData>()
                                for (i in 0 until data.length())
                                    data.getJSONObject(i).let { item ->
                                        val userData = UserData(item)
                                        if (userData.uid != userUid)
                                            users.add(userData)
                                    }

                                runOnUiThread {
                                    visibility(user_search_recyclerView, data.length() > 0)
                                    visibility(no_search_result_textView, data.length() <= 0)

                                    user_search_recyclerView.layoutManager =
                                        LinearLayoutManager(this@FriendSearchActivity)
                                    user_search_recyclerView.adapter =
                                        UserAdapter(
                                            this@FriendSearchActivity,
                                            AuthFragment.user!!,
                                            users
                                        )
                                }
                            }
                            lastSearch = textViewText
                        } else
                            runOnUiThread {
                                visibility(user_search_recyclerView, false)
                                visibility(no_search_result_textView, true)
                            }
                }
            }
        }
    }

    private val textChanged = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            textViewText = s.toString()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private var searchThread: Thread = searchThreadProto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_search)

        if (intent == null) {
            Timber.e("Intent is null")
            onBackPressed()
            return
        }
        if (intent.extras == null) {
            Timber.e("Intent has no extras")
            onBackPressed()
            return
        }
        if (!intent.extras!!.containsKey(BUNDLE_EXTRA_USER_UID)) {
            Timber.e("Intent doesn't have BUNDLE_EXTRA_USER_UID")
            onBackPressed()
            return
        }
        userUid = intent.getStringExtra(BUNDLE_EXTRA_USER_UID)!!

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_round_arrow_back_24)

        visibility(user_search_recyclerView, false)
        visibility(no_search_result_textView, true)

        searchThread.start()

        search_editText.addTextChangedListener(textChanged)

        scan_button.setOnClickListener {
            IntentIntegrator(this)
                .setPrompt("Scan")
                .setBeepEnabled(true)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setOrientationLocked(true)
                .setBarcodeImageEnabled(true)
                .initiateScan()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IntentIntegrator.parseActivityResult(
            requestCode,
            resultCode,
            data
        )?.contents?.let { result ->
            if (!loggedIn()) GlobalScope.launch {
                val user = UserData.fromQR(networkState, result)
                val loggedUserUid = MainActivity.user()!!.uid
                val sent = UserData.sendFriendRequest(networkState, loggedUserUid, user.uid)
                runOnUiThread {
                    if (sent) {
                        toast(R.string.toast_friend_request_sent)
                        Timber.v("Sent friend request to %s.", user.uid)
                    } else {
                        toast(R.string.toast_error_request)
                        Timber.e("Could not send friend request.")
                    }
                }
            } else {
                toast(R.string.toast_error_not_logged_in)
                Timber.e("User currently not logged in")
            }
        }
    }
}