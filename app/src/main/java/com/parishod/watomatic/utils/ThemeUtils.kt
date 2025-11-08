package com.parishod.watomagic.utils

import android.content.Context
import android.util.TypedValue

object ThemeUtils {
    fun getThemeColor(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
