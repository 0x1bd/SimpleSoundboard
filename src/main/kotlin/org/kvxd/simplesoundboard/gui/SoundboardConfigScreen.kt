package org.kvxd.simplesoundboard.gui

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.text.Text
import org.kvxd.simplesoundboard.SoundboardConfig

class SoundboardConfigScreen(private val parent: Screen?) : Screen(Text.literal("Soundboard Configuration")) {

    override fun init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back")) { close() }
            .position(width / 2 - 100, height - 30)
            .size(200, 20)
            .build()
        )

        addDrawableChild(
            CyclingButtonWidget.onOffBuilder(SoundboardConfig.playLocally)
                .build(width / 2 - 100, 50, 200, 20, Text.literal("Play Locally")) { _, value ->
                    SoundboardConfig.playLocally = value
                    SoundboardConfig.save()
                }
        )

        addDrawableChild(
            CyclingButtonWidget.onOffBuilder(SoundboardConfig.playWhileMuted)
                .build(width / 2 - 100, 75, 200, 20, Text.literal("Play While Muted")) { _, value ->
                    SoundboardConfig.playWhileMuted = value
                    SoundboardConfig.save()
                }
        )
    }

    override fun close() {
        client?.setScreen(parent)
    }
}