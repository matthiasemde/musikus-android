package app.musikus.di

import javax.inject.Qualifier


// Source: https://medium.com/androiddevelopers/create-an-application-coroutinescope-using-hilt-dd444e721528

// CoroutinesQualifiers.kt file

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainImmediateDispatcher
