package com.musicvibe.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Video(val id:String, val title:String, val artist:String, val image:String)
data class Playlist(val name:String, val ids:List<String>)
private val accent=Color(0xFFAF70FC)
private val pink=Color(0xFFFF68C9)
private val dark=Color(0xFF0E0819)
private val backgrounds=listOf(Color(0xFF3A1C70),Color(0xFFB335A4))
private val genres=listOf("Hindi","English","Punjabi","Bhojpuri","Romantic","Sad","Party","Devotional")
private fun queryFor(g:String)=when(g) {
  "Hindi"->"latest hindi songs official music video"
  "English"->"english pop songs official video"
  "Punjabi"->"punjabi songs official music video"
  "Bhojpuri"->"bhojpuri songs official music video"
  "Romantic"->"romantic love songs official music video"
  "Sad"->"sad emotional songs official music video"
  "Party"->"party dance songs official music video"
  "Devotional"->"bhakti devotional songs official music video"
  else->"$g official music video"
}

/* Official public YouTube video IDs: only metadata is bundled, never copied audio/video. */
private data class CatalogTrack(val id:String,val title:String,val artist:String,val tags:Set<String>){
 fun asVideo()=Video(id,title,artist,"https://i.ytimg.com/vi/$id/hqdefault.jpg")
}
private val starterCatalog=listOf(
 CatalogTrack("BddP6PYo2gs","Kesariya","Arijit Singh · Sony Music India",setOf("Hindi","Romantic")),
 CatalogTrack("ElZfdU54Cp8","Apna Bana Le","Arijit Singh · Zee Music Company",setOf("Hindi","Romantic")),
 CatalogTrack("AFTIVN8rRbI","Sajni","Arijit Singh · T-Series",setOf("Hindi","Sad","Romantic")),
 CatalogTrack("wr9M-CoxP7A","Chaleya","Arijit Singh & Shilpa Rao · T-Series",setOf("Hindi","Romantic")),
 CatalogTrack("6SGRn9OHtFY","Agar Tum Saath Ho","Alka Yagnik & Arijit Singh · T-Series",setOf("Hindi","Sad","Romantic")),
 CatalogTrack("LK7-_dgAVQE","Tauba Tauba","Karan Aujla · Saregama Music",setOf("Hindi","Punjabi","Party")),
 CatalogTrack("JGwWNGJdvx8","Shape of You","Ed Sheeran",setOf("English","Party")),
 CatalogTrack("2Vv-BfVoq4g","Perfect","Ed Sheeran",setOf("English","Romantic")),
 CatalogTrack("4NRXx6U8ABQ","Blinding Lights","The Weeknd",setOf("English","Party")),
 CatalogTrack("N000qglmmY0","Levitating (Animated)","Dua Lipa",setOf("English","Party")),
 CatalogTrack("gdZLi9oWNZg","Dynamite","BTS · HYBE LABELS",setOf("English","Party")),
 CatalogTrack("dCmp56tSSmA","Born to Shine","Diljit Dosanjh",setOf("Punjabi","Party")),
 CatalogTrack("4tywp83zkmk","Cheques","Shubh",setOf("Punjabi","Party")),
 CatalogTrack("0hGGaVCCqPk","Hello Kaun","Ritesh Pandey & Sneh Upadhya",setOf("Bhojpuri","Party")),
 CatalogTrack("o1mM3qsYO14","Bhojpuri Dance Hits (Jukebox)","Khesari Lal Yadav · Saregama Hum Bhojpuri",setOf("Bhojpuri","Party")),
 CatalogTrack("7KEF-Qpw7Ww","Chhath Puja Special Jukebox","Sharda Sinha & Anuradha Paudwal",setOf("Bhojpuri","Devotional")),
 CatalogTrack("cC4ORxmpUDQ","Chhath Geet Collection","Sharda Sinha · Tips Bhojpuri",setOf("Bhojpuri","Devotional")),
 CatalogTrack("HSmjdnH8gQs","Shree Hanuman Chalisa","Hariharan · T-Series Bhakti Sagar",setOf("Devotional")),
 CatalogTrack("txXXRMfp12w","Ram Siya Ram","Sachet Tandon · T-Series",setOf("Devotional")),
 CatalogTrack("ZIKzQJYKJV0","Shiv Tandav Stotram","Shankar Mahadevan · Times Music Spiritual",setOf("Devotional"))
)
private fun includedVideos(search:String):List<Video>{
 val raw=search.trim()
 if(Regex("^[A-Za-z0-9_-]{11}$").matches(raw)){
  val known=starterCatalog.firstOrNull{it.id==raw}
  return listOf(known?.asVideo() ?: Video(raw,"Featured YouTube video","YouTube","https://i.ytimg.com/vi/$raw/hqdefault.jpg"))
 }
 val q=raw.lowercase()
 val category=listOf("hindi","english","punjabi","bhojpuri","romantic","sad","party","devotional")
  .firstOrNull{q.contains(it)}
 val tracks=when{
  q in listOf("trending","latest","featured","") -> starterCatalog
  category!=null -> starterCatalog.filter{entry->entry.tags.any{it.equals(category,true)}}
  else -> starterCatalog.filter{entry->
   entry.title.lowercase().contains(q)||entry.artist.lowercase().contains(q)||
    entry.tags.any{it.lowercase().contains(q)}
  }
 }
 return tracks.map{it.asVideo()}
}
private fun youtubeIdFromLink(s:String):String? =
 Regex("(?:[?&]v=|youtu\\.be/|/shorts/|/embed/)([A-Za-z0-9_-]{11})").find(s)?.groupValues?.getOrNull(1)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  setContent { MusicVibe(this) }
 }
}
private fun parseVideos(body:String):List<Video>{
 val result=mutableListOf<Video>()
 val arr=JSONObject(body).optJSONArray("items")?:return result
 for(i in 0 until arr.length()){
  val item=arr.optJSONObject(i)?:continue
  val idNode=item.opt("id")
  val id=if(idNode is JSONObject) idNode.optString("videoId") else item.optString("id")
  if(!Regex("^[a-zA-Z0-9_-]{11}$").matches(id))continue
  val sn=item.optJSONObject("snippet")?:continue
  val thumbs=sn.optJSONObject("thumbnails")
  val image=thumbs?.optJSONObject("high")?.optString("url")?:""
  result.add(Video(id,sn.optString("title","Music video"),sn.optString("channelTitle","YouTube"),image))
 }
 return result
}
private suspend fun youtube(apiKey:String,query:String,trending:Boolean=false,latest:Boolean=false): Result<List<Video>> =
 withContext(Dispatchers.IO){
  runCatching{
   require(apiKey.isNotBlank()){"Enter a YouTube Data API v3 key under Settings."}
   val params=if(trending)
     "part=snippet&chart=mostPopular&videoCategoryId=10&regionCode=IN&maxResults=20"
   else "part=snippet&type=video&videoEmbeddable=true&maxResults=25&safeSearch=moderate&order="+
      (if(latest)"date" else "relevance")+"&q="+URLEncoder.encode(query,"UTF-8")
   val url=URL("https://www.googleapis.com/youtube/v3/"+(if(trending)"videos" else "search")+"?"+params+"&key="+URLEncoder.encode(apiKey,"UTF-8"))
   val conn=url.openConnection() as HttpURLConnection
   try{
    conn.connectTimeout=12000;conn.readTimeout=12000
    val code=conn.responseCode
    val body=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
    if(code !in 200..299)error(JSONObject(body).optJSONObject("error")?.optString("message")?:"YouTube error $code")
    parseVideos(body)
   }finally{conn.disconnect()}
  }
 }
@Composable
fun MusicVibe(activity:Activity){
 val pref=remember{activity.getSharedPreferences("musicvibe",0)}
 var key by remember{mutableStateOf(pref.getString("key","")?:"")}
 var darkMode by remember{mutableStateOf(pref.getBoolean("dark",true))}
 var tab by remember{mutableStateOf(0)}
 var query by remember{mutableStateOf("")}
 var request by remember{mutableStateOf("trending")}
 var section by remember{mutableStateOf("Included YouTube songs")}
 var current by remember{mutableStateOf<Video?>(null)}
 var loading by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf("")}
 var videos by remember{mutableStateOf(starterCatalog.map{it.asVideo()})}
 var recent by remember{mutableStateOf(pref.getString("recent","")!!.split("|").filter{it.isNotBlank()})}
 var favorite by remember{mutableStateOf(pref.getString("favourites","")!!.split("|").filter{it.isNotBlank()})}
 var cached by remember{mutableStateOf(starterCatalog.associate{it.id to it.asVideo()})}
 var playlists by remember{
  mutableStateOf(runCatching{
   val a=JSONArray(pref.getString("playlists","[]"))
   List(a.length()){i->
    val p=a.getJSONObject(i);val arr=p.optJSONArray("ids")?:JSONArray()
    Playlist(p.getString("name"),List(arr.length()){j->arr.getString(j)})
   }
  }.getOrDefault(emptyList()))
 }
 var pickedPlaylist by remember{mutableStateOf("")}
 var playlistName by remember{mutableStateOf("")}
 var banner by remember{mutableStateOf(pref.getString("banner","Your sound. Your vibe.")?:"")}
 var customGenres by remember{mutableStateOf(pref.getString("genres","")!!.split("|").filter{it.isNotBlank()})}
 var newGenre by remember{mutableStateOf("")}
 var featuredInput by remember{mutableStateOf("")}
 var featured by remember{mutableStateOf(pref.getString("featured","")!!.split("|").filter{it.isNotBlank()})}
 var plays by remember{mutableIntStateOf(pref.getInt("plays",0))}
 var searches by remember{mutableIntStateOf(pref.getInt("searches",0))}
 var showAdmin by remember{mutableStateOf(false)}
 var showPlaylistDialog by remember{mutableStateOf<Video?>(null)}
 val colors=if(darkMode) darkColorScheme(primary=accent,secondary=pink,background=dark,surface=Color(0xFF21152F))
  else lightColorScheme(primary=Color(0xFF6B31C2),secondary=Color(0xFFB5289A),background=Color(0xFFF8F4FF))
 fun savePlaylists(updated:List<Playlist>){
  playlists=updated
  val a=JSONArray()
  updated.forEach{p->a.put(JSONObject().put("name",p.name).put("ids",JSONArray(p.ids)))}
  pref.edit().putString("playlists",a.toString()).apply()
 }
 fun open(v:Video){
  current=v
  cached=cached+(v.id to v)
  recent=(listOf(v.id)+recent.filter{it!=v.id}).take(30)
  pref.edit().putString("recent",recent.joinToString("|")).apply()
  plays++;pref.edit().putInt("plays",plays).apply()
 }
 fun saveFavorite(v:Video){
  cached=cached+(v.id to v)
  favorite=if(v.id in favorite) favorite-v.id else listOf(v.id)+favorite
  pref.edit().putString("favourites",favorite.joinToString("|")).apply()
 }
 fun go(q:String,title:String){
  request=q;section=title;tab=1
 }
 BackHandler(enabled=current!=null||showAdmin){if(current!=null)current=null else showAdmin=false}
 LaunchedEffect(request,key){
  val builtIn=includedVideos(request)
  videos=builtIn
  cached=cached+builtIn.associateBy{it.id}
  error=""
  if(key.isBlank())return@LaunchedEffect
  loading=true
  val trend=request=="trending"
  val latest=request=="latest"
  val result=youtube(key,if(latest)"new official music video" else request,trend,latest)
  result.onSuccess{online->
   videos=if(online.isNotEmpty())online else builtIn
   cached=cached+videos.associateBy{it.id}
  }.onFailure{error="Live YouTube search unavailable; showing included music.";videos=builtIn}
  loading=false
 }
 MaterialTheme(colorScheme=colors) {
  Scaffold(containerColor=MaterialTheme.colorScheme.background,
   bottomBar={
    if(current==null&&!showAdmin){
     NavigationBar {
      listOf("Home","Discover","Library","Offline","Settings").forEachIndexed{idx,t->
       NavigationBarItem(tab==idx,{tab=idx},
        icon={Icon(when(idx){0->Icons.Rounded.Home;1->Icons.Rounded.Search;2->Icons.Rounded.LibraryMusic;3->Icons.Rounded.Headphones;else->Icons.Rounded.Settings},contentDescription=t)},
        label={Text(t)})
      }
     }
    }
   }
  ){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp)){
   if(current!=null){
    val v=current!!
    Row(verticalAlignment=Alignment.CenterVertically){
     IconButton(onClick={current=null}){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}
     Text("NOW PLAYING • YouTube",fontWeight=FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
    androidx.compose.runtime.key(v.id){
     val html="<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"+
      "<body style='margin:0;background:black'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/${v.id}?playsinline=1'"+
      " frameborder='0' allow='encrypted-media;picture-in-picture' allowfullscreen></iframe></body></html>"
     AndroidView(factory={context->WebView(context).apply{
      settings.javaScriptEnabled=true;settings.domStorageEnabled=true
      webChromeClient=WebChromeClient()
      loadDataWithBaseURL("https://appassets.androidplatform.net/",html,"text/html","UTF-8",null)
     }},modifier=Modifier.fillMaxWidth().height(245.dp))
    }
    Spacer(Modifier.height(16.dp))
    Text(v.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Text(v.artist,color=MaterialTheme.colorScheme.primary)
    Row{
     TextButton(onClick={saveFavorite(v)}){Icon(if(v.id in favorite)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,contentDescription=null);Text(" Favourite")}
     TextButton(onClick={showPlaylistDialog=v}){Icon(Icons.Rounded.PlaylistAdd,contentDescription=null);Text(" Playlist")}
    }
    TextButton(onClick={
     activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=${v.id}")))
    }){Icon(Icons.Rounded.OpenInNew,contentDescription=null);Text(" Play in YouTube")}
    Text("Music streams from YouTube using its official player. Playback availability depends on YouTube.",style=MaterialTheme.typography.bodySmall)
   }else if(showAdmin){
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=15.dp)){
     item{Row(verticalAlignment=Alignment.CenterVertically){
      IconButton(onClick={showAdmin=false}){Icon(Icons.Rounded.ArrowBack,contentDescription="Back")}
      Text("LOCAL ADMIN",fontWeight=FontWeight.Bold,fontSize=22.sp)
     }}
     item{Text("Device statistics • $plays plays • $searches searches • ${favorite.size} favourites")}
     item{OutlinedTextField(banner,{banner=it.take(100)},label={Text("Home banner")},modifier=Modifier.fillMaxWidth())}
     item{Button(onClick={pref.edit().putString("banner",banner).apply()}){Text("Save banner")}}
     item{OutlinedTextField(newGenre,{newGenre=it},label={Text("New music category")},modifier=Modifier.fillMaxWidth())}
     item{Button(onClick={
      val g=newGenre.trim()
      if(g.isNotBlank()&&g !in genres+customGenres){
       customGenres=customGenres+g
       pref.edit().putString("genres",customGenres.joinToString("|")).apply()
       newGenre=""
      }
     }){Text("Add category")}}
     items(customGenres){g->TextButton(onClick={
      customGenres=customGenres-g;pref.edit().putString("genres",customGenres.joinToString("|")).apply()
     }){Text("Remove $g")}}
     item{OutlinedTextField(featuredInput,{featuredInput=it},label={Text("Featured YouTube video ID or link")},modifier=Modifier.fillMaxWidth())}
     item{Button(onClick={
      val match=Regex("(?:v=|youtu\\.be/|shorts/|embed/)?([A-Za-z0-9_-]{11})").find(featuredInput)
      val id=match?.groupValues?.getOrNull(1)
      if(id!=null&&id !in featured){featured=featured+id;pref.edit().putString("featured",featured.joinToString("|")).apply();featuredInput=""}
     }){Text("Add featured video")}}
     items(featured){id->TextButton(onClick={featured=featured-id;pref.edit().putString("featured",featured.joinToString("|")).apply()}){Text("Remove featured $id")}}
     item{Text("Registered users: Not available. This local-only app does not create accounts.",style=MaterialTheme.typography.bodySmall)}
    }
   }else when(tab){
    0->LazyColumn(verticalArrangement=Arrangement.spacedBy(17.dp),contentPadding=PaddingValues(vertical=15.dp)){
     item{Brand("Discover your next favourite track")}
     item{Box(Modifier.fillMaxWidth().height(148.dp).background(Brush.horizontalGradient(backgrounds),RoundedCornerShape(20.dp)).padding(20.dp),contentAlignment=Alignment.CenterStart){
      Column{Text("SAUNE VIBE",color=Color.White,fontWeight=FontWeight.Black,fontSize=25.sp);Text(banner,color=Color.White,fontSize=17.sp)}
     }}
     item{Text("20 songs ready to explore • no API key required",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.primary)}
     item{OutlinedButton(onClick={tab=3},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Headphones,contentDescription=null);Text(" Offline MP3 Player & Downloads")}}
     item{Text("Find your vibe",fontWeight=FontWeight.Bold,fontSize=19.sp)}
     items((genres+customGenres).chunked(2)){row->
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       row.forEach{g->Surface(modifier=Modifier.weight(1f).height(78.dp).clickable{go(queryFor(g),g)},shape=RoundedCornerShape(15.dp),color=MaterialTheme.colorScheme.surface){
        Box(contentAlignment=Alignment.Center){Text(g,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
       }}
       if(row.size<2) Spacer(Modifier.weight(1f))
      }
     }
     item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
      Button(onClick={go("trending",if(key.isBlank())"All songs" else "Daily trending")}){Text(if(key.isBlank())"All songs" else "Trending")}
      OutlinedButton(onClick={go("latest",if(key.isBlank())"Music picks" else "Latest releases")}){Text(if(key.isBlank())"Music picks" else "New releases")}
     }}
     item{Text("Music for you • $section",fontWeight=FontWeight.Bold,fontSize=19.sp)}
     if(loading)item{CircularProgressIndicator()}
     if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
     items(videos.take(12),key={it.id}){v->SongCard(v,v.id in favorite,{open(v)},{saveFavorite(v)},{showPlaylistDialog=v})}
     if(featured.isNotEmpty())item{Text("Featured video IDs",fontWeight=FontWeight.Bold)}
     items(featured){id->TextButton(onClick={go(id,"Featured music")}){Text("Featured: $id")}}
    }
    1->Column{
     Spacer(Modifier.height(15.dp))
     Brand("Search the music you love")
     Spacer(Modifier.height(12.dp))
     OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Search artist/song or paste a YouTube link")},singleLine=true,
      trailingIcon={IconButton(onClick={
       if(query.isNotBlank()){
        val id=youtubeIdFromLink(query)
        searches++;pref.edit().putInt("searches",searches).apply()
        if(id!=null)open(Video(id,"YouTube song","Added from YouTube link","https://i.ytimg.com/vi/$id/hqdefault.jpg"))
        else go(query,"Search results")
       }
      }){
       Icon(Icons.Rounded.Search,contentDescription="Search")
      }})
     Spacer(Modifier.height(11.dp))
     Text(section,fontWeight=FontWeight.Bold,fontSize=18.sp)
     if(loading) CircularProgressIndicator()
     if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
     if(key.isBlank()){
      Text("Browse the included songs or paste any YouTube video link above. Add an optional API key under Settings for wider search.",style=MaterialTheme.typography.bodySmall)
      TextButton(onClick={
       activity.startActivity(Intent(Intent.ACTION_VIEW,
        Uri.parse("https://www.youtube.com/results?search_query="+URLEncoder.encode(query.ifBlank{section},"UTF-8"))))
      }){Text("Search all YouTube songs")}
     }
     LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
      items(videos,key={it.id}){v->SongCard(v,v.id in favorite,{open(v)},{saveFavorite(v)},{showPlaylistDialog=v})}
     }
    }
    2->LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)){
     item{Brand("Your favourites and playlists")}
     item{Text("Favourites",fontSize=20.sp,fontWeight=FontWeight.Bold)}
     items(favorite){id->cached[id]?.let{v->SongCard(v,true,{open(v)},{saveFavorite(v)},{showPlaylistDialog=v})}}
     item{Text("Recently played",fontSize=20.sp,fontWeight=FontWeight.Bold)}
     items(recent){id->cached[id]?.let{v->SongCard(v,id in favorite,{open(v)},{saveFavorite(v)},{showPlaylistDialog=v})}}
     item{Text("Your playlists",fontSize=20.sp,fontWeight=FontWeight.Bold)}
     item{Row(verticalAlignment=Alignment.CenterVertically){
      OutlinedTextField(playlistName,{playlistName=it},modifier=Modifier.weight(1f),label={Text("Playlist name")})
      Button(onClick={val n=playlistName.trim();if(n.isNotEmpty()&&playlists.none{it.name==n}){savePlaylists(playlists+Playlist(n,emptyList()));playlistName=""}}){Text("Add")}
     }}
     items(playlists){p->
      Column{
       TextButton(onClick={pickedPlaylist=if(pickedPlaylist==p.name)"" else p.name}){Text("${p.name} (${p.ids.size})")}
       if(pickedPlaylist==p.name){
        p.ids.forEach{id->cached[id]?.let{v->SongCard(v,id in favorite,{open(v)},{saveFavorite(v)},{showPlaylistDialog=v})}}
        TextButton(onClick={savePlaylists(playlists.filter{it.name!=p.name});pickedPlaylist=""}){Text("Delete playlist")}
       }
      }
     }
     item{Text("Videos from previous sessions may need YouTube search to reload their details.",style=MaterialTheme.typography.bodySmall)}
    }
    3->OfflineMp3Screen(activity)
    else->LazyColumn(verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(vertical=16.dp)){
     item{Brand("Make SAUNE VIBE yours")}
     item{Text("YouTube search (optional)",fontWeight=FontWeight.Bold,fontSize=19.sp)}
     item{OutlinedTextField(key,{key=it.trim();pref.edit().putString("key",key).apply()},modifier=Modifier.fillMaxWidth(),label={Text("YouTube Data API v3 key")},singleLine=true)}
     item{Text("20 included official YouTube links work without an API key. Add a YouTube Data API v3 key only for live search/trending. Key is stored on this device; a backend is needed for production.",style=MaterialTheme.typography.bodySmall)}
     item{Row(verticalAlignment=Alignment.CenterVertically){
      Text("Dark mode",modifier=Modifier.weight(1f))
      Switch(darkMode,{darkMode=it;pref.edit().putBoolean("dark",it).apply()})
     }}
     item{Button(onClick={showAdmin=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.Settings,contentDescription=null);Text(" Local admin dashboard")}}
     item{Text("Music via official YouTube playback. No MP3 downloads or background-only streams.",style=MaterialTheme.typography.bodySmall)}
     item{Text("SAUNE VIBE • v1.0.4 • Android 7+",style=MaterialTheme.typography.bodySmall)}
    }
   }
  }
  }
  if(showPlaylistDialog!=null){
   val song=showPlaylistDialog!!
   AlertDialog(onDismissRequest={showPlaylistDialog=null},title={Text("Add to playlist")},
    text={Column{
     if(playlists.isEmpty())Text("Create a playlist in Library first.")
     playlists.forEach{p->TextButton(onClick={
      cached=cached+(song.id to song)
      savePlaylists(playlists.map{if(it.name==p.name&&song.id !in it.ids)it.copy(ids=it.ids+song.id) else it})
      showPlaylistDialog=null
     }){Text(p.name)}}
    }},confirmButton={TextButton(onClick={showPlaylistDialog=null}){Text("Close")}})
  }
 }
}
@Composable private fun Brand(subtitle:String){
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Image(painter=painterResource(id=R.drawable.saune_vibe_icon),contentDescription="Saune Vibe logo",modifier=Modifier.size(64.dp))
  Spacer(Modifier.width(12.dp))
  Column {
   Text("SAUNE VIBE",fontWeight=FontWeight.Black,fontSize=25.sp,color=MaterialTheme.colorScheme.primary)
   Text(subtitle,style=MaterialTheme.typography.bodyMedium)
  }
 }
}
@Composable private fun SongCard(v:Video,isFav:Boolean,onPlay:()->Unit,onFav:()->Unit,onPlaylist:()->Unit){
 Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(15.dp)){
  Row(Modifier.fillMaxWidth().clickable(onClick=onPlay).padding(8.dp),verticalAlignment=Alignment.CenterVertically){
   AsyncImage(model=v.image,contentDescription="YouTube thumbnail",modifier=Modifier.size(66.dp))
   Spacer(Modifier.width(9.dp))
   Column(Modifier.weight(1f)){
    Text(v.title,maxLines=2,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)
    Text(v.artist,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
   }
   IconButton(onClick=onFav){Icon(if(isFav)Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,contentDescription="Favourite",tint=if(isFav)pink else MaterialTheme.colorScheme.primary)}
   IconButton(onClick=onPlaylist){Icon(Icons.Rounded.PlaylistAdd,contentDescription="Add to playlist")}
  }
 }
}
