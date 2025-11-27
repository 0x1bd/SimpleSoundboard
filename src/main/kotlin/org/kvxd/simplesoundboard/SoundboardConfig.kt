package org.kvxd.simplesoundboard

import com.google.gson.GsonBuilder
import net.minecraft.client.MinecraftClient
import java.io.File

object SoundboardConfig {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile = File(MinecraftClient.getInstance().runDirectory, "config/simplesoundboard.json")

    var playLocally: Boolean = true
    var soundVolumes: MutableMap<String, SoundVolume> = mutableMapOf()

    data class SoundVolume(var local: Float = 1.0f, var player: Float = 1.0f)

    init {
        load()
    }

    fun getVolume(filename: String): SoundVolume {
        return soundVolumes.computeIfAbsent(filename) { SoundVolume() }
    }

    fun save() {
        try {
            if (!configFile.parentFile.exists()) configFile.parentFile.mkdirs()
            val json = gson.toJson(ConfigData(playLocally, soundVolumes))
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
            soundVolumes = data.soundVolumes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class ConfigData(
        val playLocally: Boolean,
        val soundVolumes: MutableMap<String, SoundVolume>
    )
}