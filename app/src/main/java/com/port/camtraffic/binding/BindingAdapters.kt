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
                ContextCompat.getColor(textInputLayout.context, R.color.redMarker)
        } else {
            ContextCompat.getColor(textInputLayout.context, R.color.lightBlue)
        }

        textInputLayout.startIconDrawable?.setTint(colorId)
    }
}