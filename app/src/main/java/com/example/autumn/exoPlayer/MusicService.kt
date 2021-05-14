package com.example.autumn.exoPlayer

import android.app.PendingIntent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.autumn.data.remote.FirebaseMusicSource
import com.example.autumn.exoPlayer.callbacks.MusicPlaybackPreparer
import com.example.autumn.exoPlayer.callbacks.MusicPlayerEventListener
import com.example.autumn.exoPlayer.callbacks.MusicPlayerNotificationListener
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject

const val SERVICE_TAG="MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory
    @Inject
    lateinit var exoPlayer: SimpleExoPlayer
    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    private val serviceJob= Job()
    private val serviceScope= CoroutineScope(Dispatchers.Main+serviceJob)

    private lateinit var musicNotificationManager: MusicNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    var isForegroundService=false
    private var curPlayingSong : MediaMetadataCompat?=null


    override fun onCreate() {
        super.onCreate()
        val activityIntent=packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }
        mediaSession= MediaSessionCompat.fromMediaSession(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive=true
        }
        sessionToken=mediaSession.sessionToken

        musicNotificationManager= MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ){
            // this is lambda function
            // to update notification on song change

        }

        val musicPlayerPreparer =MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong=it
            preparePlayer(
                firebaseMusicSource.songs,
                it,
                playNow = true
                )
        }

        mediaSessionConnector=MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlayerPreparer)
        mediaSessionConnector.setPlayer(exoPlayer)
        exoPlayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoPlayer)
    }
    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow:Boolean
    ){
        val currSongIndex=if(curPlayingSong==null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currSongIndex,0L)
        exoPlayer.playWhenReady=playNow
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}