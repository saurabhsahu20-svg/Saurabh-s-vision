package com.musicvibe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class OpenSong(
  val source:String, val id:String, val title:String, val artist:String,
  val image:String, val stream:String, val download:String?,
  val license:String, val licenseUrl:String, val sourceUrl:String
) {
  val key:String get()=source+":"+id
}

private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
private fun fetchJson(url:String):JSONObject{
  require(url.startsWith("https://"))
  val c=URL(url).openConnection() as HttpURLConnection
  c.connectTimeout=12000
  c.readTimeout=16000
  c.setRequestProperty("User-Agent","SauneVibe/1.1 (Android music app)")
  try{
    val status=c.responseCode
    val body=(if(status in 200..299)c.inputStream else c.errorStream)
      ?.bufferedReader()?.use{it.readText()}.orEmpty()
    require(status in 200..299){"Provider HTTP "+status}
    return JSONObject(body)
  }finally{c.disconnect()}
}

object OpenCatalog {
  suspend fun browse(source:String,search:String,clientId:String,mode:String):Result<List<OpenSong>> =
    withContext(Dispatchers.IO){
      runCatching {
        when(source){
          "Audius"->audius(search,mode)
          "Jamendo"->jamendo(search,clientId,mode)
          "Wikimedia"->commons(search)
          else->emptyList()
        }.distinctBy{it.key}
      }
    }

  private fun audius(search:String,mode:String):List<OpenSong>{
    val route=when{
      search.isNotBlank()->"tracks/search?query="+enc(search)+"&limit=30"
      mode=="Latest"->"tracks/latest?limit=30"
      else->"tracks/trending?limit=30"
    }
    val arr=fetchJson("https://api.audius.co/v1/"+route+"&app_name=SauneVibe")
      .optJSONArray("data")?:return emptyList()
    return buildList{
      for(i in 0 until arr.length()){
        val item=arr.optJSONObject(i)?:continue
        val id=item.optString("id")
        if(id.isBlank() || !item.optBoolean("is_streamable",true) || !item.optBoolean("is_available",true))continue
        val access=item.optJSONObject("access")
        if(access!=null && access.has("stream") && !access.optBoolean("stream",false))continue
        val user=item.optJSONObject("user")
        val art=item.optJSONObject("artwork")
        val dl=item.optJSONObject("download")
        val permitted=item.optBoolean("is_downloadable",false) &&
          (dl==null || dl.optBoolean("is_downloadable",false))
        add(OpenSong(
          "Audius",id,item.optString("title","Untitled"),
          user?.optString("name","Audius Artist")?:"Audius Artist",
          art?.optString("480x480").orEmpty().ifBlank{art?.optString("150x150").orEmpty()},
          "https://api.audius.co/v1/tracks/"+enc(id)+"/stream?app_name=SauneVibe",
          if(permitted)"https://api.audius.co/v1/tracks/"+enc(id)+"/download?app_name=SauneVibe" else null,
          "Artist controlled • check source","","https://audius.co"
        ))
      }
    }
  }

  private fun jamendo(search:String,clientId:String,mode:String):List<OpenSong>{
    require(clientId.isNotBlank()){"Enter your Jamendo developer client ID to use Jamendo."}
    val url="https://api.jamendo.com/v3.0/tracks/?format=json&limit=30&include=licenses&client_id="+enc(clientId)+
      (if(search.isNotBlank())"&namesearch="+enc(search)
       else "&order="+if(mode=="Latest")"releasedate_desc" else "popularity_total")
    val arr=fetchJson(url).optJSONArray("results")?:return emptyList()
    return buildList{
      for(i in 0 until arr.length()){
        val item=arr.optJSONObject(i)?:continue
        val audio=item.optString("audio")
        if(!audio.startsWith("https://"))continue
        val license=item.optString("license_ccurl")
        val dl=item.optString("audiodownload")
        val allowed=item.optBoolean("audiodownload_allowed",false) &&
          dl.startsWith("https://") && license.startsWith("https://creativecommons.org/licenses/by/") &&
          !license.contains("-nc")
        add(OpenSong(
          "Jamendo",item.optString("id"),item.optString("name","Untitled"),
          item.optString("artist_name","Jamendo Artist"),item.optString("image"),
          audio,if(allowed)dl else null,
          "Creative Commons • verify license",license,
          item.optString("shareurl").ifBlank{"https://www.jamendo.com"}
        ))
      }
    }
  }

  private fun plain(s:String)=s.replace(Regex("<[^>]*>"),"")
    .replace("&amp;","&").trim()

  private fun commons(search:String):List<OpenSong>{
    val q=if(search.isBlank())"instrumental music" else search
    val api="https://commons.wikimedia.org/w/api.php?action=query&generator=search"+
      "&gsrnamespace=6&gsrlimit=25&gsrsearch="+enc(q+" filetype:audio")+
      "&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&format=json"
    val pages=fetchJson(api).optJSONObject("query")?.optJSONObject("pages")?:return emptyList()
    return buildList{
      for(k in pages.keys()){
        val page=pages.optJSONObject(k)?:continue
        val info=page.optJSONArray("imageinfo")?.optJSONObject(0)?:continue
        if(!info.optString("mime").startsWith("audio/"))continue
        val audio=info.optString("url")
        if(!audio.startsWith("https://"))continue
        val extra=info.optJSONObject("extmetadata")?:continue
        val license=plain(extra.optJSONObject("LicenseShortName")?.optString("value").orEmpty())
        val open=license.startsWith("CC BY",true) || license.equals("CC0",true) ||
          license.contains("public domain",true)
        if(!open || license.contains("NC",true))continue
        val name=page.optString("title").removePrefix("File:")
        val artist=plain(extra.optJSONObject("Artist")?.optString("value").orEmpty())
          .ifBlank{"Wikimedia contributor"}.take(100)
        val mp3=info.optString("mime").contains("mpeg") || audio.substringBefore("?").endsWith(".mp3",true)
        add(OpenSong(
          "Wikimedia",page.optString("pageid",k),name,artist,"",audio,
          if(mp3)audio else null,license,
          plain(extra.optJSONObject("LicenseUrl")?.optString("value").orEmpty()),
          info.optString("descriptionurl").ifBlank{"https://commons.wikimedia.org"}
        ))
      }
    }
  }
}
