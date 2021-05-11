package com.example.autumn.data.remote

import com.example.autumn.data.entity.Song
import com.example.autumn.util.Constants.SONG_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.lang.Exception

const val TAG="MusicDatabase"

class MusicDatabase {


    private val fireStore = FirebaseFirestore.getInstance()
    private val songCollection =fireStore.collection(SONG_COLLECTION)


    suspend fun getAllSongs(): List<Song>{
       return try {
            songCollection.get().await().toObjects(Song::class.java)
        }catch (e: Exception){
           // Log.d(TAG,e.message.toString())
            emptyList<Song>()
        }
    }
}