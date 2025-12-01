package org.kvxd.simplesoundboard.config

import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    var playLocally: Boolean = true,
    var playWhileMuted: Boolean = true,
    val sounds: MutableMap<String, SoundData> = mutableMapOf()
)