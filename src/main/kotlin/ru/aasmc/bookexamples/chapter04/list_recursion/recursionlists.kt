package ru.aasmc.bookexamples.chapter04.list_recursion

import ru.aasmc.bookexamples.chapter04.recursion.prepend

fun <T> List<T>.head(): T =
    if (this.isEmpty()) {
        error("head called on empty list")
    } else {
        this[0]
    }

fun <T> List<T>.tail(): List<T> =
    if (this.isEmpty()) {
        error("tail called on empty list")
    } else {
        this.drop(1)
    }

fun sum(list: List<Int>): Int =
    if (list.isEmpty()) {
        0
    } else {
        list.head() + sum(list.tail())
    }

fun tailRecSum(list: List<Int>): Int {
    tailrec fun sumTail(list: List<Int>, acc: Int): Int =
        if (list.isEmpty()) {
            acc
        } else {
            sumTail(list.tail(), acc + list.head())
        }
    return sumTail(list, 0)
}

fun <T> makeString(list: List<T>, delim: String): String =
    when {
        list.isEmpty() -> ""
        list.tail().isEmpty() -> "${list.head()}${makeString(list.tail(), delim)}"
        else -> "${list.head()}$delim${makeString(list.tail(), delim)}"
    }

fun <T> tailRecMakeString(list: List<T>, delim: String): String {
    tailrec fun makeString_(list: List<T>, acc: String): String =
        when {
            list.isEmpty() -> acc
            acc.isEmpty() -> makeString_(list.tail(), "${list.head()}")
            else -> makeString_(list.tail(), "$acc$delim${list.head()}")
        }
    return makeString_(list, "")
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

fun sumFoldLeft(list: List<Int>): Int = foldLeft(list, 0, Int::plus)

fun <T> makeStringFoldLeft(list: List<T>, delim: String): String =
    foldLeft(list, "") { acc, current ->
        if (acc.isEmpty()) {
            "$current"
        } else {
            "$acc$delim$current"
        }
    }

fun toStringFoldLeft(list: List<Char>): String = foldLeft(list, "", String::plus)

fun <T, U> foldRight(list: List<T>, identity: U, f: (T, U) -> U): U =
    if (list.isEmpty()) {
        identity
    } else {
        f(list.head(), foldRight(list.tail(), identity, f))
    }


fun toStringFoldRight(list: List<Char>): String =
    foldRight(list, "") { curr, acc ->
        prepend(curr, acc)
    }

fun <T> prepend(list: List<T>, elem: T): List<T> = listOf(elem) + list

fun <T> reverse(list: List<T>): List<T> = foldLeft(list, listOf(), ::prepend)

fun rangeRecursive(start: Int, end: Int): List<Int> =
    if (end <= start) {
        listOf()
    } else {
        prepend(rangeRecursive(start + 1, end), start)
    }

fun <T> unfoldRecursive(seed: T, f: (T) -> T, p: (T) -> Boolean): List<T> =
    if (p(seed)) {
        prepend(unfoldRecursive(f(seed), f, p), seed)
    } else {
        listOf()
    }





















