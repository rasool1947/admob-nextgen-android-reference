package com.example.admob_next_gen.utilities.base.activities

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding



abstract class BaseActivity<T : ViewBinding>(bindingFactory: (LayoutInflater) -> T) : ParentActivity<T>(bindingFactory) {

}