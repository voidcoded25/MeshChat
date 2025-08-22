package com.meshchat.app.core

/**
 * A sealed class representing the result of an operation.
 * Can be either Success with data, or Error with an exception.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun exceptionOrNull(): Exception? = when (this) {
        is Success -> null
        is Error -> exception
    }
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Exception): Result<Nothing> = Error(exception)
    }
}
