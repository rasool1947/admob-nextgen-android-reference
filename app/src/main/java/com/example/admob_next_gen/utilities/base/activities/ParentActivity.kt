package com.example.admob_next_gen.utilities.base.activities

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding




abstract class ParentActivity<T : ViewBinding>(private val bindingFactory: (LayoutInflater) -> T) : AppCompatActivity() {

    protected val binding by lazy { bindingFactory(layoutInflater) }
    protected var includeTopPadding = true
    protected var includeBottomPadding = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setPadding()
        onCreated()
    }

    private fun setPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val topPadding = if (includeTopPadding) bars.top else 0
            val bottomPadding = if (includeBottomPadding) bars.bottom else 0
            v.updatePadding(left = bars.left, top = topPadding, right = bars.right, bottom = bottomPadding)
            WindowInsetsCompat.CONSUMED
        }
    }

    abstract fun onCreated()
}
