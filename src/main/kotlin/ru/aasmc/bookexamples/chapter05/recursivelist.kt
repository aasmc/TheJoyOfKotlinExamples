package ru.aasmc.bookexamples.chapter05

sealed class List<out A> {

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

    internal object Nil : List<Nothing>() {
        override fun isEmpty(): Boolean = true

        override fun toString(): String = "[NIL]"

        override fun init(): List<Nothing> {
            error("Cannot delete last element of an empty list.")
        }
    }

    internal class Cons<out A>(
        internal val head: A,
        internal val tail: List<A>
    ) : List<A>() {

        override fun init(): List<A> = reverse().drop(1).reverse()

        override fun isEmpty(): Boolean = false

        override fun toString(): String = "[${toString("", this)}NIL]"

        private tailrec fun toString(acc: String, list: List<A>): String =
            when (list) {
                is Nil -> acc
                is Cons -> toString("$acc${list.head}, ", list.tail)
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














