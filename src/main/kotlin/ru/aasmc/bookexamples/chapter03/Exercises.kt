package ru.aasmc.bookexamples.chapter03

fun compose(second: (Int) -> Int, first: (Int) -> Int): (Int) -> Int = { /* parameter of the returned function */x ->
    second(first(x))
}

fun <T, U, V> polymorphicCompose(second: (U) -> V, first: (T) -> U): (T) -> V = { t ->
    second(first(t))
}

val add: (Int) -> (Int) -> Int = { a ->
    { b ->
        a + b
    }
}

typealias IntBinOp = (Int) -> (Int) -> Int

val addBinOp: IntBinOp = { a -> { b -> a + b } }
val multBinOp: IntBinOp = { a -> { b -> a * b } }

val compose: ((Int) -> Int) -> ((Int) -> Int) -> (Int) -> Int = { second ->
    { first ->
        { argument ->
            second(first(argument))
        }
    }
}

/**
 * Create a higher order function of type: ((U) -> V) -> ((T) -> U) -> (T) -> V
 */
fun <T, U, V> higherOrderCompose(): ((U) -> V) -> ((T) -> U) -> (T) -> V = { second ->
    { first ->
        { argument ->
            second(first(argument))
        }
    }
}

/**
 * Example of anonymous functions used in HOF
 */
val cos = higherOrderCompose<Double, Double, Double>()() { x: Double -> Math.PI / 2 - x }(Math::sin)

fun <A, B, C> partialA(a: A, f: (A) -> (B) -> C): (B) -> C = f(a)

fun <A, B, C> partialB(b: B, f: (A) -> (B) -> C): (A) -> C = { a: A ->
    f(a)(b)
}

fun <A, B, C, D> curriedQuartet() = { a: A ->
    { b: B ->
        { c: C ->
            { d: D ->
                "$a, $b, $c, $d"
            }
        }
    }
}

fun <A, B, C> curriedABC(f: (A, B) -> C): (A) -> (B) -> C = { a: A ->
    { b: B ->
        f(a, b)
    }
}

val addTax: (Double) -> (Double) -> Double = { tax ->
    { price ->
        price + price / 100 * tax
    }
}

val add9PercentTax = addTax(9.0)

val identity: (Int) -> Int = { it }

fun <T, U, V> swapArgs(f: (T) -> (U) -> V): (U) -> (T) -> V = { u: U ->
    { t: T ->
        f(t)(u)
    }
}

fun main() {
    val f = partialA<Double, Int, Float>(2.0) { x ->
        { y ->
            (x + y).toFloat()
        }
    }
    println(f(9))

    val b = curriedQuartet<Int, Int, Int, Int>()
    println(b(1)(2)(3)(4))

    val price = 1000.0
    println(add9PercentTax(price))

    val addPrice = swapArgs<Double, Double, Double>(addTax)
    println(addPrice(1000.0)(9.0))
}














