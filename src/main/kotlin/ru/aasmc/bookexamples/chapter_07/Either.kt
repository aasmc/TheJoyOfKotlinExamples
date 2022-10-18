package ru.aasmc.bookexamples.chapter_07

import ru.aasmc.bookexamples.chapter05.List

sealed class Either<E, out A> {

    /**
     * Maps current [Either] to [Either] that holds previous [E] value, or
     * new [B] value that is received after mapping [A] value to [B] value.
     */
    abstract fun <B> map(f: (A) -> B): Either<E, B>

    abstract fun <B> flatMap(f: (A) -> Either<E, B>): Either<E, B>

    fun getOrElse(defaultValue: () -> @UnsafeVariance A): A =
        when (this) {
            is Left -> defaultValue()
            is Right -> value
        }

    fun orElse(defaultValue: () -> Either<E, @UnsafeVariance A>): Either<E, A> =
        map { this }.getOrElse(defaultValue)

    /**
     * Represents failure.
     */
    internal class Left<E, out A>(internal val value: E) : Either<E, A>() {

        override fun toString(): String = "Left ($value)"

        override fun <B> map(f: (A) -> B): Either<E, B> = Left(value)

        override fun <B> flatMap(f: (A) -> Either<E, B>): Either<E, B> = Left(value)
    }

    /**
     * Represents success.
     */
    internal class Right<E, out A>(internal val value: A) : Either<E, A>() {
        override fun toString(): String = "Right ($value)"

        override fun <B> map(f: (A) -> B): Either<E, B> = Right(f(value))

        override fun <B> flatMap(f: (A) -> Either<E, B>): Either<E, B> =
            f(value)
    }

    companion object {
        fun <E, A> left(value: E): Either<E, A> = Left(value)
        fun <E, A> right(value: A): Either<E, A> = Right(value)
    }
}

fun <A : Comparable<A>> max(list: List<A>): Either<String, A> = when (list) {
    List.Nil -> Either.left("max called on an empty list")
    is List.Cons -> Either.right(list.foldLeft(list.head) { x ->
        { y ->
            if (x.compareTo(y) == 0) x else y
        }
    })
}