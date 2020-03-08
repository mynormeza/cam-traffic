package com.port.camtraffic.binding

import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout
import com.port.camtraffic.R

object BindingAdapters {
    @BindingAdapter("startIcon")
    @JvmStatic fun setNumberOfSets(textInputLayout: TextInputLayout, @DrawableRes id: Int) {
        textInputLayout.setStartIconDrawable(id)
        val colorId = if (id == R.drawable.ic_location_on) {
                ContextCompat.getColor(textInputLayout.context, R.color.red_marker)
        } else {
            ContextCompat.getColor(textInputLayout.context, R.color.light_blue)
        }

        textInputLayout.startIconDrawable?.setTint(colorId)
    }
}