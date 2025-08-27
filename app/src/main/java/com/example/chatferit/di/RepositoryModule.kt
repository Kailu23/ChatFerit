package com.example.chatferit.di

import com.example.chatferit.data.repository.UserRepository
import com.example.chatferit.data.repository.iUserRepository
import dagger.Binds
import dagger.Module
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
}

