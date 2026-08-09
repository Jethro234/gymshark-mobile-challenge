package com.gymshark.catalogue.core.model

/**
 * A closed set of failure categories the UI can speak about in plain language. A raw
 * [Throwable] never reaches presentation state — see docs/ARCHITECTURE.md §5.
 */
public sealed interface ErrorCause {
    public data object NoConnection : ErrorCause

    public data object Server : ErrorCause

    public data object Malformed : ErrorCause

    public data object NotFound : ErrorCause

    public data object Unknown : ErrorCause
}
