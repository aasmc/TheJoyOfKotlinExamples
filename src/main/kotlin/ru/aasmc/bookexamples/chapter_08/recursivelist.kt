package ru.aasmc.bookexamples.chapter_08

import ru.aasmc.bookexamples.chapter_06.Option
import ru.aasmc.bookexamples.chapter_07.Result
import ru.aasmc.bookexamples.chapter_07.map2
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import kotlin.RuntimeException

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

    fun <A1, A2> unzip(f: (A) -> Pair<A1, A2>): Pair<List<A1>, List<A2>> =
        this.coFoldRight(Pair(Nil, Nil)) { elem ->
            { pair: Pair<List<A1>, List<A2>> ->
                f(elem).let {
                    Pair(pair.first.cons(it.first), pair.second.cons(it.second))
                }
            }
        }

    fun getAt(index: Int): Result<A> {
        tailrec fun getAt_(list: List<A>, index: Int): Result<A> = when (list) {
            Nil -> Result.failure<A>("Dead code. Should never execute")
            is Cons -> {
                if (index == 0) {
                    Result(list.head)
                } else {
                    getAt_(list.tail, index - 1)
                }
            }
        }
        return if (index < 0 || index > length) {
            Result.failure("Index out of bounds")
        } else {
            getAt_(this, index)
        }
    }

    fun getAtViaFoldLeft(index: Int): Result<A> =
        Pair(Result.failure<A>("Index out of bounds"), index).let {
            if (index < 0 || index >= length) {
                it
            } else {
                foldLeft(it) { ta ->
                    { a ->
                        if (ta.second < 0) {
                            ta
                        } else {
                            Pair(Result(a), ta.second - 1)
                        }
                    }
                }
            }
        }.first

    fun getAtViaFoldLeftTerminating(index: Int): Result<A> {
        data class Pair<out A>(val first: Result<A>, val second: Int) {
            override fun equals(other: Any?): Boolean = when {
                other == null -> false
                other.javaClass == this.javaClass ->
                    (other as Pair<A>).second == second
                else -> false
            }

            override fun hashCode(): Int {
                return first.hashCode() + second.hashCode();
            }
        }
        return Pair<A>(Result.failure("Index out of bounds"), index).let { identity ->
            Pair<A>(Result.failure("Index out of bounds"), -1).let { zero ->
                if (index < 0 || index >= length) {
                    identity
                } else {
                    foldLeftTerminating(identity, zero) { ta: Pair<A> ->
                        { a: A ->
                            if (ta.second < 0) {
                                ta
                            } else {
                                Pair(Result(a), ta.second - 1)
                            }
                        }
                    }
                }
            }
        }.first
    }

    abstract fun <B> foldLeftTerminating(identity: B, zero: B, f: (B) -> (A) -> B): B

    /**
     * Returns two lists by splitting current list at position [index].
     * Index below 0 is treated as 0, index above list.length is treated as the max value for the index.
     */
    fun splitAt(index: Int): Pair<List<A>, List<A>> {
        tailrec fun splitAt(acc: List<A>, list: List<A>, i: Int): Pair<List<A>, List<A>> =
            when (list) {
                Nil -> Pair(list.reverse(), acc)
                is Cons -> if (i == 0) {
                    Pair(list.reverse(), acc)
                } else {
                    splitAt(acc.cons(list.head), list.tail, i - 1)
                }
            }

        return when {
            index < 0 -> splitAt(0)
            index > length -> splitAt(length)
            else -> splitAt(Nil, this.reverse(), this.length - index)
        }
    }

    fun splitAtViaFoldLeft(index: Int): Pair<List<A>, List<A>> {
        val idx = if (index < 0) {
            0
        } else if (index >= length) {
            length
        } else {
            index
        }
        // Holder that stores info about left list, right list and current index
        val identity = Triple(Nil, Nil, idx)
        val result = foldLeft(identity) { triple: Triple<List<A>, List<A>, Int> -> // accumulator
            { elem -> // for each element in the list
                if (triple.third == 0) { // if reached the end of the first list, begin forming the second list
                    Triple(triple.first, triple.second.cons(elem), triple.third)
                } else {
                    // if not reached the end of the first list, then add current element to the first list
                    Triple(triple.first.cons(elem), triple.second, triple.third - 1)
                }
            }
        }
        // since we can add only at the head of the list, then we need to reverse both lists
        // this function is not ideal in terms of time complexity
        return Pair(result.first.reverse(), result.second.reverse())
    }

    abstract fun <B> foldLeftAndReturnRemaining(identity: B, zero: B, f: (B) -> (A) -> B): Pair<B, List<A>>

    fun splitAtViaFoldLeftAndReturnRemaining(index: Int): Pair<List<A>, List<A>> {
        data class Holder<out A>(val first: List<A>, val second: Int) {
            // explicitly make equals compare only indices (second values in the holder)
            override fun equals(other: Any?): Boolean = when {
                other == null -> false
                other.javaClass == this.javaClass -> (other as Holder<A>).second == second
                else -> false
            }

            override fun hashCode(): Int = first.hashCode() + second.hashCode()
        }

        return when {
            index <= 0 -> Pair(Nil, this)
            index >= length -> Pair(this, Nil)
            else -> {
                val identity = Holder(Nil as List<A>, -1)
                val zero = Holder(this, index)
                val (holder, list) = this.foldLeftAndReturnRemaining(identity, zero) { acc ->
                    { elem ->
                        Holder(acc.first.cons(elem), acc.second + 1)
                    }
                }
                Pair(holder.first.reverse(), list)
            }
        }
    }

    fun hasSubList(sub: List<@UnsafeVariance A>): Boolean {
        tailrec fun helper(list: List<A>, sub: List<A>): Boolean =
            when (list) {
                Nil -> sub.isEmpty()
                is Cons -> {
                    if (list.startsWith(sub)) {
                        true
                    } else {
                        helper(list.tail, sub)
                    }
                }
            }
        return helper(this, sub)
    }

    /**
     * Checks whether this list starts with a given sub list. If the sub list
     * is an empty list (Nil) it will return true.
     */
    fun startsWith(sub: List<@UnsafeVariance A>): Boolean {
        tailrec fun helper(list: List<A>, sub: List<A>): Boolean =
            when (sub) {
                Nil -> true
                is Cons -> {
                    when (list) {
                        Nil -> false
                        is Cons -> {
                            if (list.head == sub.head) {
                                helper(list.tail, sub.tail)
                            } else {
                                false
                            }
                        }
                    }
                }
            }
        return helper(this, sub)
    }

    /**
     * Returns a Map where keys are the results of applying [f] to
     * each element of the list, and values are lists of elements corresponding to each key.
     */
    // to ensure that the order of elements is preserved in the sublists we need to
    // reverse the current list
    fun <B> groupBy(f: (A) -> B): Map<B, List<A>> =
        reverse().foldLeft(mapOf()) { acc: Map<B, List<A>> ->
            { elem ->
                val key = f(elem)
                acc + (key to (acc[key] ?: Nil).cons(elem))
            }
        }

    /**
     * Checks if at least one element in the list satisfies the condition
     * represented by the predicate [p].
     */
    fun exists(p: (A) -> Boolean): Boolean =
        foldLeftAndReturnRemaining(false, true) { x ->
            { y: A ->
                x || p(y)
            }
        }.first

    /**
     * Checks if all elements in the list satisfy the condition
     * represented by the predicate [p].
     */
    fun forAll(p: (A) -> Boolean): Boolean =
        foldLeftAndReturnRemaining(true, false) { x -> { elem -> x && p(elem) } }.first

    fun <B> parallelFoldLeft(
        es: ExecutorService,
        identity: B,
        f: (B) -> (A) -> B,
        m: (B) -> (B) -> B
    ): Result<B> = try {
        // divide current list into 1024 sublists
        val result: List<B> = divide(1024).map { list: List<A> -> // for each sublist
            // submit a task to the executor service
            // the task is to fold the sublist with the given identity and computing function
            es.submit<B> { list.foldLeft(identity, f) }
        }.map<B> { fb -> // for each resulting Future object returned from the Executor service
            try {
                fb.get() // try to get the computational result
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            }
        }
        // fold the resulting list and return Success
        Result(result.foldLeft(identity, m))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Divides this list into a number of sublists. The list is divided in two
     * and each sublist recursively is divided in two, with the depth parameter
     * representing the number of recursive steps.
     */
    fun divide(depth: Int): List<List<A>> {
        tailrec fun helper(list: List<List<A>>, depth: Int): List<List<A>> =
            when (list) {
                Nil -> list // dead code
                is Cons -> {
                    if (list.head.length < 2 || depth < 1) {
                        list
                    } else {
                        helper(list.flatMap { x -> x.splitListAt(x.length / 2) }, depth - 1)
                    }
                }
            }
        return if (this.isEmpty()) {
            List(this)
        } else {
            helper(List(this), depth)
        }
    }

    /**
     * Splists the current list into sublists at the given index
     * and returns the list of sublists.
     */
    fun splitListAt(index: Int): List<List<A>> {
        tailrec fun helper(acc: List<A>, list: List<A>, i: Int): List<List<A>> =
            when (list) {
                Nil -> List(list.reverse(), acc)
                is Cons -> {
                    if (i == 0) {
                        List(list.reverse(), acc)
                    } else {
                        helper(acc.cons(list.head), list.tail, i - 1)
                    }
                }
            }
        return when {
            index < 0 -> splitListAt(0)
            index > length -> splitListAt(length)
            else -> {
                helper(Nil, this.reverse(), this.length - index)
            }
        }
    }

    fun <B> parallelMap(es: ExecutorService, g: (A) -> B): Result<List<B>> =
        try {
            val result = this.map { elem -> // for each element in the list
                // submit to executor service a lambda that will map current element to B
                es.submit<B> { g(elem) }
            }.map<B> { future -> // for each resulting Future
                try {
                    // try to get the result
                    future.get()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                } catch (e: ExecutionException) {
                    throw RuntimeException(e)
                }
            }
            Result(result)
        } catch (e: Exception) {
            Result.failure(e)
        }

    internal object Nil : List<Nothing>() {

        override val length: Int = 0

        override fun isEmpty(): Boolean = true

        override fun toString(): String = "[NIL]"

        override fun init(): List<Nothing> {
            error("Cannot delete last element of an empty list.")
        }

        override fun headSafe(): Result<Nothing> = Result()

        override fun <B> foldLeftTerminating(identity: B, zero: B, f: (B) -> (Nothing) -> B): B =
            identity

        override fun <B> foldLeftAndReturnRemaining(
            identity: B,
            zero: B,
            f: (B) -> (Nothing) -> B
        ): Pair<B, List<Nothing>> = Pair(identity, Nil)
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

        override fun <B> foldLeftTerminating(identity: B, zero: B, f: (B) -> (A) -> B): B {
            fun foldLeftTerminating_(acc: B, zero: B, list: List<A>, f: (B) -> (A) -> B): B = when (list) {
                Nil -> acc
                is Cons -> if (acc == zero) {
                    acc
                } else {
                    foldLeftTerminating_(f(acc)(list.head), zero, list.tail, f)
                }
            }
            return foldLeftTerminating_(identity, zero, this, f)
        }

        override fun <B> foldLeftAndReturnRemaining(
            identity: B,
            zero: B,
            f: (B) -> (A) -> B
        ): Pair<B, List<A>> {
            fun <B> helper(acc: B, zero: B, list: List<A>, f: (B) -> (A) -> B): Pair<B, List<A>> =
                when (list) {
                    Nil -> Pair(acc, list)
                    is Cons -> if (acc == zero) {
                        Pair(acc, list)
                    } else {
                        helper(f(acc)(list.head), zero, list.tail, f)
                    }
                }
            return helper(identity, zero, this, f)
        }
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
    list.unzip { it }

/**
 * Non stack-safe function that accepts a mapper from [S] to [Option<Pair<A, S>>]
 * and produces a List<A> by successively applying f to the S value as long as the result
 * is Option.Some.
 */
fun <A, S> unfoldUnsafe(start: S, f: (S) -> Option<Pair<A, S>>): List<A> =
    f(start).map { x ->
        unfoldUnsafe(x.second, f).cons(x.first)
    }.getOrElse { List.Nil }

fun <A, S> unfoldCorecursive(start: S, getNext: (S) -> Option<Pair<A, S>>): List<A> {
    tailrec fun helper(acc: List<A>, s: S): List<A> {
        val next = getNext(s)
        return when (next) {
            Option.None -> acc
            is Option.Some -> {
                helper(acc.cons(next.value.first), next.value.second)
            }
        }
    }
    return helper(List.Nil, start).reverse()
}

fun range(start: Int, endExclusive: Int): List<Int> {
    return unfoldCorecursive(start) { i ->
        if (i < endExclusive) {
            Option(Pair(i, i + 1))
        } else {
            Option()
        }
    }
}


fun main() {
    val pairs = product(List(1, 2), List(4, 5, 6)) { x -> { y -> Pair(x, y) } }
    val zipped = zipWith(List(1, 2), List(4, 5, 6)) { x -> { y -> Pair(x, y) } }
    println("Product: $pairs")
    println("ZipWith: $zipped")

    println("****************************************")
    println("Example of using unfold")
    val f: (Int) -> Option<Pair<Int, Int>> = { num ->
        if (num < 10_000) Option(Pair(num, num + 1)) else Option()
    }
    val result = unfoldCorecursive(0, f)
    println(result)
}


