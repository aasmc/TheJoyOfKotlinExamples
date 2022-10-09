package ru.aasmc.bookexamples.chapter02

import org.junit.jupiter.api.Test

internal class ResourceClosureKtTest {

    @Test
    fun fileLinesSequence_demo() {
        fileLinesSequence("src/test/resources/example.txt") {
            println(it)
        }
    }

    @Test
    fun fileForEachLine_demo() {
        fileForEachLine("src/test/resources/example.txt") {
            println(it)
        }
    }

    @Test
    fun fileUseLines_demo() {
        fileUseLines("src/test/resources/example.txt") {
            println(it)
        }
    }

}