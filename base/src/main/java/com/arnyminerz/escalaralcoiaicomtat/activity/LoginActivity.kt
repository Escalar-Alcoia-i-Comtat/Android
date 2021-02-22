package com.arnyminerz.escalaralcoiaicomtat.activity

import android.os.Bundle
import com.arnyminerz.escalaralcoiaicomtat.R
import com.arnyminerz.escalaralcoiaicomtat.activity.model.NetworkChangeListenerActivity
import com.arnyminerz.escalaralcoiaicomtat.data.user.UserData
import com.arnyminerz.escalaralcoiaicomtat.databinding.ActivityLoginBinding
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.UserNotAuthenticableException
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.UserNotFoundException
import com.arnyminerz.escalaralcoiaicomtat.exception.auth.WrongPasswordException
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.disable
import com.arnyminerz.escalaralcoiaicomtat.generic.extension.enable
import com.arnyminerz.escalaralcoiaicomtat.generic.toast
import com.arnyminerz.escalaralcoiaicomtat.list.viewListOf
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LoginActivity: NetworkChangeListenerActivity() {
    private lateinit var binding: ActivityLoginBinding

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        with(binding){
            usernameTextInput.error = null
            loginButton.setOnClickListener {
                viewListOf(usernameTextInput, passwordTextInput, it).disable()
                GlobalScope.launch {
                    try{
                        UserData.login(
                            this@LoginActivity,
                            usernameEditText.text.toString(),
                            passwordEditText.text.toString()
                        )
                    }catch (e: WrongPasswordException){
                        toast(this@LoginActivity, R.string.toast_login_password_wrong)
                    }catch (e: UserNotFoundException){
                        toast(this@LoginActivity, R.string.toast_error_user_not_found)
                    }catch (e: UserNotAuthenticableException){
                        // TODO: Add create password logic
                    }finally {
                        viewListOf(usernameTextInput, passwordTextInput, it).enable(this@LoginActivity)
                    }
                }
            }
        }
    }
}