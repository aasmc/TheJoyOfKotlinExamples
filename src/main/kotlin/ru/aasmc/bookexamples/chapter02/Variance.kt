package ru.aasmc.bookexamples.chapter02

fun <T> addAll(to: MutableList<T>, from: MutableList<out T>) {
    for (elem in from) {
        to.add(elem)
    }
}

interface Bag<T> {
    fun get(): T
    fun use(t: T) : Boolean
}

open class MyClassParent

class MyClass : MyClassParent()

class BagImpl: Bag<MyClassParent> {
    override fun get(): MyClassParent = MyClassParent()

    override fun use(t: MyClassParent): Boolean = true
}

/**
 * Example of use-site variance
 */
fun useBag(bag: Bag<in MyClass>): Boolean {
    // do smth with bag
    return true
}

val bag = useBag(BagImpl())

class BagImpl2 : Bag<MyClass> {
    override fun get(): MyClass {
        return MyClass()
    }

    override fun use(t: MyClass): Boolean {
        return true
    }
}

fun createBag(): Bag<out MyClassParent> = BagImpl2()