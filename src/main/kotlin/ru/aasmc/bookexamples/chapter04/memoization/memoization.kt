package ru.aasmc.bookexamples.chapter04.memoization

import ru.aasmc.bookexamples.chapter04.list_recursion.head
import ru.aasmc.bookexamples.chapter04.list_recursion.tail
import java.lang.StringBuilder
import java.math.BigInteger

fun fibo(limit: Int): String =
    when {
        limit < 1 -> error("Fibonacci numbers cannot be computed for negative limit: $limit")
        limit == 1 -> "1"
        else -> {
            var prev = BigInteger.ONE
            var next = BigInteger.ONE
            var fibonacci: BigInteger
            val builder = StringBuilder("1, 1")
            for (i in 2 until limit) {
                fibonacci = prev.add(next)
                builder.append(", ").append(fibonacci)
                prev = next
                next = fibonacci
            }
            builder.toString()
        }
    }

fun fiboTailRec(limit: Int): String {
    tailrec fun fibo(
        acc: List<BigInteger>,
        acc1: BigInteger,
        acc2: BigInteger,
        x: BigInteger
    ): List<BigInteger> = when (x) {
        BigInteger.ZERO -> acc
        BigInteger.ONE -> acc + (acc1 + acc2)
        else -> fibo(acc + (acc1 + acc2), acc2, acc1 + acc2, x - BigInteger.ONE)
    }

    val list = fibo(listOf(), BigInteger.ONE, BigInteger.ZERO, BigInteger.valueOf(limit.toLong()))
    return makeString(list, ", ")
}


fun <T, U> foldLeft(list: List<T>, acc: U, f: (U, T) -> U): U {
    tailrec fun foldLeft_(list: List<T>, acc: U): U =
        if (list.isEmpty()) {
            acc
        } else {
            foldLeft_(list.tail(), f(acc, list.head()))
        }
    return foldLeft_(list, acc)
}

fun <T> makeString(list: List<T>, separator: String): String =
    when {
        list.isEmpty() -> ""
        list.tail().isEmpty() -> list.head().toString()
        else -> list.head().toString() +
                foldLeft(list.tail(), "") { acc, elem -> "$acc$separator${elem.toString()}" }
    }

fun <T> iterate(
    seed: T,
    f: (T) -> T,
    n: Int
): List<T> {
    tailrec fun iterate_(
        acc: List<T>,
        seed: T
    ): List<T> =
        if (acc.size < n) {
            iterate_(acc + seed, f(seed))
        } else {
            acc
        }

    return iterate_(listOf(), seed)
}

fun <T, U> mapTailRec(list: List<T>, mapper: (T) -> U): List<U> {
    tailrec fun map_(acc: List<U>, list: List<T>): List<U> =
        if (list.isEmpty()) {
            acc
        } else {
            map_(acc + mapper(list.head()), list.tail())
        }
    return map_(listOf(), list)
}

fun <T, U> map(list: List<T>, mapper: (T) -> U): List<U> =
    foldLeft(list, listOf()) { acc, elem ->
        acc + mapper(elem)
    }

fun fiboCorecursive(limit: Int): String {
    val seed = Pair(BigInteger.ZERO, BigInteger.ONE)
    val f = { (left, right): Pair<BigInteger, BigInteger> ->
        Pair(right, left + right)
    }
    val listOfPairs = iterate(seed, f, limit + 1)
    val list = map(listOfPairs) { p -> p.first }
    return makeString(list.drop(1), ", ")
}

fun main() {
    println("Fibo iterative with memoization:")
    println(fibo(10))
    println("Fibo recursive with memoization:")
    println(fiboTailRec(10))
    println("Fibo corecursive:")
    println(fiboCorecursive(10))
}