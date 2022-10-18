package ru.aasmc.bookexamples.chapter_07

import java.io.IOException
import java.io.Serializable

sealed class Result<out A> : Serializable {
    internal class Failure<out A>(
        internal val exception: RuntimeException
    ) : Result<A>() {
        override fun toString(): String = "Failure (${exception.message})"

        override fun <B> map(f: (A) -> B): Result<B> = Failure(exception)

        override fun <B> flatMap(f: (A) -> Result<B>): Result<B> = Failure(exception)

        override fun mapFailure(message: String): Result<A> =
            Failure(RuntimeException(message, exception))

        override fun forEach(
            onSuccess: (A) -> Unit,
            onFailure: (RuntimeException) -> Unit,
            onEmpty: () -> Unit
        ) = onFailure(exception)
    }

    internal class Success<out A>(
        internal val value: A
    ) : Result<A>() {
        override fun toString(): String = "Success ($value)"

        override fun <B> map(f: (A) -> B): Result<B> = try {
            Success(f(value))
        } catch (e: RuntimeException) {
            Failure(e)
        } catch (e: Exception) {
            Failure(RuntimeException(e))
        }

        override fun <B> flatMap(f: (A) -> Result<B>): Result<B> = try {
            f(value)
        } catch (e: RuntimeException) {
            Failure(e)
        } catch (e: Exception) {
            Failure(RuntimeException(e))
        }

        override fun mapFailure(message: String): Result<A> = this

        override fun forEach(
            onSuccess: (A) -> Unit,
            onFailure: (RuntimeException) -> Unit,
            onEmpty: () -> Unit
        ) = onSuccess(value)
    }

    internal object Empty : Result<Nothing>() {
        override fun toString(): String = "Empty"

        override fun <B> map(f: (Nothing) -> B): Result<B> = Empty

        override fun <B> flatMap(f: (Nothing) -> Result<B>): Result<B> = Empty

        override fun mapFailure(message: String): Result<Nothing> = this

        override fun forEach(
            onSuccess: (Nothing) -> Unit,
            onFailure: (RuntimeException) -> Unit,
            onEmpty: () -> Unit
        ) = onEmpty()
    }

    abstract fun <B> map(f: (A) -> B): Result<B>

    abstract fun <B> flatMap(f: (A) -> Result<B>): Result<B>

    abstract fun mapFailure(message: String): Result<A>

    abstract fun forEach(
        onSuccess: (A) -> Unit = {},
        onFailure: (RuntimeException) -> Unit = {},
        onEmpty: () -> Unit = {}
    )

    fun getOrElse(defaultValue: @UnsafeVariance A): A = when (this) {
        is Success -> this.value
        else -> defaultValue
    }

    fun orElse(defaultValue: () -> Result<@UnsafeVariance A>): Result<A> =
        when (this) {
            is Success -> this
            else -> try {
                defaultValue()
            } catch (e: RuntimeException) {
                Result.failure<A>(e)
            } catch (e: Exception) {
                Result.failure<A>(RuntimeException(e))
            }
        }

    fun filter(p: (A) -> Boolean): Result<A> = flatMap { value ->
        if (p(value)) {
            Success(value)
        } else {
            Result.failure("Condition not matched")
        }
    }

    fun filter(message: String, p: (A) -> Boolean): Result<A> = flatMap {
        if (p(it)) {
            Success(it)
        } else {
            Result.failure(message)
        }
    }

    fun exists(p: (A) -> Boolean): Boolean = map(p).getOrElse(false)

    companion object {
        operator fun <A> invoke(a: A? = null): Result<A> =
            when (a) {
                null -> Failure(NullPointerException())
                else -> Success(a)
            }

        operator fun <A> invoke(): Result<A> = Empty

        operator fun <A> invoke(a: A? = null, message: String): Result<A> =
            when (a) {
                null -> failure(message)
                else -> Success(a)
            }

        operator fun <A> invoke(a: A? = null, p: (A) -> Boolean): Result<A> =
            when (a) {
                null -> failure(NullPointerException())
                else -> {
                    if (p(a)) {
                        Success(a)
                    } else {
                        Empty
                    }
                }
            }

        operator fun <A> invoke(a: A? = null, message: String, p: (A) -> Boolean): Result<A> =
            when (a) {
                null -> Failure(NullPointerException())
                else -> when {
                    p(a) -> Success(a)
                    else -> Failure(
                        IllegalArgumentException(
                            "Argument $a doesn't match condition. $message"
                        )
                    )
                }
            }

        fun <A> failure(message: String): Result<A> =
            Failure(IllegalStateException(message))

        fun <A> failure(exception: RuntimeException): Result<A> =
            Failure(exception)

        fun <A> failure(exception: Exception): Result<A> =
            Failure(IllegalStateException(exception))
    }
}

fun <A, B> lift(f: (A) -> B): (Result<A>) -> Result<B> = { aRes ->
    aRes.map(f)
}

fun <A, B, C> lift2(f: (A) -> (B) -> C): (Result<A>) -> (Result<B>) -> Result<C> = { aRes ->
    { bRes ->
        // first map to Result of function (B) -> C
        // then flatmap the result to Result<C>
        aRes.map(f).flatMap {
            bRes.map(it)
        }
    }
}

fun <A, B, C, D> lift3(
    f: (A) -> (B) -> (C) -> D
): (Result<A>) -> (Result<B>) -> (Result<C>) -> Result<D> = { aRes ->
    { bRes ->
        { cRes ->
            aRes.map(f).flatMap { bcd ->
                bRes.map(bcd).flatMap {
                    cRes.map(it)
                }
            }
        }
    }
}

fun <A, B, C> map2(
    a: Result<A>,
    b: Result<B>,
    f: (A) -> (B) -> C
): Result<C> = lift2(f)(a)(b)

fun <K, V> Map<K, V>.getResult(key: K) = when {
    this.containsKey(key) -> Result(this[key])
    else -> Result.Empty
}

data class Toon private constructor(
    val firstName: String,
    val lastName: String,
    val email: Result<String>
) {
    companion object {
        operator fun invoke(firstName: String, lastName: String) =
            Toon(firstName, lastName, Result.Empty)

        operator fun invoke(firstName: String, lastName: String, email: String) =
            Toon(firstName, lastName, Result(email))
    }
}

fun main() {
    val toons: Map<String, Toon> = mapOf(
        "Mickey" to Toon("Mickey", "Mouse", "mickey@email.com"),
        "Minnie" to Toon("Minnie", "Mouse"),
        "Donald" to Toon("Donald", "Duck", "donald@email.com"),
    )
    val toon = getName()
        .flatMap(toons::getResult)
        .flatMap(Toon::email)
    println(toon)
    val z = 125
    val result = if (z % 2 == 0) Result(z) else Result()
    result.forEach(
        onSuccess = { println(it) },
        onEmpty = { println("This one is odd") }
    )

    val createPerson: (String) -> (String) -> (String) -> Toon =
        { x -> { y -> { z -> Toon(x, y, z) } } }

    val toonRes = lift3(createPerson)(getFirstName())(getLastName())(getMail())

    val toonComprehension = getFirstName()
        .flatMap { firstName ->
            getLastName()
                .flatMap { lastName ->
                    getMail()
                        .map { mail ->
                            Toon(firstName, lastName, mail)
                        }
                }
        }
}

fun getName(): Result<String> = try {
    validate("Mickey")
} catch (e: IOException) {
    Result.failure(e)
}

fun validate(name: String?): Result<String> = when {
    name?.isNotEmpty() ?: false -> Result(name)
    else -> Result.failure(IOException())
}

fun getFirstName(): Result<String> = Result("Mickey")
fun getLastName(): Result<String> = Result("Mouse")
fun getMail(): Result<String> = Result("mickey@disney.com")














