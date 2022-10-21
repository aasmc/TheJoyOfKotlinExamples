package ru.aasmc.bookexamples.chapter_10

import ru.aasmc.bookexamples.StackSafe
import ru.aasmc.bookexamples.chapter_07.Result
import ru.aasmc.bookexamples.chapter_08.List
import kotlin.math.max

/**
 * Represents a binary search tree of elements of type [A] which are also
 * [Comparable]. Since [Comparable] uses [A] in the 'in' position,
 * making it contravariant, we either have to cast Empty tree to Tree<A>
 * or add @UnsafeVariance annotation.
 *
 * The tree is immutable. Each time we add a new node to the tree,
 * a new tree is created with the added value, leaving the original
 * tree untouched.
 *
 * The tree allows no duplicates. All values smaller than the root
 * are in the left subtree, higher than the root are in the right
 * subtree.
 *
 * The tree is unbalanced.
 */
sealed class Tree<out A : Comparable<@UnsafeVariance A>> {

    abstract fun isEmpty(): Boolean

    operator fun plus(element: @UnsafeVariance A): Tree<A> = when (this) {
        Empty -> T(left = Empty, value = element, right = Empty)
        is T -> {
            when {
                element < value -> T(left = left + element, value = this.value, right = this.right)
                element > value -> T(left = this.left, value = this.value, right = right + element)
                // here we make sure that if A has some properties that are not
                // participating in Comparable.compareTo() different to the new element
                // then the new element is inserted in the tree instead of the old one
                // although the comparison showed that they are equal.
                // We could implement this branch by simply returning 'this' without the above consideration.
                else -> T(left = this.left, value = element, right = this.right)
            }
        }
    }

    @StackSafe
    operator fun <A : Comparable<A>> invoke(vararg az: A): Tree<A> =
        az.foldRight(Empty) { elem: A, acc: Tree<A> ->
            acc.plus(elem)
        }

    @StackSafe
    operator fun <A : Comparable<A>> invoke(az: List<A>): Tree<A> =
        az.foldLeft(Empty) { acc: Tree<A> ->
            { elem ->
                acc.plus(elem)
            }
        }

    fun contains(a: @UnsafeVariance A): Boolean = when (this) {
        Empty -> false
        is T -> when {
            a < value -> left.contains(a)
            a > value -> right.contains(a)
            else -> value == a
        }
    }

    /**
     * Represents the number of elements in the tree.
     */
    abstract val size: Int

    /**
     * Represents the number of segments on the longest path
     * from root of the tree to the leaf. I.e. the height of
     * the tree is the number of nodes on the longest path from
     * the root to the leaf - 1 (root).
     */
    abstract val height: Int

    abstract fun min(): Result<A>

    abstract fun max(): Result<A>

    fun remove(a: @UnsafeVariance A): Tree<A> = when (this) {
        Empty -> this
        is T -> when {
            a < value -> T(left = left.remove(a), value = value, right = right)
            a > value -> T(left = left, value = value, right = right.remove(a))
            else -> left.removeMerge(right) // a == value, merge left and right branches
        }
    }

    /**
     * Merges two this tree with [other] tree. If this tree is Empty,
     * returns [other] tree. If [other] tree is Empty returns this tree.
     * Otherwise compares values in this root and other root. If this value
     * is greater than other.value then proceeds to merge other tree in the
     * left subtree, otherwise does the same in the right subtree.
     * Since we know that this function is invoked from Tree.remove(a) method
     * when a == root.value, we don't compare for equality in this method.
     * I.e. all values in one of the trees are strictly lower that values
     * in the other tree.
     */
    fun removeMerge(other: Tree<@UnsafeVariance A>): Tree<A> = when (this) {
        Empty -> other
        is T -> when (other) {
            Empty -> this
            is T -> when {
                other.value < value -> T(left = left.removeMerge(other), value = value, right = right)
                else -> T(left = left, value = value, right = right.removeMerge(other))
            }
        }
    }

    internal object Empty : Tree<Nothing>() {

        override fun isEmpty(): Boolean = true

        override fun toString(): String = "E"

        override val size: Int = 0

        override val height: Int = -1

        override fun min(): Result<Nothing> = Result.empty()

        override fun max(): Result<Nothing> = Result.empty()
    }

    internal class T<out A : Comparable<@UnsafeVariance A>>(
        internal val left: Tree<A>,
        internal val value: A,
        internal val right: Tree<A>
    ) : Tree<A>() {
        override fun isEmpty(): Boolean = false

        override fun toString(): String = "(T $left $value $right)"

        override val size: Int = left.size + right.size + 1

        override val height: Int = 1 + max(left.height, right.height)

        override fun min(): Result<A> = left.min().orElse { Result(value) }

        override fun max(): Result<A> = right.max().orElse { Result(value) }
    }

    companion object {
        operator fun <A : Comparable<A>> invoke(): Tree<A> = Empty
    }

}


































