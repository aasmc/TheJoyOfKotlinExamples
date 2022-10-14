package ru.aasmc.bookexamples.chapter_06

import kotlin.math.pow
import ru.aasmc.bookexamples.chapter05.List

sealed class Option<out A> {

    abstract fun isEmpty(): Boolean

    fun getOrElse(default: () -> @UnsafeVariance A) = when (this) {
        None -> default()
        is Some -> value
    }

    fun <B> map(f: (A) -> B): Option<B> = when (this) {
        None -> None
        is Some -> Some(f(value))
    }

    fun <B> flatMapEasy(f: (A) -> Option<B>): Option<B> = when (this) {
        None -> None
        is Some -> f(value)
    }

    fun <B> flatMap(f: (A) -> Option<B>): Option<B> = map(f).getOrElse { None }

    fun orElseSimple(default: () -> Option<@UnsafeVariance A>): Option<A> = when (this) {
        None -> default()
        is Some -> this
    }

    fun orElse(default: () -> Option<@UnsafeVariance A>): Option<A> =
        map { this }.getOrElse(default)

    fun filter(p: (A) -> Boolean): Option<A> = flatMap { cur ->
        if (p(cur)) this else None
    }

    internal object None : Option<Nothing>() {
        override fun isEmpty(): Boolean = true

        override fun toString(): String = "None"

        override fun equals(other: Any?): Boolean = other === None

        override fun hashCode(): Int = 0
    }

    internal data class Some<out A>(
        internal val value: A
    ) : Option<A>() {
        override fun isEmpty(): Boolean = false
    }

    companion object {
        operator fun <A> invoke(a: A? = null): Option<A> =
            when (a) {
                null -> None
                else -> Some(a)
            }
    }
}

val mean: (kotlin.collections.List<Double>) -> Option<Double> = { list ->
    when {
        list.isEmpty() -> Option()
        else -> Option(list.sum() / list.size)
    }
}

/**
 * Value function to compute variance of a given series of values around
 * the mean value in the series. The variance of a series is the mean of
 * Math.pow(x - m, 2) for each element x in the series, where m is the mean of
 * the series.
 */
val variance: (kotlin.collections.List<Double>) -> Option<Double> = { list ->
    // get the mean of the list
    mean(list).flatMap { m ->
        // get the mean of the variance
        mean(list.map { x ->
            // compute the variance
            2.0.pow(x - m)
        })
    }
}

fun mean(list: kotlin.collections.List<Double>): Option<Double> =
    when {
        list.isEmpty() -> Option()
        else -> Option(list.sum() / list.size)
    }

fun variance(list: kotlin.collections.List<Double>): Option<Double> =
    mean(list).flatMap { m ->
        mean(list.map { x ->
            2.0.pow(m - x)
        })
    }

fun <A, B> lift(f: (A) -> B): (Option<A>) -> Option<B> = { curOpt ->
    try {
        curOpt.map(f)
    } catch (e: Exception) {
        Option() // BAD. we lose the exception
    }
}

fun <A, B> hLift(f: (A) -> B): (A) -> Option<B> = { value ->
    try {
        Option(value).map(f)
    } catch (e: Exception) {
        Option() // BAD. we lose the exception
    }
}

val parseWithRadix: (Int) -> (String) -> Int = { radix ->
    { str ->
        Integer.parseInt(str, radix)
    }
}

val parseHex: (String) -> Int = parseWithRadix(16)
val parseDecimal: (String) -> Int = parseWithRadix(10)
val parseOctal: (String) -> Int = parseWithRadix(8)
val parseBinary: (String) -> Int = parseWithRadix(2)

fun <A, B, C> map2(
    oa: Option<A>,
    ob: Option<B>,
    f: (A) -> (B) -> C
): Option<C> = oa.flatMap { a ->
    ob.map { b ->
        f(a)(b)
    }
}

fun <A, B, C, D> map3(
    oa: Option<A>,
    ob: Option<B>,
    oc: Option<C>,
    f: (A) -> (B) -> (C) -> D
): Option<D> = oa.flatMap { a ->
    ob.flatMap { b ->
        oc.map { c ->
            f(a)(b)(c)
        }
    }
}

/**
 * Converts the given [list] of options into an
 * [Option] of list if all elements of the given list are all [Option.Some],
 * if at least one element in the given list is [Option.None] ,
 * then it returns [Option.None].
 */
fun <A> sequence(list: List<Option<A>>): Option<List<A>> =
    traverse(list) { it }

fun <A, B> traverse(list: List<A>, f: (A) -> Option<B>): Option<List<B>> =
    list.foldRight(Option(List())) { a ->
        { olb ->
            map2(f(a), olb) { b ->
                { result ->
                    result.cons(b)
                }
            }
        }
    }















