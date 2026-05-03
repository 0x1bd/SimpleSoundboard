package org.kvxd.simplesoundboard.gui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.kvxd.simplesoundboard.SimpleSoundboardClient
import org.kvxd.simplesoundboard.YtDlpManager
import java.awt.Color
import java.util.concurrent.CompletableFuture

class YtDlpScreen(private val parent: Screen?) : Screen(Component.literal("YouTube Downloader")) {

    private lateinit var urlField: EditBox
    private lateinit var audioToggle: CycleButton<Boolean>
    private lateinit var downloadBtn: Button

    private lateinit var statusLabel: StringWidget
    private lateinit var logLabel: StringWidget

    private var progress: Int = 0

    override fun init() {
        val padding = 10
        val contentWidth = width - padding * 2

        urlField =
            EditBox(font, padding, 30, contentWidth, 20, Component.literal("Paste YouTube / media URL"))
        addRenderableWidget(urlField)

        audioToggle = CycleButton.onOffBuilder(true)
            .create(padding, 60, 120, 20, Component.literal("Audio only")) { _, _ -> }
        addRenderableWidget(audioToggle)

        downloadBtn = Button.builder(Component.literal("Download")) {
            val url = urlField.value.trim()
            if (url.isBlank()) {
                minecraft?.player?.displayClientMessage(
                    Component.literal("Please provide a URL").withStyle(ChatFormatting.RED),
                    false
                )
                return@builder
            }
            startDownload(url, audioToggle.value)
        }.size(120, 20).pos(width - padding - 120, 60).build()
        addRenderableWidget(downloadBtn)

        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) { onClose() }
                .size(150, 20)
                .pos(width / 2 - 75, height - 23)
                .build()
        )

        statusLabel = StringWidget(Component.literal(" "), font)
        statusLabel.setPosition(padding, 90)
        addRenderableWidget(statusLabel)

        logLabel = StringWidget(Component.literal(" "), font)
        logLabel.setPosition(padding, 110)
        addRenderableWidget(logLabel)
    }

    private fun startDownload(url: String, audioOnly: Boolean) {
        statusLabel.message = Component.literal("Downloading…")
        progress = 0

        CompletableFuture.runAsync {
            val result = YtDlpManager.downloadUrlIntoSoundDir(
                url,
                audioOnly
            ) { line ->
                val p = extractProgress(line)

                minecraft?.execute {
                    if (p != null) {
                        progress = p
                    }

                    logLabel.message = Component.literal(line.take(80))
                }
            }

            minecraft?.execute {
                if (result.first) {
                    statusLabel.message = Component.literal("Finished!").withStyle(ChatFormatting.GREEN)
                    progress = 100
                } else {
                    statusLabel.message = Component.literal("Failed").withStyle(ChatFormatting.RED)
                }
            }
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        context.drawCenteredString(font, title.visualOrderText, width / 2, 8, 0xFFFFFF)

        val barX = 10
        val barY = 140
        val barW = width - 20
        val filled = (barW * (progress / 100f)).toInt()

        context.fill(barX, barY, barX + barW, barY + 10, 0xFF555555.toInt())
        context.fill(barX, barY, barX + filled, barY + 10, 0xFF00AA00.toInt())

        context.drawString(
            font,
            Component.literal("Save folder: ${SimpleSoundboardClient.soundDir.absolutePath}").visualOrderText,
            10,
            height - 40,
            Color.WHITE.rgb,
            false
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    private fun extractProgress(line: String): Int? {
        val match = Regex("""(\d{1,3}\.\d)%""").find(line)
            ?: Regex("""(\d{1,3})%""").find(line)

        return match?.groupValues?.get(1)?.toFloatOrNull()?.toInt()
    }
}