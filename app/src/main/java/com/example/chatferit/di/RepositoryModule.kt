package com.example.chatferit.di

import com.example.chatferit.data.repository.FriendRepository
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iFriendRepository
import com.example.chatferit.data.repository.iUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepository
    ): iUserRepository

    companion object{
        @Provides
        @Singleton
        fun provideFriendRepository(
            firebaseAuth: FirebaseAuth,
            firebaseDatabase: FirebaseDatabase
        ) : iFriendRepository {
            return FriendRepository(firebaseAuth, firebaseDatabase)
        }
    }
}

