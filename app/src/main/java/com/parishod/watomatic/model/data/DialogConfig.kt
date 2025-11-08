package com.parishod.watomagic.model.data

import android.os.Parcelable
import com.parishod.watomagic.model.interfaces.DialogItem
import com.parishod.watomagic.model.enums.DialogType
import kotlinx.parcelize.Parcelize

@Parcelize
data class DialogConfig(
    val dialogType: DialogType,
    val title: String,
    val description: String = "",
    val showSearch: Boolean = false,
    val searchHint: String = "Search",
    val saveButtonText: String = "Save",
    val items: List<DialogItem>
) : Parcelable