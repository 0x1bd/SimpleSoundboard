package org.kvxd.simplesoundboard

import net.minecraft.util.Util
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.TimeUnit

object YtDlpManager {

    private val isWindows = Util.getOperatingSystem().getName().contains("windows", ignoreCase = true)
    private val binaryName = if (isWindows) "yt-dlp.exe" else "yt-dlp"
    private val downloadUrl = if (isWindows)
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
    else
        "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"

    private fun binFile(): File {
        if (!SimpleSoundboardClient.modDir.exists()) SimpleSoundboardClient.modDir.mkdirs()
        return File(SimpleSoundboardClient.modDir, binaryName)
    }

    @Synchronized
    fun ensureYtDlpPresent(): Boolean {
        val bin = binFile()
        if (bin.exists() && bin.canExecute()) return true

        return try {
            downloadBinary(bin)

            bin.setExecutable(true, false)
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    @Throws(Exception::class)
    private fun downloadBinary(dest: File) {
        val url = URI(downloadUrl).toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "SimpleSoundboard-yt-dlp")
        }

        conn.connect()
        val code = conn.responseCode
        if (code >= 400) {
            conn.disconnect()
            throw RuntimeException("Failed to download yt-dlp: HTTP $code")
        }

        BufferedInputStream(conn.inputStream).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        conn.disconnect()
        dest.setExecutable(true, false)
    }

    fun downloadUrlIntoSoundDir(
        url: String,
        audioOnly: Boolean = true,
        onProgress: (String) -> Unit = {}
    ): Pair<Boolean, String> {
        if (url.isBlank()) return Pair(false, "Empty URL")

        if (!ensureYtDlpPresent()) {
            return Pair(false, "Failed to ensure yt-dlp binary is available.")
        }

        val bin = binFile()
        val soundDir = SimpleSoundboardClient.soundDir.also { if (!it.exists()) it.mkdirs() }

        val outputPattern = File(soundDir, "%(title)s.%(ext)s").absolutePath
        val args = mutableListOf<String>()

        args.add(bin.absolutePath)

        if (audioOnly) {
            args.addAll(listOf("-x", "--audio-format", "mp3"))
        }

        args.addAll(listOf("-o", outputPattern, url))

        try {
            val pb = ProcessBuilder(args).apply {
                directory(soundDir)
                redirectErrorStream(true)
            }

            val proc = pb.start()

            val output = StringBuilder()
            BufferedReader(InputStreamReader(proc.inputStream)).use { r ->
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    output.appendLine(line)

                    onProgress(line!!)
                }
            }

            val finished = proc.waitFor(10, TimeUnit.MINUTES)
            if (!finished) {
                proc.destroyForcibly()
                return Pair(false, "yt-dlp timed out.\n${output}")
            }

            val exit = proc.exitValue()
            val outStr = output.toString()
            return if (exit == 0) {
                Pair(true, outStr.ifBlank { "Download completed." })
            } else {
                Pair(false, "yt-dlp exit code $exit\n$outStr")
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return Pair(false, "Exception: ${t.message}")
        }
    }

}