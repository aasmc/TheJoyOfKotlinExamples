package ru.aasmc.bookexamples.chapter04.list_recursion

fun <T> unfold(
    seed: T,
    f: (T) -> T,
    p: (T) -> Boolean
): List<T> {
    val result = mutableListOf<T>()
    var elem = seed
    while (p(elem)) {
        result.add(elem)
        elem = f(elem)
    }
    return result
}

fun range(start: Int, end: Int): List<Int> = unfold(start, Int::inc) {
    it < end
}

fun <T> unfoldCorecursive(
    seed: T,
    f: (T) -> T,
    p: (T) -> Boolean
): List<T> {
    tailrec fun helper(
        acc: List<T>,
        seed: T
    ): List<T> =
        if (p(seed)) {
            helper(acc + seed, f(seed))
        } else {
            acc
        }

    return helper(listOf(), seed)
}