package com.nipun.locationtracking.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.nipun.locationtracking.BuildConfig
import com.nipun.locationtracking.R
import com.nipun.locationtracking.databinding.LoginFragmentBinding


class LoginFragment : BaseFragment() {
    private lateinit var binding: LoginFragmentBinding

    companion object {
        private const val RC_SIGN_IN = 9001
        fun newInstance() = LoginFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LoginFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSignIn.setOnClickListener {
            startSignIn()
        }
        val auth = getFireBaseAuth()
        if (auth != null && auth.currentUser != null) {
            //Already signed in
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToPermissionsFragment())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                // Sign in succeeded
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToPermissionsFragment())
            } else {
                // Sign in failed
                Toast.makeText(requireContext(), "Sign In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startSignIn() {
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
            .setIsSmartLockEnabled(!BuildConfig.DEBUG)
            .setAvailableProviders(listOf(AuthUI.IdpConfig.EmailBuilder().build()))
            .setLogo(R.mipmap.ic_location_tracking)
            .build()

        startActivityForResult(intent,
            RC_SIGN_IN
        )
    }
}
