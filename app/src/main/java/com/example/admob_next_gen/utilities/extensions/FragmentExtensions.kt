package com.example.admob_next_gen.utilities.extensions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController



/* ----------------------------------------- Navigation's -----------------------------------------*/

/**
 *     Used launchWhenCreated, bcz of screen rotation
 * @param fragmentId : Current Fragment's Id (from Nav Graph)
 * @param action : Action / Id of other fragment
 */

fun Fragment.navigateTo(fragmentId: Int, action: Int) {
    launchWhenCreated {
        if (isAdded && isCurrentDestination(fragmentId)) {
            findNavController().navigate(action)
        }
    }
}

fun Fragment.popFrom(fragmentId: Int) {
    launchWhenCreated {
        if (isAdded && isCurrentDestination(fragmentId)) {
            findNavController().popBackStack()
        }
    }
}

private fun Fragment.isCurrentDestination(fragmentId: Int): Boolean {
    return findNavController().currentDestination?.id == fragmentId
}
