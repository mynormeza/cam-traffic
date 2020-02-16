package com.port.camtraffic.binding

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

class BindableProperty<T>(
    initial: T,
    private val observable: ObservableViewModel,
    private val id: Int
) : ObservableProperty<T>(initial) {

    override fun beforeChange(
        property: KProperty<*>,
        oldValue: T,
        newValue: T
    ): Boolean = oldValue != newValue

    override fun afterChange(
        property: KProperty<*>,
        oldValue: T,
        newValue: T
    ) = observable.notifyPropertyChanged(id)
}