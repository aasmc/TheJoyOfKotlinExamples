package ru.aasmc.bookexamples.chapter_09

import ru.aasmc.bookexamples.chapter_08.List
import kotlin.random.Random

fun main() {
    val first = Lazy {
        println("Evaluationg first")
        true
    }
    val second = Lazy<Boolean> {
        println("Evaluating second")
        throw IllegalStateException()
    }
    println(first() || second())
    println(first() || second())
    println(or(first, second))

    println("++++++++++++++++++++++++++++++++++++++++")
    println("Composing Lazy example")
    val greetings = Lazy {
        println("Evaluating greetings")
        "Hello"
    }
    val name1 = Lazy {
        println("Evaluating name1")
        "Mickey"
    }
    val name2 = Lazy {
        println("Evaluating name2")
        "Donald"
    }
    val defaultMessage = Lazy {
        println("Evaluating default message")
        "No greetings when time is odd"
    }
    val message1 = constructMessage(greetings, name1)
    val message2 = constructMessage(greetings, name2)
    val condition = Random(System.currentTimeMillis()).nextInt() % 2 == 0
    println(if (condition) message1() else defaultMessage())
    println(if (condition) message1() else defaultMessage())
    println(if (condition) message2() else defaultMessage())

    val currying1 = constructMessageCurrying(greetings)(name1)
    val currying2 = constructMessageCurrying(greetings)(name2)
    println(if (condition) currying1() else defaultMessage())
    println(if (condition) currying1() else defaultMessage())
    println(if (condition) currying2() else defaultMessage())

    println("++++++++++++++++++++++++++++++++++++++++++++++")
    println("Testing Lazy mapping")
    val greets: (String) -> String = { "Hello $it" }
    val nameMap: Lazy<String> = Lazy {
        println("Evaluating name in map")
        "Mickey"
    }
    val defaultMessageMap = Lazy {
        println("Evaluating default message in map")
        "No greetings when time is odd"
    }
    val messageMap = nameMap.map(greets)
    println(if (condition) messageMap() else defaultMessageMap())
    println(if (condition) messageMap() else defaultMessageMap())

    println("++++++++++++++++++++++++++++++++++++++++++++++")
    println("Testing Lazy flatMap")
    val fmGreetings = Lazy {
        getGreetings()
    }
    val flatGreets: (String) -> Lazy<String> = { name ->
        fmGreetings.map { greets ->
            "$greets, $name"
        }
    }

    val fmName = Lazy {
        println("Evaluating name in flatMap")
        "Mickey"
    }

    val defaultFMMessage = Lazy {
        println("evaluating default message in flatmap")
        "No greetings when the time is odd in in flatMap"
    }

    val fmMessage = fmName.flatMap(flatGreets)

    println(if (condition) fmMessage() else defaultFMMessage())
    println(if (condition) fmMessage() else defaultFMMessage())

    println("++++++++++++++++++++++++++++++++++++++++++++++")
    println("Testing Lazy composition with list")
    val lName1 = Lazy {
        println("Evaluating name1 for list")
        "Mickey"
    }
    val lName2 = Lazy {
        println("Evaluating name2 for list")
        "Donald"
    }
    val lName3 = Lazy {
        println("Evaluating name3 for list")
        "Goofy"
    }
    val list = sequence(List(lName1, lName2, lName3))
    val defaultMessageForList = "No greetings when time is odd for list"
    println(if (condition) list() else defaultMessageForList)
    println(if (condition) list() else defaultMessageForList)
}

/**
 * Stub method that may be a costly operation.
 */
fun getGreetings(): String {
    println("Getting greetings!")
    return "Greetings from GetGreetings"
}

fun or(a: Lazy<Boolean>, b: Lazy<Boolean>) = if (a()) true else b()

/**
 * Function performing a lazy concatenation of the two arguments.
 * Returns a non evaluated result, without evaluating any of its
 * parameters.
 */
fun constructMessage(greetings: Lazy<String>, name: Lazy<String>): Lazy<String> =
    Lazy {
        "${greetings()}, ${name()}"
    }

val constructMessageCurrying: (Lazy<String>) -> (Lazy<String>) -> Lazy<String> = { greeting ->
    { name ->
        Lazy {
            "${greeting()}, ${name()}"
        }
    }
}

val consMessage: (String) -> (String) -> String = { greeting ->
    { name ->
        "$greeting, $name"
    }
}




























