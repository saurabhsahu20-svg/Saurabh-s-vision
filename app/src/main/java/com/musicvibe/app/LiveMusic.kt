package com.musicvibe.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiveMusicScreen(activity:Activity){
  val prefs=remember{activity.getSharedPreferences("saune_real_music",0)}
  val scope=rememberCoroutineScope()
  var provider by remember{mutableStateOf("Audius")}
  var query by remember{mutableStateOf("")}
  var submitted by remember{mutableStateOf("")}
  var mode by remember{mutableStateOf("Trending")}
  var jamendoInput by remember{mutableStateOf(prefs.getString("jamendo_id","").orEmpty())}
  var jamendoId by remember{mutableStateOf(jamendoInput)}
  var tracks by remember{mutableStateOf<List<OpenSong>>(emptyList())}
  var loading by remember{mutableStateOf(false)}
  var downloading by remember{mutableStateOf(false)}
  var message by remember{mutableStateOf("Loading actual tracks from Audius...")}
  var controller by remember{mutableStateOf<MediaController?>(null)}
  var nowPlaying by remember{mutableStateOf("")}
  var isPlaying by remember{mutableStateOf(false)}
  var position by remember{mutableLongStateOf(0)}
  var duration by remember{mutableLongStateOf(0)}
  var repeat by remember{mutableIntStateOf(Player.REPEAT_MODE_OFF)}
  var shuffled by remember{mutableStateOf(false)}
  var favorites by remember{mutableStateOf(
    prefs.getString("favorites","").orEmpty().split("|").filter{it.isNotBlank()}.toSet()
  )}
  var showInfo by remember{mutableStateOf(false)}

  fun openSource(url:String){
    if(url.startsWith("https://"))activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))
  }

  fun play(track:OpenSong){
    val media=controller
    if(media==null){message="Connecting audio service. Please try again.";return}
    try{
      val item=MediaItem.Builder().setMediaId(track.key).setUri(track.stream)
        .setMediaMetadata(MediaMetadata.Builder()
          .setTitle(track.title).setArtist(track.artist)
          .apply{if(track.image.startsWith("https://"))setArtworkUri(Uri.parse(track.image))}
          .build()).build()
      val queue=tracks.map { song ->
        MediaItem.Builder().setMediaId(song.key).setUri(song.stream)
          .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(song.title).setArtist(song.artist)
            .apply{if(song.image.startsWith("https://"))setArtworkUri(Uri.parse(song.image))}
            .build()).build()
      }
      val idx=tracks.indexOfFirst{it.key==track.key}
      if(queue.isNotEmpty() && idx>=0)media.setMediaItems(queue,idx,0L)
      else media.setMediaItem(item)
      media.prepare()
      media.play()
      nowPlaying=track.title
      message="Streaming from "+track.source+"; playback depends on provider availability."
    }catch(e:Exception){message="Unable to play: "+e.message}
  }

  DisposableEffect(Unit){
    val token=SessionToken(activity,ComponentName(activity,SaunePlaybackService::class.java))
    val future=MediaController.Builder(activity,token).buildAsync()
    var connected:MediaController?=null
    val listener=object:Player.Listener{
      override fun onIsPlayingChanged(value:Boolean){isPlaying=value}
      override fun onMediaItemTransition(item:MediaItem?,reason:Int){
        nowPlaying=item?.mediaMetadata?.title?.toString().orEmpty()
      }
      override fun onPlayerError(error:PlaybackException){
        message="Track unavailable: "+error.errorCodeName
        isPlaying=false
      }
    }
    future.addListener({
      runCatching{
        val c=future.get()
        connected=c
        c.addListener(listener)
        controller=c
        isPlaying=c.isPlaying
        nowPlaying=c.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
      }.onFailure{message="Media player connection failed: "+it.message}
    },ContextCompat.getMainExecutor(activity))
    onDispose{
      connected?.removeListener(listener)
      controller=null
      MediaController.releaseFuture(future)
    }
  }

  LaunchedEffect(provider,submitted,mode,jamendoId){
    loading=true
    message="Fetching actual "+provider+" recordings..."
    val result=OpenCatalog.browse(provider,submitted,jamendoId,mode)
    result.onSuccess{
      tracks=it
      message=if(it.isEmpty())"No available recordings found. Try a new search or provider."
        else "Showing "+it.size+" real tracks from "+provider+"."
    }.onFailure{
      tracks=emptyList()
      message="Provider error: "+(it.message?:"Network not available")
    }
    loading=false
  }
  LaunchedEffect(controller){
    while(controller!=null){
      val c=controller
      position=(c?.currentPosition?:0L).coerceAtLeast(0)
      duration=(c?.duration?:0L).coerceAtLeast(0)
      delay(600)
    }
  }
  LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(vertical=12.dp),
    verticalArrangement=Arrangement.spacedBy(12.dp)){
    item{
      Text("SAUNE VIBE • REAL MUSIC",fontSize=23.sp,fontWeight=FontWeight.Black,
        color=MaterialTheme.colorScheme.primary)
      Text("Feel the Music, Live the Vibe!",style=MaterialTheme.typography.bodyMedium)
    }
    item{
      Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
        listOf("Audius","Jamendo","Wikimedia").forEach{p->
          FilterChip(selected=provider==p,onClick={provider=p;query="";submitted=""},
            label={Text(p)})
        }
      }
    }
    item{
      OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),
        label={Text("Song, artist, genre or musical era")},singleLine=true)
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Button(onClick={submitted=query.trim()},enabled=!loading){
          Icon(Icons.Rounded.Search,contentDescription=null)
          Text(" Search")
        }
        OutlinedButton(onClick={submitted="";query="";mode="Trending"}){Text("Trending")}
        OutlinedButton(onClick={submitted="";query="";mode="Latest"}){Text("Latest")}
      }
    }
    if(provider=="Jamendo")item{
      OutlinedTextField(jamendoInput,{jamendoInput=it.trim()},
        label={Text("Jamendo client ID")},singleLine=true,
        modifier=Modifier.fillMaxWidth())
      TextButton(onClick={
        jamendoId=jamendoInput
        prefs.edit().putString("jamendo_id",jamendoId).apply()
      }){Text("Use client ID and load Jamendo")}
      TextButton(onClick={openSource("https://developer.jamendo.com")}){
        Text("Get a developer client ID")
      }
    }
    item{
      Row(verticalAlignment=Alignment.CenterVertically){
        Text(provider+" music",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
        TextButton(onClick={showInfo=!showInfo}){Text("Credits & licenses")}
      }
      if(loading)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
      Text(message,style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.primary)
      if(showInfo)Text(
        "Each recording comes from its provider. Streaming is not permission to copy or redistribute. "+
          "Downloads appear only if provider rights data permits them. Consult the original source "+
          "and license before reuse. Jamendo requires your own client ID.",
        style=MaterialTheme.typography.bodySmall)
    }
    if(nowPlaying.isNotBlank())item{
      Surface(color=MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(16.dp)){
        Column(Modifier.fillMaxWidth().padding(14.dp)){
          Text("NOW PLAYING",color=MaterialTheme.colorScheme.primary,
            fontWeight=FontWeight.Bold)
          Text(nowPlaying,maxLines=2,overflow=TextOverflow.Ellipsis)
          Slider(value=if(duration>0)(position.toFloat()/duration).coerceIn(0f,1f) else 0f,
            onValueChange={v->if(duration>0)controller?.seekTo((duration*v).toLong())})
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text((position/60000).toString()+":"+
              ((position/1000)%60).toString().padStart(2,'0'))
            Text((duration/60000).toString()+":"+
              ((duration/1000)%60).toString().padStart(2,'0'))
          }
          Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
            IconButton(onClick={controller?.seekToPreviousMediaItem()}){
              Icon(Icons.Rounded.SkipPrevious,contentDescription="Previous")
            }
            FilledIconButton(onClick={
              if(controller?.isPlaying==true)controller?.pause() else controller?.play()
            }){
              Icon(if(isPlaying)Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription="Play/pause")
            }
            IconButton(onClick={controller?.seekToNextMediaItem()}){
              Icon(Icons.Rounded.SkipNext,contentDescription="Next")
            }
            IconButton(onClick={
              repeat=when(repeat){
                Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE->Player.REPEAT_MODE_ALL
                else->Player.REPEAT_MODE_OFF
              }
              controller?.repeatMode=repeat
            }){Icon(Icons.Rounded.Repeat,contentDescription="Repeat")}
            IconButton(onClick={shuffled=!shuffled;controller?.shuffleModeEnabled=shuffled}){
              Icon(Icons.Rounded.Shuffle,contentDescription="Shuffle")
            }
          }
        }
      }
    }
    items(tracks,key={it.key}){song->
      Surface(color=MaterialTheme.colorScheme.surface,
        shape=RoundedCornerShape(14.dp)){
        Column(Modifier.fillMaxWidth().padding(8.dp)){
          Row(verticalAlignment=Alignment.CenterVertically){
            if(song.image.isNotBlank())AsyncImage(model=song.image,
              contentDescription="Album art",modifier=Modifier.size(58.dp))
            else Icon(Icons.Rounded.MusicNote,contentDescription=null,
              modifier=Modifier.size(58.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable{play(song)}){
              Text(song.title,fontWeight=FontWeight.Bold,maxLines=2,
                overflow=TextOverflow.Ellipsis)
              Text(song.artist,maxLines=1,overflow=TextOverflow.Ellipsis,
                style=MaterialTheme.typography.bodySmall)
              Text(song.source,style=MaterialTheme.typography.labelSmall,
                color=MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick={
              favorites=if(song.key in favorites)favorites-song.key else favorites+song.key
              prefs.edit().putString("favorites",favorites.joinToString("|")).apply()
            }){
              Icon(if(song.key in favorites)Icons.Rounded.Favorite
                else Icons.Rounded.FavoriteBorder,contentDescription="Favorite")
            }
            IconButton(onClick={play(song)}){
              Icon(Icons.Rounded.PlayArrow,contentDescription="Play audio")
            }
          }
          Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={openSource(song.sourceUrl)}){Text("Source")}
            if(song.licenseUrl.startsWith("https://"))
              TextButton(onClick={openSource(song.licenseUrl)}){Text("License")}
            if(song.download!=null)
              TextButton(enabled=!downloading,onClick={
                scope.launch{
                  downloading=true
                  message="Saving authorized audio..."
                  try{
                    val file=withContext(Dispatchers.IO){
                      downloadLicensedMp3(activity,song.download,song.title)
                    }
                    message="Saved "+file.name+". Open Offline tab to play."
                  }catch(e:Exception){
                    message="MP3 download failed: "+e.message
                  }finally{downloading=false}
                }
              }){
                Icon(Icons.Rounded.Download,contentDescription=null)
                Text("Download")
              }
          }
        }
      }
    }
  }
}
