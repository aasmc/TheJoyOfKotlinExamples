package ru.aasmc.bookexamples.chapter02

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class VarianceKtTest {
    @Test
    fun addAllVarianceDemo() {
        val ls = mutableListOf("A String")
        val la: MutableList<Any> = mutableListOf()
        addAll(la, ls)
        for (elem in la) {
            println(elem)
        }
    }
}