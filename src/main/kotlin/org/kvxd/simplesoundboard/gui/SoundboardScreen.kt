package org.kvxd.simplesoundboard.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.kvxd.simplesoundboard.SimpleSoundboardClient
import org.kvxd.simplesoundboard.SoundboardAudioSystem
import org.kvxd.simplesoundboard.config.SoundboardConfig
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.io.File

class SoundboardScreen(
    private val parent: Screen? = null
) : Screen(Text.literal("Soundboard")) {

    private val mc = MinecraftClient.getInstance()

    private val bottomPaneHeight = 65
    private val headerHeight = 55

    private lateinit var queryField: TextFieldWidget
    private lateinit var resultsList: ResultListWidget

    private lateinit var detailLocalSlider: VolumeSlider
    private lateinit var detailPlayerSlider: VolumeSlider
    private lateinit var detailBindBtn: ButtonWidget
    private lateinit var detailLabel: TextWidget

    private var selectedFile: File? = null
    private var isBinding = false
    private var results: List<File> = emptyList()

    override fun init() {
        val padding = 10
        val searchFieldWidth = width - 2 * padding
        val contentWidth = width - 2 * padding

        val titleWidget = TextWidget(Text.literal("Soundboard").formatted(Formatting.BOLD), textRenderer)
        titleWidget.setPosition(width / 2 - titleWidget.width / 2, 5)
        addDrawableChild(titleWidget)

        queryField = TextFieldWidget(textRenderer, padding, 20, searchFieldWidth, 20, Text.literal("Search"))
        queryField.setChangedListener { scanSounds() }
        addDrawableChild(queryField)

        setupTopButtons()

        val detailsY = height - bottomPaneHeight + 5

        detailLabel = TextWidget(Text.literal("Select a sound to edit settings"), textRenderer)
        detailLabel.setPosition(width / 2 - detailLabel.width / 2, detailsY)
        addDrawableChild(detailLabel)

        detailLocalSlider = VolumeSlider(width / 2 - 155, detailsY + 15, 100, 20, Text.literal("Local"), 1.0f) {
            updateSelectedVolume(local = it)
        }
        detailLocalSlider.active = false
        addDrawableChild(detailLocalSlider)

        detailPlayerSlider = VolumeSlider(width / 2 - 50, detailsY + 15, 100, 20, Text.literal("Player"), 1.0f) {
            updateSelectedVolume(player = it)
        }
        detailPlayerSlider.active = false
        addDrawableChild(detailPlayerSlider)

        detailBindBtn = ButtonWidget.builder(Text.literal("Keybind: None")) {
            if (selectedFile != null) {
                isBinding = true
                it.message = Text.literal("> Press Key <").formatted(Formatting.YELLOW)
            }
        }.size(100, 20).position(width / 2 + 55, detailsY + 15).build()
        detailBindBtn.active = false
        addDrawableChild(detailBindBtn)

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done")) { close() }
            .size(150, 20)
            .position(width / 2 - 75, height - 23)
            .build())

        val listTop = headerHeight + 14
        val listBottom = height - bottomPaneHeight
        val itemHeight = 22

        resultsList = ResultListWidget(mc, contentWidth, listBottom - listTop, listTop, itemHeight)
        resultsList.setX(padding)
        addDrawableChild(resultsList)

        scanSounds()
        setInitialFocus(queryField)
    }

    private fun setupTopButtons() {
        val actionButtonWidth = 80
        val actionButtonHeight = 20
        val buttonSpacing = 5
        val actionsY = 45

        val totalActionWidth = (actionButtonWidth * 5) + (buttonSpacing * 4)
        var currentX = width / 2 - totalActionWidth / 2

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Stop All").formatted(Formatting.RED)) {
                SoundboardAudioSystem.stopAll()
            }.size(actionButtonWidth, actionButtonHeight)
                .position(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Refresh")) {
                scanSounds()
            }.size(actionButtonWidth, actionButtonHeight)
                .position(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Folder")) {
                Util.getOperatingSystem().open(SimpleSoundboardClient.soundDir)
            }.size(actionButtonWidth, actionButtonHeight)
                .position(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addDrawableChild(
            ButtonWidget.builder(Text.literal("Config")) {
                mc.setScreen(SoundboardConfigScreen(this))
            }.size(actionButtonWidth, actionButtonHeight)
                .position(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addDrawableChild(
            ButtonWidget.builder(Text.literal("YouTube")) {
                mc.setScreen(YtDlpScreen(this))
            }.size(actionButtonWidth, actionButtonHeight)
                .position(currentX, actionsY).build()
        )
    }

    private fun scanSounds() {
        val query = queryField.text.trim().lowercase()
        val allFiles = SimpleSoundboardClient.soundDir.listFiles { _, name -> name.endsWith(".mp3") } ?: emptyArray()

        results = allFiles.filter { it.name.lowercase().contains(query) }
            .sortedWith(compareByDescending<File> { SoundboardConfig[it.name].favorite }
                .thenBy { it.name })

        resultsList.setResults(results)

        if (selectedFile != null && results.none { it.name == selectedFile!!.name }) {
            selectSound(null)
        }
    }

    fun selectSound(file: File?) {
        this.selectedFile = file
        isBinding = false

        if (file == null) {
            detailLabel.message = Text.literal("Select a sound to edit settings").formatted(Formatting.GRAY)
            detailLabel.x = width / 2 - textRenderer.getWidth(detailLabel.message) / 2

            detailLocalSlider.active = false
            detailPlayerSlider.active = false
            detailBindBtn.active = false
            detailBindBtn.message = Text.literal("Keybind: -")
        } else {
            val data = SoundboardConfig[file.name]

            detailLabel.message = Text.literal("Settings: ${file.name}").formatted(Formatting.YELLOW)
            detailLabel.x = width / 2 - textRenderer.getWidth(detailLabel.message) / 2

            detailLocalSlider.active = true
            detailLocalSlider.setValue(data.localVolume)

            detailPlayerSlider.active = true
            detailPlayerSlider.setValue(data.playerVolume)

            detailBindBtn.active = true
            updateBindButtonText(data.keybind)
        }
    }

    private fun updateSelectedVolume(local: Float? = null, player: Float? = null) {
        val file = selectedFile ?: return
        val data = SoundboardConfig[file.name]

        if (local != null) data.localVolume = local
        if (player != null) data.playerVolume = player

        SoundboardConfig.save()
        SoundboardAudioSystem.setVolume(file.name, data.localVolume, data.playerVolume)
    }

    private fun updateBindButtonText(keyCode: Int) {
        val keyName =
            if (keyCode > 0)
                InputUtil.fromKeyCode(KeyInput(keyCode, 0, 0)).localizedText
            else
                Text.literal("None")

        detailBindBtn.message = Text.literal("Keybind: ").append(keyName)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val lineY = height - bottomPaneHeight
        context.fill(10, lineY, width - 10, lineY + 1, Color.GRAY.rgb)
    }

    override fun close() {
        SoundboardConfig.save()
        mc.setScreen(parent)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (isBinding && selectedFile != null) {
            val file = selectedFile!!.name
            val data = SoundboardConfig[file]

            if (input.key == GLFW.GLFW_KEY_ESCAPE) {
                data.keybind = -1
            } else {
                data.keybind = input.key()
            }

            SoundboardConfig.save()
            updateBindButtonText(data.keybind)
            isBinding = false

            return true
        }

        if (input.key == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }

        return super.keyPressed(input)
    }

    private inner class ResultListWidget(
        client: MinecraftClient, width: Int, height: Int, y: Int, itemHeight: Int
    ) : ElementListWidget<ResultListWidget.Entry>(client, width, height, y, itemHeight) {

        fun setResults(results: List<File>) {
            clearEntries()
            results.forEach { addEntry(Entry(it)) }
            scrollY = 0.0
        }

        override fun getRowWidth(): Int = width - 20

        inner class Entry(private val file: File) : ElementListWidget.Entry<Entry>() {

            private val playBtn: ButtonWidget
            private val favBtn: ButtonWidget
            private val elements = mutableListOf<Element>()
            private val selectables = mutableListOf<Selectable>()

            init {
                val data = SoundboardConfig[file.name]

                favBtn = ButtonWidget.builder(
                    Text.literal(if (data.favorite) "★" else "☆")
                        .formatted(if (data.favorite) Formatting.GOLD else Formatting.GRAY)
                ) {
                    data.favorite = !data.favorite
                    SoundboardConfig.save()
                    scanSounds()
                }.size(20, 20).build()

                playBtn = ButtonWidget.builder(Text.literal("Play")) {
                    if (SoundboardAudioSystem.isPlaying(file.name)) {
                        SoundboardAudioSystem.stop(file.name)
                    } else {
                        val currentData = SoundboardConfig[file.name]
                        SoundboardAudioSystem.playFile(file, currentData.localVolume, currentData.playerVolume)
                    }
                }.size(40, 20).build()

                elements.add(favBtn)
                elements.add(playBtn)
                selectables.add(favBtn)
                selectables.add(playBtn)
            }

            override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, deltaTicks: Float) {
                val isPlaying = SoundboardAudioSystem.isPlaying(file.name)
                playBtn.message =
                    if (isPlaying) Text.literal("Stop").formatted(Formatting.RED) else Text.literal("Play")

                if (selectedFile?.name == file.name) {
                    context.fill(x, y + 1, x + width, y + height - 1, 0x33FFFFFF)
                }

                val textY = y + (height - textRenderer.fontHeight) / 2 + 1
                var nameText = file.nameWithoutExtension

                if (textRenderer.getWidth(nameText) > width - 70) {
                    nameText = textRenderer.trimToWidth(nameText, width - 75) + "..."
                }

                context.drawText(textRenderer, nameText, x + 25, textY, Color.WHITE.rgb, true)

                favBtn.x = x
                favBtn.y = y + (height - 20) / 2
                favBtn.render(context, mouseX, mouseY, deltaTicks)

                playBtn.x = x + width - playBtn.width
                playBtn.y = y + (height - 20) / 2
                playBtn.render(context, mouseX, mouseY, deltaTicks)
            }

            override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
                if (favBtn.mouseClicked(click, doubled)) return true
                if (playBtn.mouseClicked(click, doubled)) return true

                selectSound(file)
                return true
            }

            override fun children() = elements
            override fun selectableChildren() = selectables
        }
    }
}