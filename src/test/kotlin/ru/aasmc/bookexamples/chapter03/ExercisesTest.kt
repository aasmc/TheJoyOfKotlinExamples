package ru.aasmc.bookexamples.chapter03

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ExercisesTest {

    fun triple(x: Int): Int = x * 3
    fun square(x: Int): Int = x * x

    val square: IntUnaryOp = { it * it }
    val triple: IntUnaryOp = { it * 3 }

    @Test
    fun test_composing_function() {
        val squareOfTriple = compose(::square, ::triple)
        assertEquals(36, squareOfTriple(2))
    }

    @Test
    fun add_test() {
        assertEquals(5, add(1)(4))
    }

    @Test
    fun compose_currying_test() {
        val squareOfTriple = compose(square)(triple)
        assertEquals(36, squareOfTriple(2))
    }

    @Test
    fun hof_polymorphic_compose() {

        val squareOfTriples = higherOrderCompose<Int, Int, Int>()(square)(triple)
        assertEquals(36, squareOfTriples(2))
    }
}

typealias IntUnaryOp = (Int) -> Int