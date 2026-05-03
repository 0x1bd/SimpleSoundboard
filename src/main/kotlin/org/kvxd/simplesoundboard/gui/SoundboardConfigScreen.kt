package org.kvxd.simplesoundboard.gui

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.network.chat.Component
import org.kvxd.simplesoundboard.config.SoundboardConfig

class SoundboardConfigScreen(private val parent: Screen?) : Screen(Component.literal("Soundboard Configuration")) {

    override fun init() {
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }
            .pos(width / 2 - 100, height - 30)
            .size(200, 20)
            .build()
        )

        addRenderableWidget(
            CycleButton.onOffBuilder(SoundboardConfig.data.playLocally)
                .create(width / 2 - 100, 50, 200, 20, Component.literal("Play Locally")) { _, value ->
                    SoundboardConfig.data.playLocally = value
                    SoundboardConfig.save()
                }
        )

        addRenderableWidget(
            CycleButton.onOffBuilder(SoundboardConfig.data.playWhileMuted)
                .create(width / 2 - 100, 75, 200, 20, Component.literal("Play While Muted")) { _, value ->
                    SoundboardConfig.data.playWhileMuted = value
                    SoundboardConfig.save()
                }
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }
}