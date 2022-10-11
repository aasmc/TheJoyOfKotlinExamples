package ru.aasmc.bookexamples.chapter04.recursion

fun prepend(c: Char, s: String) = "$c$s"

fun toStringRec(list: List<Char>): String =
    if (list.isEmpty()) {
        ""
    } else {
        prepend(list.first(), toStringRec(list.drop(1)))
    }

fun factorial(n: Int): Int = if (n == 0) 1 else n * factorial(n - 1)

object Factorial {
    private lateinit var fact: (Int) -> Int

    init {
        fact = { n -> if (n <= 1) n else n * fact(n - 1) }
    }

    private val factorial = fact
}