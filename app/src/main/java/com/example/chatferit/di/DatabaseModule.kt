package com.example.chatferit.di

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)  object DatabaseModule {
    @Provides
    @Singleton
    fun ProvideFirebaseDatabase(): FirebaseDatabase {
        val url : String = "https://chatferit-default-rtdb.europe-west1.firebasedatabase.app/"
        return FirebaseDatabase.getInstance(url)
    }

    @Provides
    @Singleton // Optional: if you want a single instance
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}