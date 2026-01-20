package com.nova.funkindevplugin

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE = "messages.VSliceBundle"

object VSliceBundle {
    private val INSTANCE = DynamicBundle(VSliceBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = INSTANCE.getMessage(key, *params)

    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): () -> String = { INSTANCE.getMessage(key, *params) }
}