package ru.aasmc.bookexamples.chapter_08

import ru.aasmc.bookexamples.chapter_07.Result
import ru.aasmc.bookexamples.chapter_07.map2

sealed class List<out A> {

    abstract val length: Int

    abstract fun isEmpty(): Boolean

    fun cons(a: @UnsafeVariance A): List<A> = Cons(a, this)

    fun setHead(a: @UnsafeVariance A): List<A> = when (this) {
        is Nil -> error("You cant set head on an empty list.")
        is Cons -> tail.cons(a)
    }

    fun drop(n: Int): List<A> = drop(this, n)

    fun dropWhile(p: (A) -> Boolean): List<A> = Companion.dropWhile(this, p)

    fun concat(list: List<@UnsafeVariance A>): List<A> = concat(this, list)

    fun reverse(): List<A> = reverse(List.invoke(), this)

    abstract fun init(): List<A>

    fun <B> foldRight(identity: B, f: (A) -> (B) -> B): B =
        foldRight(this, identity, f)

    fun <B> foldLeft(identity: B, f: (B) -> (A) -> B): B =
        foldLeft(identity, this, f)

    /**
     * Time Complexity O(N).
     * Has poor performance and can cause StackOverflow error.
     */
    fun lengthFoldRight(): Int = foldRight(0) { _ -> { it + 1 } }

    /**
     * Inefficient because of Time Complexity O(N).
     * Won't cause StackOverflow error because of the tail recursion.
     */
    fun lengthFoldLeft(): Int = foldLeft(0) { acc -> { _ -> acc + 1 } }

    fun reverseFoldLeft(): List<A> =
        foldLeft(Nil as List<A>) { acc -> { elem -> acc.cons(elem) } }

    fun <B> foldRightViaFoldLeft(identity: B, f: (A) -> (B) -> B): B =
        this.reverseFoldLeft().foldLeft(identity) { acc ->
            { elem ->
                f(elem)(acc)
            }
        }

    fun concatFoldRight(list: List<@UnsafeVariance A>): List<A> = concatFoldRight(this, list)

    fun concatFoldLeft(list: List<@UnsafeVariance A>): List<A> = concatFoldLeft(this, list)

    fun <B> coFoldRight(identity: B, f: (A) -> (B) -> B): B =
        coFoldRight(identity, this.reverseFoldLeft(), f) // need to reverse the list to ensure right to left traversal

    fun <B> map(f: (A) -> B): List<B> =
        foldLeft(Nil) { acc: List<B> ->
            { elem ->
                Cons(f(elem), acc)
            }
        }.reverseFoldLeft()

    fun filter(p: (A) -> Boolean): List<A> =
        coFoldRight(Nil) { elem ->
            { result: List<A> ->
                if (p(elem)) Cons(elem, result) else result
            }
        }

    fun <B> flatMap(f: (A) -> List<B>): List<B> =
        flatten(map(f))

    fun filterUsingFlatMap(p: (A) -> Boolean): List<A> =
        flatMap { elem -> if (p(elem)) List(elem) else Nil }

    abstract fun headSafe(): Result<A>

    fun lastSafeTrivial(): Result<A> = when (this) {
        Nil -> Result()
        is Cons -> when (tail) {
            Nil -> Result(head)
            is Cons -> tail.lastSafeTrivial()
        }
    }

    fun lastSafeCorecursive(): Result<A> = lastSafeCorecursive(this)

    fun lastSafe(): Result<A> =
        foldLeft(Result()) { ignoredAcc: Result<A> ->
            { elem: A ->
                Result(elem)
            }
        }


    internal object Nil : List<Nothing>() {

        override val length: Int = 0

        override fun isEmpty(): Boolean = true

        override fun toString(): String = "[NIL]"

        override fun init(): List<Nothing> {
            error("Cannot delete last element of an empty list.")
        }

        override fun headSafe(): Result<Nothing> = Result()
    }

    internal class Cons<out A>(
        internal val head: A,
        internal val tail: List<A>
    ) : List<A>() {

        override val length: Int = tail.length + 1

        override fun init(): List<A> = reverse().drop(1).reverse()

        override fun isEmpty(): Boolean = false

        override fun toString(): String = "[${toString("", this)}NIL]"

        private tailrec fun toString(acc: String, list: List<A>): String =
            when (list) {
                is Nil -> acc
                is Cons -> toString("$acc${list.head}, ", list.tail)
            }

        override fun headSafe(): Result<A> = Result(head)
    }

    companion object {
        operator fun <A> invoke(vararg az: A): List<A> =
            az.foldRight(Nil as List<A>) { elem: A, acc: List<A> ->
                Cons(elem, acc)
            }

        tailrec fun <A> drop(list: List<A>, n: Int): List<A> = when (list) {
            Nil -> list
            is Cons -> if (n <= 0) list else drop(list.tail, n - 1)
        }

        private tailrec fun <A> dropWhile(list: List<A>, p: (A) -> Boolean): List<A> =
            when (list) {
                Nil -> list
                is Cons -> if (p(list.head)) dropWhile(list.tail, p) else list
            }

        private fun <A> concat(list1: List<A>, list2: List<A>): List<A> = when (list1) {
            Nil -> list2
            is Cons -> concat(list1.tail, list2).cons(list1.head)
        }

        tailrec fun <A> reverse(acc: List<A>, list: List<A>): List<A> =
            when (list) {
                Nil -> acc
                is Cons -> reverse(acc.cons(list.head), list.tail)
            }

        fun <A, B> foldRight(
            list: List<A>,
            identity: B,
            f: (A) -> (B) -> B
        ): B = when (list) {
            Nil -> identity
            is Cons -> f(list.head)(foldRight(list.tail, identity, f))
        }

        private tailrec fun <A, B> foldLeft(
            acc: B,
            list: List<A>,
            f: (B) -> (A) -> B
        ): B = when (list) {
            Nil -> acc
            is Cons -> foldLeft(f(acc)(list.head), list.tail, f)
        }

        private tailrec fun <A, B> coFoldRight(
            acc: B,
            list: List<A>,
            f: (A) -> (B) -> B
        ): B = when (list) {
            Nil -> acc
            is Cons -> coFoldRight(f(list.head)(acc), list.tail, f)
        }

        fun <A> concatFoldRight(list1: List<A>, list2: List<A>): List<A> =
            foldRight(list1, list2) { elem -> { lst -> lst.cons(elem) } }

        /**
         * Stack-safe but less efficient that concatFoldRight.
         */
        fun <A> concatFoldLeft(list1: List<A>, list2: List<A>): List<A> =
            list1.reverseFoldLeft().foldLeft(list2) { acc -> acc::cons }

        tailrec fun <A> lastSafeCorecursive(list: List<A>): Result<A> = when (list) {
            Nil -> Result()
            is Cons -> when (list.tail) {
                Nil -> Result(list.head)
                is Cons -> lastSafeCorecursive(list.tail)
            }
        }
    }
}

fun sum(ints: List<Int>): Int = when (ints) {
    List.Nil -> 0
    is List.Cons -> ints.head + sum(ints.tail)
}

fun product(ints: List<Int>): Int = when (ints) {
    List.Nil -> 1
    is List.Cons -> ints.head * product(ints.tail)
}

fun product(doubles: List<Double>): Double = when (doubles) {
    List.Nil -> 1.0
    is List.Cons -> {
        if (doubles.head == 0.0) { // absorbing element
            0.0
        } else {
            doubles.head * product(doubles.tail)
        }
    }
}

fun <A, B> foldRight(
    list: List<A>,
    identity: B,
    f: (A) -> (B) -> B
): B = when (list) {
    List.Nil -> identity
    is List.Cons -> f(list.head)(foldRight(list.tail, identity, f))
}

fun sumFold(list: List<Int>): Int =
    foldRight(list, 0) { x: Int ->
        { y: Int ->
            x + y
        }
    }

fun productFold(list: List<Double>): Double =
    foldRight(list, 1.0) { x -> { y -> x * y } }

fun sumFoldLeft(list: List<Int>): Int = list.foldLeft(0) { acc -> { elem -> acc + elem } }

fun productFoldLeft(list: List<Double>): Double = list.foldLeft(1.0) { acc -> { elem -> acc * elem } }


fun <A> flatten(list: List<List<A>>): List<A> =
    list.coFoldRight(List.Nil) { elem -> elem::concat }

fun triple(list: List<Int>): List<Int> =
    List.foldRight(list, List.invoke()) { elem: Int ->
        { acc: List<Int> ->
            acc.cons(elem * 3)
        }
    }

fun doubleToString(list: List<Double>): List<String> =
    List.foldRight(list, List.invoke()) { elem: Double ->
        { acc: List<String> ->
            acc.cons(elem.toString())
        }
    }

/**
 * Returns a list containing all the success values in the original list,
 * ignoring failures and empty values.
 */
fun <A> flattenResult(list: List<Result<A>>): List<A> =
    list.flatMap { aRes -> // flattens the List of Lists
        aRes.map { elem -> // produces either List(elem) or List()
            List(elem)
        }.getOrElse(List())
    }

/**
 * Combines a [List<Result<A>>] into a [Result<List<A>>].
 * It'll be a Success<List<A>> if all values in the original list were Success instances,
 * or a Failure<List<A>> otherwise.
 */
fun <A> sequence(list: List<Result<A>>): Result<List<A>> =
    list.foldRight(Result(List())) { elem ->
        { acc ->
            map2(elem, acc) { mElem ->
                { mRes ->
                    mRes.cons(mElem)
                }
            }
        }
    }

fun <A, B> traverse(list: List<A>, f: (A) -> Result<B>): Result<List<B>> =
    list.foldRight(Result(List())) { elem ->
        { acc ->
            map2(f(elem), acc) { mElem ->
                { mAcc ->
                    mAcc.cons(mElem)
                }
            }
        }
    }

fun <A> sequenceViaTraverse(list: List<Result<A>>): Result<List<A>> =
    traverse(list) { res -> res }

/**
 * Combines the elements of two lists of different types to produce a new list,
 * given a function argument.
 */
fun <A, B, C> zipWith(
    list1: List<A>,
    list2: List<B>,
    f: (A) -> (B) -> C
): List<C> {
    tailrec fun zipWith_(
        acc: List<C>,
        list1: List<A>,
        list2: List<B>
    ): List<C> = when (list1) {
        List.Nil -> acc // means list1 has been fully traversed, so return accumulator
        is List.Cons -> when (list2) {
            List.Nil -> acc // means list2 has been fully traversed, so return accumulator
            is List.Cons -> {
                zipWith_(acc.cons(f(list1.head)(list2.head)), list1.tail, list2.tail)
            }
        }
    }
    return zipWith_(List(), list1, list2).reverse()
}

/**
 * Produces a list of all possible combinations of elements taken from both lists.
 * E.g. given list("a", "b", "c") and list("d", "e", "f") and string concatenation,
 * produces: List("ad", "ae", "af", "bd", "be", "bf", "cd", "ce", "cf").
 */
fun <A, B, C> product(
    list1: List<A>,
    list2: List<B>,
    f: (A) -> (B) -> C
): List<C> = list1.flatMap { aElem -> // for each elem in list1, flatten the resulting List of Lists.
    list2.map { bElem -> // for each elem in list2, produce a List(C) by applying f to aElem and bElem
        f(aElem)(bElem)
    }
}

fun <A, B> unzip(list: List<Pair<A, B>>): Pair<List<A>, List<B>> =
    list.coFoldRight(Pair(List(), List())) { pair ->
        { acc ->
            Pair(acc.first.cons(pair.first), acc.second.cons(pair.second))
        }
    }

fun main() {
    val pairs = product(List(1, 2), List(4, 5, 6)) { x -> { y -> Pair(x, y) } }
    val zipped = zipWith(List(1, 2), List(4, 5, 6)) { x -> { y -> Pair(x, y) } }
    println("Product: $pairs")
    println("ZipWith: $zipped")
}

