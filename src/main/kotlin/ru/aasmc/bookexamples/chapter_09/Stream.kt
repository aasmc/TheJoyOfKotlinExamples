package ru.aasmc.bookexamples.chapter_09

import ru.aasmc.bookexamples.NotStackSafe
import ru.aasmc.bookexamples.StackSafe
import ru.aasmc.bookexamples.chapter_07.Result
import ru.aasmc.bookexamples.chapter_08.List

sealed class Stream<out A> {
    abstract fun isEmpty(): Boolean

    // can return Result.Empty when called on an empty Stream
    abstract fun head(): Result<A>

    abstract fun tail(): Result<Stream<A>>

    abstract fun takeAtMost(n: Int): Stream<A>

    abstract fun dropAtMost(n: Int): Stream<A>

    @StackSafe
    fun dropAtMostSafe(n: Int): Stream<A> = Companion.dropAtMost(n, this)

    @StackSafe
    fun toList(): List<A> = Companion.toList(this)

    /**
     * Returns a [Stream] containing all starting elements as
     * long as a condition is matched.
     */
    abstract fun takeWhile(p: (A) -> Boolean): Stream<A>

    fun takeWhileViaFoldRight(p: (A) -> Boolean): Stream<A> =
        foldRight(Lazy { Empty }) { elem ->
            { lazy: Lazy<Stream<A>> ->
                if (p(elem)) {
                    cons(Lazy { elem }, lazy)
                } else {
                    Empty
                }
            }
        }

    @StackSafe
    fun dropWhile(p: (A) -> Boolean): Stream<A> = Companion.dropWhile(p, this)

    @StackSafe
    fun exists(p: (A) -> Boolean): Boolean = Companion.exists(this, p)

    abstract fun <B> foldRight(z: Lazy<B>, f: (A) -> (Lazy<B>) -> B): B

    fun headSafeViaFoldRight(): Result<A> =
        foldRight(Lazy { Result<A>() }) { elem ->
            {
                Result(elem)
            }
        }

    fun <B> mapViaFoldRight(f: (A) -> B): Stream<B> =
        foldRight(Lazy { Empty }) { elem ->
            { b: Lazy<Stream<B>> ->
                cons(Lazy { f(elem) }, b)
            }
        }

    /**
     * Applies the filter [p] to each element in the stream,
     * if the result is true, the element is used to create a new
     * stream by "consing" it with the current stream result. Otherwise,
     * the current stream result is left unchanged (calling acc() doesn't evaluate any elements).
     */
    fun filterViaFoldRight(p: (A) -> Boolean): Stream<A> =
        foldRight(Lazy { Empty }) { elem ->
            { acc: Lazy<Stream<A>> ->
                if (p(elem)) cons(Lazy { elem }, acc) else acc()
            }
        }

    fun append(stream2: Lazy<Stream<@UnsafeVariance A>>): Stream<A> =
        this.foldRight(stream2) { elem -> // starting from the rightmost element of this stream
            { b: Lazy<Stream<A>> ->
                cons(
                    Lazy { elem },
                    b
                ) // construct a new stream with the element as head, and stream2 as tail
            }
        }

    fun <B> flatMapViaFoldRight(f: (A) -> Stream<B>): Stream<B> =
        foldRight(Lazy { Empty as Stream<B> }) { elem ->
            { acc: Lazy<Stream<B>> ->
                f(elem).append(acc)
            }
        }

    fun find(p: (A) -> Boolean): Result<A> = filterViaFoldRight(p).head()

    private object Empty : Stream<Nothing>() {
        override fun head(): Result<Nothing> = Result()

        override fun tail(): Result<Stream<Nothing>> = Result()

        override fun isEmpty(): Boolean = true

        override fun takeAtMost(n: Int): Stream<Nothing> = this

        override fun dropAtMost(n: Int): Stream<Nothing> = this

        override fun takeWhile(p: (Nothing) -> Boolean): Stream<Nothing> = this

        override fun <B> foldRight(z: Lazy<B>, f: (Nothing) -> (Lazy<B>) -> B): B = z()

    }

    private class Cons<out A>(
        internal val hd: Lazy<A>,
        internal val tl: Lazy<Stream<A>>
    ) : Stream<A>() {
        override fun isEmpty(): Boolean = false

        override fun head(): Result<A> = Result(hd())

        override fun tail(): Result<Stream<A>> = Result(tl())

        /**
         * Tests whether the argument [n] is larger than zero. If it is,
         * it returns the head of the stream "consed" with the result of
         * recursively applying the [takeAtMost] with argument [n - 1]. If
         * n is less than or equals 0, it returns the empty stream
         */
        override fun takeAtMost(n: Int): Stream<A> = when {
            n > 0 -> cons(hd, Lazy { tl().takeAtMost(n - 1) })
            else -> Empty
        }

        @NotStackSafe
        override fun dropAtMost(n: Int): Stream<A> = when {
            n > 0 -> tl().dropAtMost(n - 1)
            else -> this
        }

        @StackSafe
        override fun takeWhile(p: (A) -> Boolean): Stream<A> = when {
            p(hd()) -> cons(hd, Lazy { tl().takeWhile(p) })
            else -> Empty
        }

        @NotStackSafe
        override fun <B> foldRight(z: Lazy<B>, f: (A) -> (Lazy<B>) -> B): B =
            f(hd())(Lazy { tl().foldRight(z, f) })
    }

    companion object {
        fun <A> cons(hd: Lazy<A>, tl: Lazy<Stream<A>>): Stream<A> = Cons(hd, tl)

        operator fun <A> invoke(): Stream<A> = Empty

        /**
         * Returns an infinite stream of successive integers starting from the
         * given value.
         */
        fun from(i: Int): Stream<Int> = iterate(i) { it + 1 }

        fun <A> repeat(f: () -> A): Stream<A> =
            cons(Lazy { f() }, Lazy { repeat(f) })

        tailrec fun <A> dropAtMost(n: Int, stream: Stream<A>): Stream<A> = when {
            n > 0 -> {
                when (stream) {
                    is Empty -> stream
                    is Cons -> dropAtMost(n - 1, stream.tl())
                }
            }
            else -> stream
        }

        fun <A> toList(stream: Stream<A>): List<A> {
            tailrec fun <A> toList_(stream: Stream<A>, acc: List<A>): List<A> = when (stream) {
                Empty -> acc
                is Cons -> toList_(stream.tl(), acc.cons(stream.hd()))
            }
            return toList_(stream, List()).reverse() // need to reverse, since the list is accumulated backwards
        }

        /**
         * Generic function that accepts an initial value [seed] and a function
         * to produce the next value based on the initial value and returns
         * an infinite [Stream]. It is stack-safe because function invocations
         * are wrapped in Lazy.
         */
        @StackSafe
        fun <A> iterate(seed: A, f: (A) -> A): Stream<A> =
            cons(Lazy { seed }, Lazy { iterate(f(seed), f) })

        fun <A> iterate(seed: Lazy<A>, f: (A) -> A): Stream<A> =
            cons(seed, Lazy { iterate(f(seed()), f) })

        @StackSafe
        tailrec fun <A> dropWhile(p: (A) -> Boolean, stream: Stream<A>): Stream<A> = when (stream) {
            Empty -> stream
            is Cons -> {
                when {
                    p(stream.hd()) -> dropWhile(p, stream.tl())
                    else -> stream
                }
            }
        }

        @StackSafe
        tailrec fun <A> exists(stream: Stream<A>, p: (A) -> Boolean): Boolean = when (stream) {
            Empty -> false
            is Cons -> when {
                p(stream.hd()) -> true
                else -> exists(stream.tl(), p)
            }
        }
    }
}


fun main() {
    val stream = Stream.from(1)
    stream.head().forEach({ println(it) })
    stream.tail().flatMap { it.head() }.forEach({ println(it) })
    stream.tail().flatMap {
        it.tail().flatMap { it.head() }
    }.forEach({ println(it) })

    val list = Stream.iterate(0, ::inc)
        .takeAtMost(60_000)
        .dropAtMost(10_000)
        .takeAtMost(10)
        .toList()
    println(list)

    val lst = Stream.iterate(0, ::inc)
        .takeWhileViaFoldRight {
            it < 1_000_000
        }
        .takeWhile {
            it < 10
        }
        .toList()

    println(lst)

}


fun inc(i: Int) = (i + 1).let {
    println("Generating $it")
    it
}


fun fibs(): Stream<Int> = Stream.iterate(Pair(1, 1)) { (left, right) ->
    Pair(right, left + right)
}.mapViaFoldRight { elem -> elem.first }





















