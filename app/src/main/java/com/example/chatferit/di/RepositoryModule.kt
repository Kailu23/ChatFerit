package com.example.chatferit.di

import com.example.chatferit.data.repository.AuthRepository
import com.example.chatferit.data.repository.FriendRepository
import com.example.chatferit.data.repository.IAuthRepository
import com.example.chatferit.data.repository.ThemeRepository
import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iFriendRepository
import com.example.chatferit.data.repository.iThemeRepository
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

    @Binds
    @Singleton
    abstract fun bindThemeRepository(themeRepository: ThemeRepository) : iThemeRepository

    companion object{
        @Provides
        @Singleton
        fun provideFriendRepository(
            firebaseAuth: FirebaseAuth,
            firebaseDatabase: FirebaseDatabase
        ) : iFriendRepository {
            return FriendRepository(firebaseAuth, firebaseDatabase)
        }

        @Provides
        @Singleton
        fun provideAuthRepository(firebaseAuth: FirebaseAuth): IAuthRepository {
            return AuthRepository(firebaseAuth)
        }


    }

}

