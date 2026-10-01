package com.example.admob_next_gen.utilities.base.fragments

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.example.admob_next_gen.di.DIComponent



abstract class BaseFragment<T : ViewBinding>(bindingFactory: (LayoutInflater) -> T) : ParentFragment<T>(bindingFactory) {

    protected val diComponent by lazy { DIComponent() }

}