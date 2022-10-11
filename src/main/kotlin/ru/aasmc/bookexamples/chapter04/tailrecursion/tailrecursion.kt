package ru.aasmc.bookexamples.chapter04.tailrecursion

import ru.aasmc.bookexamples.chapter04.corecursion.append
import java.math.BigInteger

fun toStringTailRec(list: List<Char>): String {
    tailrec fun toString(list: List<Char>, s: String): String =
        if (list.isEmpty()) {
            s
        } else {
            toString(list.drop(1), append(s, list.first()))
        }

    return toString(list, "")
}

/**
 * Uses tail recursion to compute the sum of integers from 0 until [n].
 */
fun sum(n: Int): Int {
    tailrec fun sum(sum: Int, idx: Int): Int =
        if (idx < 1) {
            sum
        } else {
            sum(sum + idx, idx - 1)
        }
    return sum(0, n)
}

fun sum2(n: Int): Int {
    tailrec fun sum(s: Int, i: Int): Int = if (i > n) s else sum(s + i, i + 1)
    return sum(0, 0)
}

fun inc(n: Int) = n + 1
fun dec(n: Int) = n - 1

tailrec fun add(a: Int, b: Int): Int {
    return if (a == 0) {
        b
    } else {
        add(dec(a), inc(b))
    }
}

fun fibonacci(num: Int): BigInteger {
    tailrec fun fib(num: BigInteger, val1: BigInteger, val2: BigInteger): BigInteger {
        return when (num) {
            BigInteger.ZERO -> BigInteger.ONE
            BigInteger.ONE -> val1 + val2
            else -> fib(num - BigInteger.ONE, val2, val1 + val2)
        }
    }
    return fib(BigInteger.valueOf(num.toLong()), BigInteger.ZERO, BigInteger.ONE)
}































