package org.totschnig.myexpenses.util.config

import kotlin.reflect.KClass

interface Configurator {

    enum class Configuration {
        USE_SET_DECOR_PADDING_WORKAROUND,
        AUTO_COMPLETE_DROPDOWN_SET_INPUT_METHOD_NEEDED,
        ad_handling_waterfall;
    }

    fun <T : Any> get(key: Configuration, defaultValue: T, clazz: KClass<T>) = defaultValue

    companion object {
        val NO_OP = object : Configurator {}
    }
}

inline operator fun <reified T : Any> Configurator.get(
    key: Configurator.Configuration,
    defaultValue: T
) = get(key, defaultValue, T::class)