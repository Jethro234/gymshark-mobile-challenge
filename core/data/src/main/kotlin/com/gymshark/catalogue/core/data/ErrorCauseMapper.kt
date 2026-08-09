package com.gymshark.catalogue.core.data

import com.gymshark.catalogue.core.model.ErrorCause
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Classifies a failure into the closed set the UI can speak about
 * (docs/ARCHITECTURE.md §5). Ordered most-specific first: a timeout and a truncated body are
 * both, at the JVM level, kinds of [IOException], so the narrower checks must run before the
 * general [IOException] fallback or they would never be reached.
 */
public fun Throwable.toErrorCause(): ErrorCause =
    when (this) {
        is SocketTimeoutException -> ErrorCause.NoConnection
        is EOFException -> ErrorCause.Malformed
        is HttpException -> ErrorCause.Server
        is SerializationException -> ErrorCause.Malformed
        is ProductNotFoundException -> ErrorCause.NotFound
        is IOException -> ErrorCause.NoConnection
        else -> ErrorCause.Unknown
    }
