package com.musicvibe.app

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.UUID

private const val MAX_MP3_BYTES = 150L * 1024L * 1024L

private fun musicFolder(context: Context): File =
    File(context.filesDir, "offline_mp3").apply { mkdirs() }

private fun offlineTracks(context: Context): List<File> =
    musicFolder(context).listFiles()
        ?.filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

private fun safeTrackName(name: String): String {
    val base = name.substringBeforeLast(".").filter {
        it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-'
    }.trim().take(75)
    return if (base.isNotBlank()) base else "Offline Song"
}

private fun unusedTrackFile(context: Context, name: String): File {
    val base = safeTrackName(name)
    var output = File(musicFolder(context), base + ".mp3")
    var n = 2
    while (output.exists()) {
        output = File(musicFolder(context), base + " (" + n + ").mp3")
        n++
    }
    return output
}

private fun mp3Signature(file: File): Boolean {
    if (!file.isFile || file.length() < 256) return false
    val h = ByteArray(3)
    file.inputStream().use { if (it.read(h) < 3) return false }
    val id3 = h[0] == 'I'.code.toByte() &&
        h[1] == 'D'.code.toByte() && h[2] == '3'.code.toByte()
    val mpegFrame = (h[0].toInt() and 255) == 255 &&
        ((h[1].toInt() and 224) == 224)
    return id3 || mpegFrame
}

private fun copyUriMp3(context: Context, uri: Uri): File {
    var title = "Imported Song"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) title = it.getString(0) ?: title
    }
    val destination = unusedTrackFile(context, title)
    val temporary = File(musicFolder(context), ".tmp-" + UUID.randomUUID())
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Unable to read selected audio file.")
        input.use { i ->
            FileOutputStream(temporary).use { o ->
                val b = ByteArray(64 * 1024)
                var count = 0L
                while (true) {
                    val n = i.read(b)
                    if (n == -1) break
                    count += n
                    require(count <= MAX_MP3_BYTES) { "Maximum MP3 size is 150 MB." }
                    o.write(b, 0, n)
                }
            }
        }
        require(mp3Signature(temporary)) { "Choose a genuine MP3 audio file." }
        require(temporary.renameTo(destination)) { "Could not save imported MP3." }
        return destination
    } finally {
        temporary.delete()
    }
}

internal fun downloadLicensedMp3(context: Context, link: String, chosenName: String): File {
    val url = URL(link.trim())
    require(url.protocol.equals("https", true)) { "Use a direct HTTPS MP3 download link." }
    val host = url.host.lowercase()
    require(!host.endsWith("youtube.com") && !host.endsWith("youtu.be")) {
        "YouTube links cannot be converted into MP3. Use a licensed direct MP3 link."
    }
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 15000
    connection.readTimeout = 30000
    connection.instanceFollowRedirects = true
    val temp = File(musicFolder(context), ".tmp-" + UUID.randomUUID())
    try {
        require(connection.responseCode in 200..299) {
            "Download failed (HTTP " + connection.responseCode + ")."
        }
        require(connection.url.protocol.equals("https", true)) {
            "Download link redirected to a non-secure URL."
        }
        require(connection.contentLengthLong <= MAX_MP3_BYTES) {
            "Maximum MP3 download size is 150 MB."
        }
        val contentType = (connection.contentType ?: "").lowercase()
        require(!contentType.contains("text/html") && !contentType.contains("application/json")) {
            "This is a website, not a direct MP3 file."
        }
        connection.inputStream.use { input ->
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    total += n
                    require(total <= MAX_MP3_BYTES) { "MP3 exceeded 150 MB limit." }
                    output.write(buffer, 0, n)
                }
            }
        }
        require(mp3Signature(temp)) {
            "The downloaded file is not MP3 audio. Paste a direct .mp3 link."
        }
        val parsedName = runCatching {
            URLDecoder.decode(url.path.substringAfterLast('/'), "UTF-8")
        }.getOrDefault("Downloaded Song")
        val destination = unusedTrackFile(context,
            chosenName.ifBlank { parsedName.ifBlank { "Downloaded Song" } })
        require(temp.renameTo(destination)) { "Could not save the downloaded song." }
        return destination
    } finally {
        connection.disconnect()
        temp.delete()
    }
}

private fun clockTime(millis: Int): String {
    val seconds = (millis.coerceAtLeast(0) / 1000)
    return (seconds / 60).toString() + ":" + (seconds % 60).toString().padStart(2, '0')
}

@Composable
fun OfflineMp3Screen(activity: Activity) {
    val scope = rememberCoroutineScope()
    var songs by remember { mutableStateOf(offlineTracks(activity)) }
    var enteredUrl by remember { mutableStateOf("") }
    var enteredTitle by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("Choose an MP3 from your phone or save an authorized direct MP3 download.") }
    var busy by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var totalMs by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableIntStateOf(0) }

    fun stopTrack() {
        runCatching { player?.release() }
        player = null
        playing = false
        selected = null
        currentMs = 0
        totalMs = 0
    }

    fun playTrack(file: File) {
        if (selected == file.name && player != null) {
            runCatching {
                val mp = player!!
                if (mp.isPlaying) {
                    mp.pause()
                    playing = false
                } else {
                    mp.start()
                    playing = true
                }
            }.onFailure { info = "Playback error: " + (it.message ?: "Unknown error") }
            return
        }
        stopTrack()
        val mp = MediaPlayer()
        try {
            selected = file.name
            player = mp
            mp.setOnPreparedListener {
                totalMs = it.duration.coerceAtLeast(0)
                it.start()
                playing = true
            }
            mp.setOnCompletionListener {
                playing = false
                currentMs = totalMs
            }
            mp.setOnErrorListener { _, _, _ ->
                info = "Could not play this MP3."
                playing = false
                true
            }
            mp.setDataSource(file.absolutePath)
            mp.prepareAsync()
        } catch (t: Exception) {
            stopTrack()
            info = "Unable to play audio: " + (t.message ?: "Unknown error")
        }
    }

    val pickMp3 = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !busy) {
            scope.launch {
                busy = true
                info = "Importing MP3 into your offline library..."
                try {
                    val saved = withContext(Dispatchers.IO) { copyUriMp3(activity, uri) }
                    songs = offlineTracks(activity)
                    info = "Saved for offline playback: " + saved.name
                } catch (e: Exception) {
                    info = "Import failed: " + (e.message ?: "Please select a valid MP3.")
                } finally {
                    busy = false
                }
            }
        }
    }

    LaunchedEffect(player, playing) {
        while (playing) {
            currentMs = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
            delay(400L)
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { player?.release() } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("OFFLINE MP3 PLAYER", fontSize = 23.sp, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary)
            Text("Play downloaded MP3 songs without internet.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Button(
                onClick = { pickMp3.launch(arrayOf("audio/mpeg", "audio/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import MP3 from Phone")
            }
        }
        item {
            OutlinedTextField(
                enteredUrl, { enteredUrl = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Direct MP3 download link (HTTPS)") },
                placeholder = { Text("https://example.com/song.mp3") },
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                enteredTitle, { enteredTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Song title (optional)") },
                singleLine = true
            )
        }
        item {
            Button(
                onClick = {
                    val link = enteredUrl
                    val title = enteredTitle
                    scope.launch {
                        busy = true
                        info = "Downloading authorized MP3..."
                        try {
                            val saved = withContext(Dispatchers.IO) {
                                downloadLicensedMp3(activity, link, title)
                            }
                            songs = offlineTracks(activity)
                            enteredUrl = ""
                            enteredTitle = ""
                            info = "Downloaded for offline playback: " + saved.name
                        } catch (e: Exception) {
                            info = "Download failed: " + (e.message ?: "Unknown error")
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && enteredUrl.startsWith("https://", ignoreCase = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Processing..." else "Download MP3 for Offline")
            }
            Text("Only download MP3s you own or have permission to save. YouTube video links are not MP3 download links.",
                style = MaterialTheme.typography.bodySmall)
            Text(info, color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall)
        }
        if (selected != null) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(selected!!.removeSuffix(".mp3"), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Slider(
                            value = if (totalMs > 0) (currentMs.toFloat()/totalMs).coerceIn(0f,1f) else 0f,
                            onValueChange = { value ->
                                if (totalMs > 0) {
                                    val destination = (value * totalMs).toInt()
                                    runCatching { player?.seekTo(destination) }
                                    currentMs = destination
                                }
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(clockTime(currentMs))
                            Text(clockTime(totalMs))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val i = songs.indexOfFirst { it.name == selected }
                                if (i > 0) playTrack(songs[i-1])
                            }) { Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous") }
                            FilledIconButton(onClick = {
                                songs.firstOrNull { it.name == selected }?.let { playTrack(it) }
                            }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play") }
                            IconButton(onClick = {
                                val i = songs.indexOfFirst { it.name == selected }
                                if (i >= 0 && i + 1 < songs.size) playTrack(songs[i+1])
                            }) { Icon(Icons.Rounded.SkipNext, contentDescription = "Next") }
                            IconButton(onClick = { stopTrack() }) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop")
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Saved MP3 songs (" + songs.size + ")", fontSize = 19.sp,
                fontWeight = FontWeight.Bold)
            if (songs.isEmpty()) {
                Text("Your offline library is empty. Import an MP3 or save a licensed direct MP3 link above.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        items(songs, key = { it.name }) { song ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().clickable { playTrack(song) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.name.removeSuffix(".mp3"), fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(((song.length() / 1024) / 1024f).toString().take(4) + " MB • Offline",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { playTrack(song) }) {
                        Icon(if (selected == song.name && playing) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or pause MP3")
                    }
                    IconButton(onClick = {
                        if (selected == song.name) stopTrack()
                        if (song.delete()) {
                            songs = offlineTracks(activity)
                            info = "Removed " + song.name
                        } else {
                            info = "Could not delete " + song.name
                        }
                    }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete MP3")
                    }
                }
            }
        }
    }
}
