package com.example.admob_next_gen.utilities.base.fragments

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding



abstract class BaseFragment<T : ViewBinding>(bindingFactory: (LayoutInflater) -> T) : ParentFragment<T>(bindingFactory)
