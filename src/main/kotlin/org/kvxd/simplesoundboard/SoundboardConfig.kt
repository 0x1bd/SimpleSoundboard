package org.kvxd.simplesoundboard

import com.google.gson.GsonBuilder
import net.minecraft.client.MinecraftClient
import java.io.File

object SoundboardConfig {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(MinecraftClient.getInstance().runDirectory, "config/simplesoundboard.json")

    var playLocally: Boolean = true
    var playWhileMuted: Boolean = false

    var sounds: MutableMap<String, SoundData> = mutableMapOf()

    data class SoundData(
        var localVolume: Float = 1.0f,
        var playerVolume: Float = 1.0f,
        var favorite: Boolean = false,
        var keybind: Int = -1
    )

    init {
        load()
    }

    operator fun get(filename: String): SoundData {
        return sounds.computeIfAbsent(filename) { SoundData() }
    }

    fun save() {
        try {
            if (!configFile.parentFile.exists()) configFile.parentFile.mkdirs()
            val json = gson.toJson(
                ConfigData(
                    playLocally,
                    playWhileMuted,
                    sounds
                )
            )
            configFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load() {
        if (!configFile.exists()) return
        try {
            val data = gson.fromJson(configFile.readText(), ConfigData::class.java)
            playLocally = data.playLocally
            playWhileMuted = data.playWhileMuted
            sounds = data.sounds ?: mutableMapOf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class ConfigData(
        val playLocally: Boolean,
        val playWhileMuted: Boolean,
        val sounds: MutableMap<String, SoundData>?
    )
}