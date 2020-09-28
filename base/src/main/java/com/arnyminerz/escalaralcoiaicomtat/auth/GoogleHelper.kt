package com.arnyminerz.escalaralcoiaicomtat.auth

import com.arnyminerz.escalaralcoiaicomtat.activity.MainActivity
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import timber.log.Timber

@ExperimentalUnsignedTypes
fun firebaseAuthWithGoogle(idToken: String?, callback: (user: FirebaseUser?) -> Unit){
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    MainActivity.auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if(task.isSuccessful){
                Timber.d("Signed in with credential!")

                callback(MainActivity.auth.currentUser)
            } else{
                Timber.e("Could not sign in with credential!")

                callback(null)
            }
        }
}