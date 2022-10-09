package ru.aasmc.bookexamples.chapter03.standard_types_problems.problem

data class Product(val name: String, val price: Double, val weight: Double)

data class OrderLine(val product: Product, val count: Int) {
    fun weight() = product.weight * count

    fun amount() = product.price * count
}

object Store {
    @JvmStatic
    fun main(args: Array<String>) {
        val toothPaste = Product("Tooth paste", 1.5, 0.5)
        val toothBrush = Product("Tooth brush", 3.5, 0.3)
        val orderLines = listOf(
            OrderLine(toothPaste, 2),
            OrderLine(toothBrush, 3)
        )
        val weight = orderLines.sumOf { it.weight() }
        val price = orderLines.sumOf { it.amount() }

        println("Total price: $price")
        println("Total weight: $weight")
    }
}