package org.kvxd.simplesoundboard.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.CyclingButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.kvxd.simplesoundboard.SimpleSoundboardClient
import org.kvxd.simplesoundboard.YtDlpManager
import java.awt.Color
import java.util.concurrent.CompletableFuture

class YtDlpScreen(private val parent: Screen?) : Screen(Text.literal("YouTube Downloader")) {

    private lateinit var urlField: TextFieldWidget
    private lateinit var audioToggle: CyclingButtonWidget<Boolean>
    private lateinit var downloadBtn: ButtonWidget

    private lateinit var statusLabel: TextWidget
    private lateinit var logLabel: TextWidget

    private var progress: Int = 0

    override fun init() {
        val padding = 10
        val contentWidth = width - padding * 2

        urlField =
            TextFieldWidget(textRenderer, padding, 30, contentWidth, 20, Text.literal("Paste YouTube / media URL"))
        addDrawableChild(urlField)

        audioToggle = CyclingButtonWidget.onOffBuilder(true)
            .build(padding, 60, 120, 20, Text.literal("Audio only")) { _, _ -> }
        addDrawableChild(audioToggle)

        downloadBtn = ButtonWidget.builder(Text.literal("Download")) {
            val url = urlField.text.trim()
            if (url.isBlank()) {
                client?.player?.sendMessage(
                    Text.literal("Please provide a URL").formatted(Formatting.RED),
                    false
                )
                return@builder
            }
            startDownload(url, audioToggle.value)
        }.size(120, 20).position(width - padding - 120, 60).build()
        addDrawableChild(downloadBtn)

        addDrawableChild(
            ButtonWidget.builder(Text.translatable("gui.done")) { close() }
                .size(150, 20)
                .position(width / 2 - 75, height - 23)
                .build()
        )

        statusLabel = TextWidget(Text.literal(" "), textRenderer)
        statusLabel.setPosition(padding, 90)
        addDrawableChild(statusLabel)

        logLabel = TextWidget(Text.literal(" "), textRenderer)
        logLabel.setPosition(padding, 110)
        addDrawableChild(logLabel)
    }

    private fun startDownload(url: String, audioOnly: Boolean) {
        statusLabel.message = Text.literal("Downloading…")
        progress = 0

        CompletableFuture.runAsync {
            val result = YtDlpManager.downloadUrlIntoSoundDir(
                url,
                audioOnly
            ) { line ->
                val p = extractProgress(line)

                client?.execute {
                    if (p != null) {
                        progress = p
                    }

                    logLabel.message = Text.literal(line.take(80))
                }
            }

            client?.execute {
                if (result.first) {
                    statusLabel.message = Text.literal("Finished!").formatted(Formatting.GREEN)
                    progress = 100
                } else {
                    statusLabel.message = Text.literal("Failed").formatted(Formatting.RED)
                }
            }
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        context.drawCenteredTextWithShadow(textRenderer, title.asOrderedText(), width / 2, 8, 0xFFFFFF)

        val barX = 10
        val barY = 140
        val barW = width - 20
        val filled = (barW * (progress / 100f)).toInt()

        context.fill(barX, barY, barX + barW, barY + 10, 0xFF555555.toInt())
        context.fill(barX, barY, barX + filled, barY + 10, 0xFF00AA00.toInt())

        context.drawText(
            textRenderer,
            Text.literal("Save folder: ${SimpleSoundboardClient.soundDir.absolutePath}").asOrderedText(),
            10,
            height - 40,
            Color.WHITE.rgb,
            false
        )
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun extractProgress(line: String): Int? {
        val match = Regex("""(\d{1,3}\.\d)%""").find(line)
            ?: Regex("""(\d{1,3})%""").find(line)

        return match?.groupValues?.get(1)?.toFloatOrNull()?.toInt()
    }
}
