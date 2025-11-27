# Simple Soundboard

A simple soundboard mod for [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)

This mod allows you to play .mp3 files directly into your voice chat stream. It features a GUI to manage your sounds,
with separate volume controls for what you hear and what other players hear.

## Features

- **MP3 Support**: Play standard `.mp3` files.
- **Microphone Injection**: Sounds are merged into your microphone stream, so anyone with Simple Voice Chat can hear
  them.
- **Dual Volume Sliders**:
    - **Player Volume**: How loud the sound is for other people.
    - **Local Volume**: How loud the sound is for you.

## Dependencies

[Fabric Loader](https://fabricmc.net)
[Fabric API](https://modrinth.com/mod/fabric-api)
[Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat)

## Installation

1. Download and install **Fabric** and **Simple Voice Chat**.
2. Download Simple Soundboard from [modrinth](https://modrinth.com/project/simplesoundboard).
3. Drop the downloaded jar file into your `mods` folder.
4. Launch Minecraft.

## Usage

1. **Open the Folder:**
    * Launch the game once to generate the directories.
    * Navigate to your Minecraft instance folder (e.g., `.minecraft`).
    * Open the `soundboard` folder.
2. **Add Sounds:**
    * Drag and drop your `.mp3` files into the `soundboard` folder.
3. **Play:**
    * Join a world or server.
    * Press **`J`** (default key) to open the Soundboard GUI.
    * Click "Play" on a sound.

## Configuration

You can access the configuration via the **Config** button in the soundboard GUI.

*   **Play Locally:** Toggle this **ON** to hear the sounds yourself, or **OFF** to play them only for other players.
*   **Volume Memory:** The mod remembers the specific volume settings (Local vs Player) for every individual sound file.