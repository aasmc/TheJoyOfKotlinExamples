package ru.aasmc.bookexamples.chapter_06

import ru.aasmc.bookexamples.chapter05.List

data class Toon(
    val firstName: String,
    val lastName: String,
    val email: Option<String> = Option()
) {
    companion object {
        operator fun invoke(
            firstName: String,
            lastName: String,
            email: String? = null
        ) = Toon(firstName, lastName, Option(email))
    }
}

fun <K, V> Map<K, V>.getOption(key: K) =
    Option(this[key])

fun main() {
    val toons: Map<String, Toon> = mapOf(
        "Mickey" to Toon("Mickey", "Mouse", "mickey@email.com"),
        "Minnie" to Toon("Minnie", "Mouse"),
        "Donald" to Toon("Donald", "Duck", "donald@email.com"),
    )

    val mickey = toons.getOption("Mickey").flatMap { it.email }
    val minnie = toons.getOption("Minnie").flatMap { it.email }
    val goofy = toons.getOption("Goofy").flatMap { it.email }
    println(mickey.getOrElse { "No data" })
    println(minnie.getOrElse { "No data" })
    println(goofy.getOrElse { "No data" })

    val parse16 = hLift(parseHex)
    val list = List("4", "5", "6", "7", "8", "9", "A", "B")
    val result = sequence(list.map(parse16))
    println(result)

    val res2 = traverse(list, parse16)
    println("Result 2:")
    println(res2)
}