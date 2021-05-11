package com.example.autumn.data.remote

import android.media.browse.MediaBrowser
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.example.autumn.data.remote.State.STATE_CREATED
import com.example.autumn.data.remote.State.STATE_INITIALIZED
import com.example.autumn.data.remote.State.STATE_INITIALIZING
import com.example.autumn.data.remote.State.STATE_ERROR
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicSource @Inject constructor(
    private val musicDatabase: MusicDatabase
) {
    private val onReadyListeners= mutableListOf<(Boolean)-> Unit>()
    var songs = emptyList<MediaMetadataCompat>()

    suspend fun fetchMedia() = withContext(Dispatchers.IO){
        state=STATE_INITIALIZING
        val allSongs=musicDatabase.getAllSongs()
        songs=allSongs.map { song->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST,song.artist)
                .putString(METADATA_KEY_TITLE,song.name)
                .putString(METADATA_KEY_DISPLAY_TITLE,song.name)
                .putString(METADATA_KEY_MEDIA_URI,song.songUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_ART_URI,song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE,song.artist)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION,song.artist)
                .putString(METADATA_KEY_MEDIA_ID,song.songId)
                .putString(METADATA_KEY_DISPLAY_ICON_URI,song.imageUrl)
                .build()
        }
        state=STATE_INITIALIZED
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory):ConcatenatingMediaSource{
        val concatenatingMediaSource=ConcatenatingMediaSource()
        songs.forEach{ song->
            val mediaSource=ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())

            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }
    fun asMediaItem()=songs.map { song->
        val desc=MediaDescriptionCompat.Builder()
            .setMediaId(song.description.mediaId)
            .setIconUri(song.description.iconUri)
            .setMediaUri(song.description.mediaUri)
            .setTitle(song.description.title)
            .setSubtitle(song.description.subtitle)
            .build()
        MediaBrowserCompat.MediaItem(desc,FLAG_PLAYABLE)
    }




    private var state:State= STATE_CREATED
        set(value){
          if(value==STATE_INITIALIZED|| value==STATE_ERROR){
              synchronized(onReadyListeners){
                  field=value
              onReadyListeners.forEach { listeners ->
                  listeners(value == STATE_INITIALIZED)
              }
              }
          }else{
              field=value
          }
        }

    fun whenReady(action: (Boolean)-> Unit):Boolean{
        if(state==STATE_CREATED||state==STATE_INITIALIZING){
            onReadyListeners+=action
            return false
        }else{
            action(state==STATE_INITIALIZED)
            return true
        }
    }

}

enum class State{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}