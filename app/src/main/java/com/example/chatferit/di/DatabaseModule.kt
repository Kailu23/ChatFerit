package com.example.chatferit.di

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
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
}