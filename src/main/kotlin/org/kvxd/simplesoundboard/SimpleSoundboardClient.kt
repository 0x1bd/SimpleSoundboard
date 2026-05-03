package org.kvxd.simplesoundboard

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.resources.Identifier
import org.kvxd.simplesoundboard.config.SoundboardConfig
import org.kvxd.simplesoundboard.gui.SoundboardScreen
import java.io.File

class SimpleSoundboardClient : ClientModInitializer {

    companion object {

        const val MOD_ID = "simplesoundboard"

        val KEY_CATEGORY: KeyMapping.Category =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))

        lateinit var OPEN_GUI_KEY: KeyMapping

        private val pressedKeys = mutableSetOf<Int>()

        val soundDir = File(FabricLoader.getInstance().gameDir.toFile(), "soundboard")
        val modDir = File(FabricLoader.getInstance().gameDir.toFile(), "soundboard")
    }

    override fun onInitializeClient() {
        OPEN_GUI_KEY = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.$MOD_ID.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_J,
                KEY_CATEGORY
            )
        )

        if (!soundDir.exists())
            soundDir.mkdirs()

        if (!modDir.exists())
            modDir.mkdirs()

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player == null) return@register

            if (OPEN_GUI_KEY.consumeClick()) {
                client.setScreen(SoundboardScreen())
            }

            if (client.screen == null) {
                handleSoundKeybinds(client)
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            SoundboardConfig.save()
        }
    }

    private fun handleSoundKeybinds(client: Minecraft) {
        for ((filename, data) in SoundboardConfig.data.sounds) {
            val keyCode = data.keybind
            if (keyCode <= 0 || keyCode == InputConstants.KEY_ESCAPE) continue

            val isPressed = InputConstants.isKeyDown(client.window, keyCode)
            val wasPressed = pressedKeys.contains(keyCode)

            if (isPressed && !wasPressed) {
                pressedKeys.add(keyCode)

                val file = File(soundDir, filename)
                if (file.exists()) {
                    if (SoundboardAudioSystem.isPlaying(filename)) {
                        SoundboardAudioSystem.stop(filename)
                    } else {
                        SoundboardAudioSystem.playFile(file, data.localVolume, data.playerVolume)
                    }
                }
            } else if (!isPressed && wasPressed) {
                pressedKeys.remove(keyCode)
            }
        }
    }
}
