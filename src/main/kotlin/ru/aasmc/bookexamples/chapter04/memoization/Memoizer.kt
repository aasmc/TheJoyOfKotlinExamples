package ru.aasmc.bookexamples.chapter04.memoization

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.StreamSupport

class Memoizer<T, U> private constructor() {
    private val cache = ConcurrentHashMap<T, U>()
    private fun doMemoize(function: (T) -> U): (T) -> U =
        { input ->
            cache.computeIfAbsent(input) {
                function(it)
            }
        }

    companion object {
        fun <T, U> memoize(function: (T) -> U): (T) -> U =
            Memoizer<T, U>().doMemoize(function)
    }
}

fun longComputation(number: Int): Int {
    Thread.sleep(1000)
    return number
}

// memoizing currying functions. need to memoize all the functions
val mhc = Memoizer.memoize { x: Int ->
    Memoizer.memoize { y: Int ->
        x + y
    }
}

val f3m = Memoizer.memoize { x: Int ->
    Memoizer.memoize { y: Int ->
        Memoizer.memoize { z: Int ->
            longComputation(z) - longComputation(y) + longComputation(x)
        }
    }
}

fun main() {
    val start1 = System.currentTimeMillis()
    val result = longComputation(42)
    val end1 = System.currentTimeMillis() - start1
    val memoizedLongComputation = Memoizer.memoize(::longComputation)
    val start2 = System.currentTimeMillis()
    val result2 = memoizedLongComputation(42)
    val end2 = System.currentTimeMillis() - start2
    val start3 = System.currentTimeMillis()
    val result3 = memoizedLongComputation(42)
    val end3 = System.currentTimeMillis() - start3
    println("Call to nonmemoized function: result = $result, time = $end1")
    println("First call to memoized function: result = $result2, time = $end2")
    println("Second call to memoized function: result = $result3, time = $end3")
    println()
    println("===================================================================")
    println()
    println("Currying functions examples")
    val start4 = System.currentTimeMillis()
    val result4 = f3m(41)(42)(43)
    val time4 = System.currentTimeMillis() - start4
    val start5 = System.currentTimeMillis()
    val result5 = f3m(41)(42)(43)
    val time5 = System.currentTimeMillis() - start5
    println("First call to memoized function: result = $result4, time = $time4")
    println("Second call to memoized function: result = $result5, time = $time5")
}