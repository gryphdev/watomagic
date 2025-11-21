package com.parishod.watomagic.model.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.parishod.watomagic.R
import com.parishod.watomagic.model.data.CooldownItem

class CooldownAdapter(
    private val items: List<CooldownItem>,
    private val onCooldownTimeChanged: (Int) -> Unit
) : RecyclerView.Adapter<CooldownAdapter.ViewHolder>() {

    private var selectedHour: Int = 0
    private var selectedMinute: Int = 1
    private var selectedSecond: Int = 0
    private var timeUnit: TimeUnit = TimeUnit.MINUTES
    private var isReset: Boolean = false

    private enum class TimeUnit {
        HOURS, MINUTES, SECONDS
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val toggleButtonGroup: MaterialButtonToggleGroup = view.findViewById(R.id.toggle_button_group)
        val numberPicker: NumberPicker = view.findViewById(R.id.numberPicker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cooldown_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (!isReset) {
            items.getOrNull(position)?.let {
                val totalSeconds = it.cooldownInSeconds
                when {
                    totalSeconds >= 3600 && totalSeconds % 3600 == 0 -> {
                        timeUnit = TimeUnit.HOURS
                        selectedHour = totalSeconds / 3600
                    }
                    totalSeconds >= 60 && totalSeconds % 60 == 0 -> {
                        timeUnit = TimeUnit.MINUTES
                        selectedMinute = totalSeconds / 60
                    }
                    else -> {
                        timeUnit = TimeUnit.SECONDS
                        selectedSecond = totalSeconds
                    }
                }
            }
        }
        isReset = false

        val buttonId = when (timeUnit) {
            TimeUnit.HOURS -> R.id.button_hours
            TimeUnit.MINUTES -> R.id.button_minutes
            TimeUnit.SECONDS -> R.id.button_seconds
        }
        holder.toggleButtonGroup.check(buttonId)
        setupNumberPicker(holder)

        holder.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                timeUnit = when (checkedId) {
                    R.id.button_hours -> TimeUnit.HOURS
                    R.id.button_minutes -> TimeUnit.MINUTES
                    R.id.button_seconds -> TimeUnit.SECONDS
                    else -> TimeUnit.MINUTES
                }
                setupNumberPicker(holder)
                notifyTimeChange()
            }
        }
        notifyTimeChange()
    }

    private fun setupNumberPicker(holder: ViewHolder) {
        holder.numberPicker.apply {
            minValue = 0
            maxValue = when (timeUnit) {
                TimeUnit.HOURS -> 24
                TimeUnit.MINUTES -> 59
                TimeUnit.SECONDS -> 59
            }
            value = when (timeUnit) {
                TimeUnit.HOURS -> selectedHour
                TimeUnit.MINUTES -> selectedMinute
                TimeUnit.SECONDS -> selectedSecond
            }
            wrapSelectorWheel = true
            setOnValueChangedListener { _, _, newVal ->
                when (timeUnit) {
                    TimeUnit.HOURS -> selectedHour = newVal
                    TimeUnit.MINUTES -> selectedMinute = newVal
                    TimeUnit.SECONDS -> selectedSecond = newVal
                }
                notifyTimeChange()
            }
        }
    }

    private fun notifyTimeChange() {
        val totalSeconds = when (timeUnit) {
            TimeUnit.HOURS -> selectedHour * 3600
            TimeUnit.MINUTES -> selectedMinute * 60
            TimeUnit.SECONDS -> selectedSecond
        }
        onCooldownTimeChanged(totalSeconds)
    }

    override fun getItemCount(): Int = items.size

    fun reset() {
        isReset = true
        selectedHour = 0
        selectedMinute = 0
        selectedSecond = 0
        timeUnit = TimeUnit.MINUTES
        notifyDataSetChanged()
    }
}