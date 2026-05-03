package org.kvxd.simplesoundboard.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.input.KeyEvent
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.Util
import org.kvxd.simplesoundboard.SimpleSoundboardClient
import org.kvxd.simplesoundboard.SoundboardAudioSystem
import org.kvxd.simplesoundboard.config.SoundboardConfig
import java.awt.Color
import java.io.File

class SoundboardScreen(
    private val parent: Screen? = null
) : Screen(Component.literal("Soundboard")) {

    private val mc = Minecraft.getInstance()

    private val bottomPaneHeight = 65
    private val headerHeight = 55

    private lateinit var queryField: EditBox
    private lateinit var resultsList: ResultListWidget

    private lateinit var detailLocalSlider: VolumeSlider
    private lateinit var detailPlayerSlider: VolumeSlider
    private lateinit var detailBindBtn: Button
    private lateinit var detailLabel: StringWidget

    private var selectedFile: File? = null
    private var isBinding = false
    private var results: List<File> = emptyList()

    override fun init() {
        val padding = 10
        val searchFieldWidth = width - 2 * padding
        val contentWidth = width - 2 * padding

        val titleWidget = StringWidget(Component.literal("Soundboard").withStyle(ChatFormatting.BOLD), font)
        titleWidget.setPosition(width / 2 - titleWidget.width / 2, 5)
        addRenderableWidget(titleWidget)

        queryField = EditBox(font, padding, 20, searchFieldWidth, 20, Component.literal("Search"))
        queryField.setResponder { scanSounds() }
        addRenderableWidget(queryField)

        setupTopButtons()

        val detailsY = height - bottomPaneHeight + 5

        detailLabel = StringWidget(Component.literal("Select a sound to edit settings"), font)
        detailLabel.setPosition(width / 2 - detailLabel.width / 2, detailsY)
        addRenderableWidget(detailLabel)

        detailLocalSlider = VolumeSlider(width / 2 - 155, detailsY + 15, 100, 20, Component.literal("Local"), 1.0f) {
            updateSelectedVolume(local = it)
        }
        detailLocalSlider.active = false
        addRenderableWidget(detailLocalSlider)

        detailPlayerSlider = VolumeSlider(width / 2 - 50, detailsY + 15, 100, 20, Component.literal("Player"), 1.0f) {
            updateSelectedVolume(player = it)
        }
        detailPlayerSlider.active = false
        addRenderableWidget(detailPlayerSlider)

        detailBindBtn = Button.builder(Component.literal("Keybind: None")) {
            if (selectedFile != null) {
                isBinding = true
                it.message = Component.literal("> Press Key <").withStyle(ChatFormatting.YELLOW)
            }
        }.size(100, 20).pos(width / 2 + 55, detailsY + 15).build()
        detailBindBtn.active = false
        addRenderableWidget(detailBindBtn)

        addRenderableWidget(Button.builder(Component.translatable("gui.done")) { onClose() }
            .size(150, 20)
            .pos(width / 2 - 75, height - 23)
            .build())

        val listTop = headerHeight + 14
        val listBottom = height - bottomPaneHeight
        val itemHeight = 22

        resultsList = ResultListWidget(mc, contentWidth, listBottom - listTop, listTop, itemHeight)
        resultsList.setX(padding)
        addRenderableWidget(resultsList)

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

        addRenderableWidget(
            Button.builder(Component.literal("Stop All").withStyle(ChatFormatting.RED)) {
                SoundboardAudioSystem.stopAll()
            }.size(actionButtonWidth, actionButtonHeight)
                .pos(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addRenderableWidget(
            Button.builder(Component.literal("Refresh")) {
                scanSounds()
            }.size(actionButtonWidth, actionButtonHeight)
                .pos(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addRenderableWidget(
            Button.builder(Component.literal("Folder")) {
                Util.getPlatform().openFile(SimpleSoundboardClient.soundDir)
            }.size(actionButtonWidth, actionButtonHeight)
                .pos(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addRenderableWidget(
            Button.builder(Component.literal("Config")) {
                mc.setScreen(SoundboardConfigScreen(this))
            }.size(actionButtonWidth, actionButtonHeight)
                .pos(currentX, actionsY).build()
        )
        currentX += actionButtonWidth + buttonSpacing

        addRenderableWidget(
            Button.builder(Component.literal("YouTube")) {
                mc.setScreen(YtDlpScreen(this))
            }.size(actionButtonWidth, actionButtonHeight)
                .pos(currentX, actionsY).build()
        )
    }

    private fun scanSounds() {
        val query = queryField.value.trim().lowercase()
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
            detailLabel.message = Component.literal("Select a sound to edit settings").withStyle(ChatFormatting.GRAY)
            detailLabel.x = width / 2 - font.width(detailLabel.message) / 2

            detailLocalSlider.active = false
            detailPlayerSlider.active = false
            detailBindBtn.active = false
            detailBindBtn.message = Component.literal("Keybind: -")
        } else {
            val data = SoundboardConfig[file.name]

            detailLabel.message = Component.literal("Settings: ${file.name}").withStyle(ChatFormatting.YELLOW)
            detailLabel.x = width / 2 - font.width(detailLabel.message) / 2

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
                InputConstants.getKey(KeyEvent(keyCode, 0, 0)).displayName
            else
                Component.literal("None")

        detailBindBtn.message = Component.literal("Keybind: ").append(keyName)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)

        val lineY = height - bottomPaneHeight
        graphics.fill(10, lineY, width - 10, lineY + 1, Color.GRAY.rgb)
    }

    override fun onClose() {
        SoundboardConfig.save()
        mc.setScreen(parent)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (isBinding && selectedFile != null) {
            val file = selectedFile!!.name
            val data = SoundboardConfig[file]

            if (input.key == InputConstants.KEY_ESCAPE) {
                data.keybind = -1
            } else {
                data.keybind = input.key()
            }

            SoundboardConfig.save()
            updateBindButtonText(data.keybind)
            isBinding = false

            return true
        }

        if (input.key == InputConstants.KEY_ESCAPE) {
            onClose()
            return true
        }

        return super.keyPressed(input)
    }

    private inner class ResultListWidget(
        client: Minecraft, width: Int, height: Int, y: Int, itemHeight: Int
    ) : ContainerObjectSelectionList<ResultListWidget.Entry>(client, width, height, y, itemHeight) {

        fun setResults(results: List<File>) {
            clearEntries()
            results.forEach { addEntry(Entry(it)) }
            setScrollAmount(0.0)
        }

        override fun getRowWidth(): Int = width - 20

        inner class Entry(private val file: File) : ContainerObjectSelectionList.Entry<Entry>() {

            private val playBtn: Button
            private val favBtn: Button
            private val elements = mutableListOf<GuiEventListener>()
            private val selectables = mutableListOf<NarratableEntry>()

            init {
                val data = SoundboardConfig[file.name]

                favBtn = Button.builder(
                    Component.literal(if (data.favorite) "★" else "☆")
                        .withStyle(if (data.favorite) ChatFormatting.GOLD else ChatFormatting.GRAY)
                ) {
                    data.favorite = !data.favorite
                    SoundboardConfig.save()
                    scanSounds()
                }.size(20, 20).build()

                playBtn = Button.builder(Component.literal("Play")) {
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

            override fun extractContent(
                graphics: GuiGraphicsExtractor,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                a: Float
            ) {
                val isPlaying = SoundboardAudioSystem.isPlaying(file.name)
                playBtn.message =
                    if (isPlaying) Component.literal("Stop").withStyle(ChatFormatting.RED) else Component.literal("Play")

                if (selectedFile?.name == file.name) {
                    graphics.fill(x, y + 1, x + width, y + height - 1, 0x33FFFFFF)
                }

                val textY = y + (height - font.lineHeight) / 2 + 1
                var nameText = file.nameWithoutExtension

                if (font.width(nameText) > width - 70) {
                    nameText = font.plainSubstrByWidth(nameText, width - 75) + "..."
                }

                graphics.text(font, nameText, x + 25, textY, Color.WHITE.rgb, true)

                favBtn.x = x
                favBtn.y = y + (height - 20) / 2
                favBtn.extractRenderState(graphics, mouseX, mouseY, a)

                playBtn.x = x + width - playBtn.width
                playBtn.y = y + (height - 20) / 2
                playBtn.extractRenderState(graphics, mouseX, mouseY, a)
            }

            override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
                if (favBtn.mouseClicked(click, doubled)) return true
                if (playBtn.mouseClicked(click, doubled)) return true

                selectSound(file)
                return true
            }

            override fun children() = elements
            override fun narratables() = selectables
        }
    }
}