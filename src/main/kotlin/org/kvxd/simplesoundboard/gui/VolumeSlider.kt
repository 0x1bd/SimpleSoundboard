package org.kvxd.simplesoundboard.gui

import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.text.Text
import kotlin.math.roundToInt

class VolumeSlider(
    x: Int, y: Int, width: Int, height: Int,
    private val prefix: Text,
    initialValue: Float,
    private val onChange: (Float) -> Unit
) : SliderWidget(x, y, width, height, Text.empty(), initialValue.toDouble()) {

    init {
        updateMessage()
    }

    fun setValue(newValue: Float) {
        this.value = newValue.toDouble()
        updateMessage()
        applyValue()
    }

    override fun updateMessage() {
        val percent = (value * 100).roundToInt()
        message = Text.literal("").append(prefix).append(": ${percent}%")
    }

    override fun applyValue() {
        onChange(value.toFloat())
    }
}