package com.nipun.locationtracking.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.nipun.locationtracking.MainActivity
import kotlinx.android.synthetic.main.activity_main.*

/*
* Created by Nipun Kumar Rajput on 19-04-2020.
* Copyright (c) 2020 Nipun. All rights reserved.
*/

open class BaseFragment : Fragment() {
    private var progressBar: ProgressBar? = null
    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressBar = (requireActivity() as MainActivity).progressBar
        auth = (requireActivity() as MainActivity).auth
    }

    fun showProgressBar() {
        progressBar?.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        progressBar?.visibility = View.INVISIBLE
    }

    fun getFireBaseAuth(): FirebaseAuth? {
        return auth
    }

    fun hideKeyboard(view: View) {
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    public override fun onStop() {
        super.onStop()
        hideProgressBar()
    }
}