package ru.aasmc.bookexamples.chapter_09

import ru.aasmc.bookexamples.chapter_07.Result
import ru.aasmc.bookexamples.chapter_08.List

class Lazy<out A>(function: () -> A) : () -> A {
    // lazily caches the value
    private val value: A by lazy(function)

    // returns the lazily cached value
    override operator fun invoke(): A = value

    fun <B> map(f: (A) -> B): Lazy<B> = Lazy { f(value) }

    fun <B> flatMap(f: (A) -> Lazy<B>): Lazy<B> = Lazy { f(value)() }

    fun forEach(
        condition: Boolean,
        ifTrue: (A) -> Unit,
        ifFalse: (A) -> Unit
    ) = if (condition) ifTrue(value) else ifFalse(value)

    fun forEach(
        condition: Boolean,
        ifTrue: (A) -> Unit,
        ifFalse: () -> Unit = {}
    ) = if (condition) ifTrue(value) else ifFalse()

    fun forEach(
        condition: Boolean,
        ifTrue: () -> Unit,
        ifFalse: (A) -> Unit
    ) = if (condition) ifTrue() else ifFalse(value)

    companion object {
        /**
         * Curried function that takes as its argument a curried function of two evaluated
         * String arguments and returns the corresponding function for unevaluated argument that
         * produces the same, but non evaluated result.
         */
        val lift2: ((String) -> (String) -> String) -> (Lazy<String>) -> (Lazy<String>) -> Lazy<String> =
            { f -> // argument of the function
                { ls1 ->
                    { ls2 ->
                        Lazy {
                            f(ls1())(ls2())
                        }
                    }
                }
            }
    }
}

/**
 * Transforms a given [lst] into Lazy<List<A>> so that the list might
 * lazily be composed with functions of A. This is done without evaluating
 * the data.
 */
fun <A> sequence(lst: List<Lazy<A>>): Lazy<List<A>> = Lazy {
    lst.map { it.invoke() }
}

fun <A, B, C> lift2(function: (A) -> (B) -> C): (Lazy<A>) -> (Lazy<B>) -> Lazy<C> = { first ->
    { second ->
        Lazy {
            function(first())(second())
        }
    }
}

/**
 * Returns a [Result] of [List] that's not evaluated and that will turn
 * into [Result.Success] if all evaluations succeed or a [Result.Failure]
 * otherwise.
 */
fun <A> sequenceResult(lst: List<Lazy<A>>): Lazy<Result<List<A>>> = Lazy {
    ru.aasmc.bookexamples.chapter_08.sequence(
        lst.map {
            Result(it())
        }
    )
}























