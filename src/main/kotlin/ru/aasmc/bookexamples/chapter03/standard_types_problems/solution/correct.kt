package ru.aasmc.bookexamples.chapter03.standard_types_problems.solution

@JvmInline
value class Weight private constructor(val value: Double) {
    operator fun plus(other: Weight): Weight = Weight(this.value + other.value)
    operator fun times(num: Int): Weight = Weight(this.value * num)

    companion object {
        val identity = Weight(0.0)
        operator fun invoke(value: Double) =
            if (value > 0) {
                Weight(value)
            } else {
                error("Weight cannot be 0 or less than 0!")
            }
    }
}

@JvmInline
value class Price private constructor (val value: Double) {
    operator fun plus(other: Price): Price = Price(this.value + other.value)
    operator fun times(num: Int) = Price(this.value * num)

    companion object {
        val identity = Price(0.0)
        operator fun invoke(value: Double) =
            if (value > 0) {
                Price(value)
            } else {
                error("Price cannot be 0 or less than 0!")
            }
    }
}


data class Product(val name: String, val price: Price, val weight: Weight)

data class OrderLine(val product: Product, val count: Int) {
    fun weight() = product.weight * count

    fun amount() = product.price * count
}

object Store {
    @JvmStatic
    fun main(args: Array<String>) {
        val toothPaste = Product("Tooth paste", Price(1.5), Weight(0.5))
        val toothBrush = Product("Tooth brush", Price(3.5), Weight(0.3))
        val orderLines = listOf(
            OrderLine(toothPaste, 2),
            OrderLine(toothBrush, 3)
        )
        val weight = orderLines.fold(Weight.identity) { acc, line ->
            acc + line.weight()
        }
        val price = orderLines.fold(Price.identity) { acc, line ->
            acc + line.amount()
        }

        println("Total price: $price")
        println("Total weight: $weight")
    }
}