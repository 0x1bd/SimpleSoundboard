package org.kvxd.simplesoundboard

import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatClientApi
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.kvxd.simplesoundboard.config.SoundboardConfig
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

object SoundboardAudioSystem {

    private var api: VoicechatApi? = null
    private var clientApi: VoicechatClientApi? = null
    private var localAudioChannel: ClientStaticAudioChannel? = null

    private val activeSounds = ConcurrentLinkedQueue<PlayingSound>()
    private const val FRAME_SIZE = 960

    fun initialize(api: VoicechatApi) {
        this.api = api
    }

    fun onClientConnection(event: ClientVoicechatConnectionEvent) {
        if (event.isConnected) {
            clientApi = event.voicechat
            val category = clientApi!!.volumeCategoryBuilder()
                .setId("soundboard")
                .setName("Soundboard")
                .build()
            clientApi!!.registerClientVolumeCategory(category)

            localAudioChannel = clientApi!!.createStaticAudioChannel(UUID.randomUUID())
            localAudioChannel?.category = category.id
        } else {
            stopAll()
            clientApi = null
            localAudioChannel = null
        }
    }

    fun onMergeSound(event: MergeClientSoundEvent) {
        val api = clientApi ?: return

        if (api.isDisabled || (api.isMuted && !SoundboardConfig.data.playWhileMuted)) {
            if (activeSounds.isNotEmpty()) activeSounds.clear()
            return
        }

        if (activeSounds.isEmpty()) return

        val mixedAudioPlayer = ShortArray(FRAME_SIZE)
        val mixedAudioLocal = ShortArray(FRAME_SIZE)
        val playLocally = SoundboardConfig.data.playWhileMuted
        var hasAudio = false

        val iterator = activeSounds.iterator()
        while (iterator.hasNext()) {
            val sound = iterator.next()

            if (sound.isFinished) {
                iterator.remove()
                continue
            }

            hasAudio = true

            val samplesToRead = min(FRAME_SIZE, sound.remaining)

            for (i in 0 until samplesToRead) {
                val rawSample = sound.readNext()

                mixSample(mixedAudioPlayer, i, rawSample, sound.playerVolume)

                if (playLocally) {
                    mixSample(mixedAudioLocal, i, rawSample, sound.localVolume)
                }
            }
        }

        if (hasAudio) {
            event.mergeAudio(mixedAudioPlayer)

            if (playLocally) {
                localAudioChannel?.play(mixedAudioLocal)
            }
        }
    }

    private fun mixSample(buffer: ShortArray, index: Int, sample: Short, volume: Float) {
        val weightedSample = (sample * volume).toInt()
        var result = buffer[index] + weightedSample

        if (result > Short.MAX_VALUE) result = Short.MAX_VALUE.toInt()
        else if (result < Short.MIN_VALUE) result = Short.MIN_VALUE.toInt()

        buffer[index] = result.toShort()
    }

    fun playFile(file: File, localVol: Float, playerVol: Float) {
        val client = MinecraftClient.getInstance()
        val api = clientApi

        if (api == null) {
            client.player?.sendMessage(Text.of("§cVoice chat not connected!"), true)
            return
        }

        if (api.isMuted && !SoundboardConfig.data.playWhileMuted) {
            client.player?.sendMessage(Text.of("§cCannot play soundboard while muted!"), true)
            return
        }

        CompletableFuture.runAsync {
            try {
                val pcmData = decodeMp3(file)
                if (pcmData != null && pcmData.isNotEmpty()) {
                    activeSounds.add(PlayingSound(file.name, pcmData, localVol, playerVol))
                } else {
                    client.execute {
                        client.player?.sendMessage(Text.of("§cFailed to decode: ${file.name}"), false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isPlaying(file: String): Boolean {
        return activeSounds.any { it.name == file && !it.isFinished }
    }

    fun stop(file: String) {
        activeSounds.removeIf { it.name == file }
    }

    fun setVolume(file: String, localVol: Float, playerVol: Float) {
        for (sound in activeSounds) {
            if (sound.name == file) {
                sound.localVolume = localVol
                sound.playerVolume = playerVol
            }
        }
    }

    fun stopAll() {
        activeSounds.clear()
    }

    private fun decodeMp3(file: File): ShortArray? {
        val currentApi = api ?: return null

        return try {
            BufferedInputStream(Files.newInputStream(file.toPath())).use { stream ->
                val decoder = currentApi.createMp3Decoder(stream) ?: return null
                val rawPcm = decoder.decode()
                val format = decoder.audioFormat
                if (format.channels == 2) stereoToMono(rawPcm) else rawPcm
            }
        } catch (e: Exception) {
            System.err.println("Error decoding ${file.name}: ${e.message}")
            null
        }
    }

    private fun stereoToMono(stereo: ShortArray): ShortArray {
        val mono = ShortArray(stereo.size / 2)
        for (i in mono.indices) {
            val left = stereo[i * 2].toInt()
            val right = stereo[i * 2 + 1].toInt()
            mono[i] = ((left + right) / 2).toShort()
        }
        return mono
    }

    private class PlayingSound(
        val name: String,
        private val samples: ShortArray,
        @Volatile var localVolume: Float,
        @Volatile var playerVolume: Float
    ) {

        private var cursor = 0

        val isFinished: Boolean
            get() = cursor >= samples.size

        val remaining: Int
            get() = samples.size - cursor

        fun readNext(): Short {
            return if (cursor < samples.size) samples[cursor++] else 0
        }
    }
}