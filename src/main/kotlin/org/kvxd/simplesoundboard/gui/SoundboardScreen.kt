package org.kvxd.simplesoundboard.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.widget.TextWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import org.kvxd.simplesoundboard.SoundboardAudioSystem
import org.kvxd.simplesoundboard.SoundboardConfig
import org.lwjgl.glfw.GLFW
import java.awt.Color
import java.io.File
import kotlin.math.roundToInt

class SoundboardScreen(
    private val parent: Screen? = null
) : Screen(Text.literal("Soundboard")) {

    private val mc = MinecraftClient.getInstance()
    private val soundDir = File(mc.runDirectory, "soundboard")

    private lateinit var queryField: TextFieldWidget
    private lateinit var resultsList: ResultListWidget
    private lateinit var stopButton: ButtonWidget
    private lateinit var refreshButton: ButtonWidget
    private lateinit var openFolderButton: ButtonWidget
    private lateinit var configButton: ButtonWidget
    private lateinit var doneButton: ButtonWidget

    private var results: List<File> = emptyList()

    override fun init() {
        if (!soundDir.exists()) soundDir.mkdirs()

        val padding = 10
        val actionButtonWidth = 80
        val actionButtonHeight = 20
        val buttonSpacing = 5
        val searchFieldWidth = width - 2 * padding

        val titleWidget = TextWidget(Text.literal("Soundboard").formatted(Formatting.BOLD), textRenderer)
        titleWidget.setPosition(width / 2 - titleWidget.width / 2, 5)
        addDrawableChild(titleWidget)

        queryField = TextFieldWidget(textRenderer, padding, 25, searchFieldWidth, 20, Text.literal("Search"))
        queryField.setChangedListener { scanSounds() }
        addDrawableChild(queryField)

        val actionsY = 50
        val totalActionWidth = (actionButtonWidth * 4) + (buttonSpacing * 3)
        var currentX = width / 2 - totalActionWidth / 2

        stopButton = ButtonWidget.builder(Text.literal("Stop All").formatted(Formatting.RED)) {
            SoundboardAudioSystem.stopAll()
        }.size(actionButtonWidth, actionButtonHeight).position(currentX, actionsY).build()
        addDrawableChild(stopButton)
        currentX += actionButtonWidth + buttonSpacing

        refreshButton = ButtonWidget.builder(Text.literal("Refresh")) {
            scanSounds()
        }.size(actionButtonWidth, actionButtonHeight).position(currentX, actionsY).build()
        addDrawableChild(refreshButton)
        currentX += actionButtonWidth + buttonSpacing

        openFolderButton = ButtonWidget.builder(Text.literal("Folder")) {
            Util.getOperatingSystem().open(soundDir)
        }.size(actionButtonWidth, actionButtonHeight).position(currentX, actionsY).build()
        addDrawableChild(openFolderButton)
        currentX += actionButtonWidth + buttonSpacing

        configButton = ButtonWidget.builder(Text.literal("Config")) {
            mc.setScreen(SoundboardConfigScreen(this))
        }.size(actionButtonWidth, actionButtonHeight).position(currentX, actionsY).build()
        addDrawableChild(configButton)

        doneButton = ButtonWidget.builder(Text.translatable("gui.done")) { close() }
            .size(150, 20)
            .position(width / 2 - 75, height - 30)
            .build()
        addDrawableChild(doneButton)

        val listTop = actionsY + actionButtonHeight + 10
        val listBottom = height - 50
        val listWidth = width - 2 * padding

        val itemHeight = 35

        resultsList = ResultListWidget(mc, listWidth, listBottom - listTop, listTop, itemHeight)
        resultsList.setX(padding)
        addDrawableChild(resultsList)

        scanSounds()
        setInitialFocus(queryField)
    }

    private fun scanSounds() {
        val query = queryField.text.trim().lowercase()
        val allFiles = soundDir.listFiles { _, name -> name.endsWith(".mp3") } ?: emptyArray()

        results = allFiles.filter { it.name.lowercase().contains(query) }.sortedBy { it.name }
        resultsList.setResults(results)
    }

    override fun close() {
        SoundboardConfig.save()
        mc.setScreen(parent)
    }

    override fun keyPressed(input: KeyInput): Boolean {
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

        private inner class Entry(private val file: File) : ElementListWidget.Entry<Entry>() {

            private val playBtn: ButtonWidget
            private val localSlider: SimpleVolumeSlider
            private val playerSlider: SimpleVolumeSlider
            private val elements = mutableListOf<Element>()
            private val selectables = mutableListOf<Selectable>()

            private var focusedWidget: Element? = null

            init {
                val savedVol = SoundboardConfig.getVolume(file.name)

                playBtn = ButtonWidget.builder(Text.literal("Play")) {
                    SoundboardAudioSystem.playFile(file, savedVol.local, savedVol.player)
                }.size(40, 20).build()

                localSlider = SimpleVolumeSlider(0, 0, 100, 20, Text.literal("Local"), savedVol.local) { newVal ->
                    savedVol.local = newVal
                    SoundboardConfig.save()
                }

                playerSlider = SimpleVolumeSlider(0, 0, 100, 20, Text.literal("Player"), savedVol.player) { newVal ->
                    savedVol.player = newVal
                    SoundboardConfig.save()
                }

                elements.add(playBtn)
                elements.add(localSlider)
                elements.add(playerSlider)
                selectables.addAll(listOf(playBtn, localSlider, playerSlider))
            }

            override fun render(context: DrawContext, mouseX: Int, mouseY: Int, hovered: Boolean, deltaTicks: Float) {
                context.drawText(
                    textRenderer,
                    file.nameWithoutExtension,
                    x + 2,
                    y + 5,
                    Color.WHITE.rgb,
                    true
                )

                val btnWidth = 40
                val sliderWidth = 80
                val gap = 5

                var currentRight = x + width - 5

                playBtn.x = currentRight - btnWidth
                playBtn.y = y + (height - 20) / 2
                playBtn.render(context, mouseX, mouseY, deltaTicks)
                currentRight -= (btnWidth + gap)

                playerSlider.x = currentRight - sliderWidth
                playerSlider.y = y + (height - 20) / 2
                playerSlider.width = sliderWidth
                playerSlider.render(context, mouseX, mouseY, deltaTicks)
                currentRight -= (sliderWidth + gap)

                localSlider.x = currentRight - sliderWidth
                localSlider.y = y + (height - 20) / 2
                localSlider.width = sliderWidth
                localSlider.render(context, mouseX, mouseY, deltaTicks)
            }

            override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
                focusedWidget = null

                if (playBtn.isMouseOver(click.x, click.y)) {
                    if (playBtn.mouseClicked(click, doubled)) {
                        return true
                    }
                }

                if (localSlider.isMouseOver(click.x, click.y)) {
                    if (localSlider.mouseClicked(click, doubled)) {
                        focusedWidget = localSlider
                        return true
                    }
                }

                if (playerSlider.isMouseOver(click.x, click.y)) {
                    if (playerSlider.mouseClicked(click, doubled)) {
                        focusedWidget = playerSlider
                        return true
                    }
                }

                return false
            }

            override fun mouseReleased(click: Click): Boolean {
                if (focusedWidget != null) {
                    val result = focusedWidget!!.mouseReleased(click)
                    focusedWidget = null
                    return result
                }
                return false
            }

            override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
                if (focusedWidget != null) {
                    return focusedWidget!!.mouseDragged(click, offsetX, offsetY)
                }
                return false
            }

            override fun children(): List<Element> = elements
            override fun selectableChildren(): List<Selectable> = selectables
        }
    }

    class SimpleVolumeSlider(
        x: Int, y: Int, width: Int, height: Int,
        private val prefix: Text,
        initialValue: Float,
        private val onChange: (Float) -> Unit
    ) : SliderWidget(x, y, width, height, Text.empty(), initialValue.toDouble()) {

        init {
            updateMessage()
        }

        override fun updateMessage() {
            val percent = (value * 100).roundToInt()
            message = Text.literal("").append(prefix).append(": ${percent}%")
        }

        override fun applyValue() {
            onChange(value.toFloat())
        }
    }
}