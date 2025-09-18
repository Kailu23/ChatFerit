package com.example.chatferit.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}