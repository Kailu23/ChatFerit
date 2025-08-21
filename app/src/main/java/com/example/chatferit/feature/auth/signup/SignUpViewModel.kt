package com.example.chatferit.feature.auth.signup

import android.R
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(): ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun SignUp(name : String, surname : String,email : String, password : String)
    {
        val fullName : String = name + " " + surname
        _state.value = SignUpState.Loading

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result.user?.let {
                        it.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()
                        )
                        _state.value = SignUpState.Success
                        return@addOnCompleteListener
                    }
                    _state.value = SignUpState.Error
                } else {
                    _state.value = SignUpState.Error
                }
            }
    }

}


sealed class SignUpState
{
    object Nothing: SignUpState()
    object Loading: SignUpState()
    object Success: SignUpState()
    object Error: SignUpState()
}