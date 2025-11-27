package org.kvxd.simplesoundboard

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.util.Identifier
import org.kvxd.simplesoundboard.gui.SoundboardScreen
import org.lwjgl.glfw.GLFW

class SimpleSoundboardClient : ClientModInitializer {

    companion object {

        const val MOD_ID = "simplesoundboard"

        val KEY_CATEGORY: KeyBinding.Category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"))

        lateinit var OPEN_GUI_KEY: KeyBinding
    }

    override fun onInitializeClient() {
        OPEN_GUI_KEY = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.$MOD_ID.open",
                GLFW.GLFW_KEY_V,
                KEY_CATEGORY
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (OPEN_GUI_KEY.wasPressed()) {
                client.setScreen(SoundboardScreen())
            }
        }

        ClientLifecycleEvents.CLIENT_STOPPING.register {
            SoundboardConfig.save()
        }
    }
}
