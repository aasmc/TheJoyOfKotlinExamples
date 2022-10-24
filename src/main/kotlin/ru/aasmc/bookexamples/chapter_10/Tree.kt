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

    /**
     * Removes an element [a] from the tree and returns a new
     * tree without the element. If [a] is not present in the tree
     * then returns the same tree unchanged.
     */
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

    abstract fun merge(other: Tree<@UnsafeVariance A>): Tree<A>

    abstract fun <B> foldLeft(
        identity: B,
        f: (B) -> (A) -> B,
        g: (B) -> (B) -> B
    ): B

    abstract fun <B> foldInOrder(identity: B, f: (B) -> (A) -> (B) -> B): B

    abstract fun <B> foldPreOrder(identity: B, f: (A) -> (B) -> (B) -> B): B

    abstract fun <B> foldPostOrder(identity: B, f: (B) -> (B) -> (A) -> B): B

    fun <B : Comparable<B>> map(f: (A) -> B): Tree<B> =
        foldInOrder(Empty) { t1: Tree<B> ->
            { i: A ->
                { t2: Tree<B> ->
                    Tree(t1, f(i), t2)
                }
            }
        }

    protected abstract fun rotateRight(): Tree<A>

    protected abstract fun rotateLeft(): Tree<A>

    /**
     * Converts this tree into an in-order list from right to left.
     */
    fun toListInOrderRight(): List<A> = unBalanceRight(List(), this)

    internal object Empty : Tree<Nothing>() {

        override fun isEmpty(): Boolean = true

        override fun toString(): String = "E"

        override val size: Int = 0

        override val height: Int = -1

        override fun min(): Result<Nothing> = Result.empty()

        override fun max(): Result<Nothing> = Result.empty()

        override fun merge(other: Tree<Nothing>): Tree<Nothing> = other

        override fun <B> foldLeft(identity: B, f: (B) -> (Nothing) -> B, g: (B) -> (B) -> B): B = identity

        override fun <B> foldInOrder(identity: B, f: (B) -> (Nothing) -> (B) -> B): B = identity

        override fun <B> foldPreOrder(identity: B, f: (Nothing) -> (B) -> (B) -> B): B = identity

        override fun <B> foldPostOrder(identity: B, f: (B) -> (B) -> (Nothing) -> B): B = identity

        override fun rotateRight(): Tree<Nothing> = this

        override fun rotateLeft(): Tree<Nothing> = this
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

        /**
         * Merges this tree (the tree in which the function is defined)
         * with the [other] tree according to the following algorithm:
         *  - if the parameter tree is empty, return this
         *  - if the root of the parameter is higher than this root,
         *    remove the left branch of the parameter tree and merge
         *    the result with this right branch. Then merge the result
         *    with the parameter's left branch.
         *  - if the root of the parameter is lower than this root, remove
         *    the right branch of the parameter tree and merge the result with
         *    this left branch. Then merge the result with the parameter's
         *    right branch.
         *  - if the root of the parameter is equal to this root, merge the
         *    left branch of the parameter with this left branch and merge
         *    the right branch of the parameter with this right branch.
         *
         *  This implementation doesn't change this root's value, if it is equal
         *  to the other's root value.
         *
         *  No balancing of the resulting tree occurs.
         */
        override fun merge(other: Tree<@UnsafeVariance A>): Tree<A> = when (other) {
            Empty -> this
            is T -> when {
                other.value > this.value -> T(
                    left = left,
                    value = value,
                    right = right.merge( // merge with this right tree
                        T(
                            left = Empty, // remove the left branch of the other tree
                            value = other.value,
                            right = other.right
                        )
                    )
                ).merge(other.left) // merge with the other's left tree
                other.value < this.value -> T(
                    left = left.merge(
                        T(
                            left = other.left,
                            value = other.value,
                            right = Empty
                        )
                    ),
                    value = value,
                    right = right
                ).merge(other.right)
                else -> T(
                    left = left.merge(other.left),
                    value = value,
                    right = right.merge(other.right)
                )
            }
        }

        /**
         * Folds this list and returns the result of folding which is of type [B].
         * To fold we use two functions:
         *  - [f] which is the folding function for left and right branches
         *  - [g] which is used to merge the result of folding with the root
         *
         * The algorithm:
         *  - recursively fold the left branch and the right branch, giving two [B] values
         *  - combine these two [B] values with the [g] function, and then combine the result with the root
         *    and return the result.
         *
         * This function doesn't give a client a predictable result, because there are
         * many traversal orders and the client doesn't know which one is used here.
         */
        override fun <B> foldLeft(
            identity: B,
            f: (B) -> (A) -> B,
            g: (B) -> (B) -> B
        ): B = g(
            right.foldLeft(identity, f, g)
        )(
            f(left.foldLeft(identity, f, g))(this.value)
        )

        override fun <B> foldInOrder(identity: B, f: (B) -> (A) -> (B) -> B): B =
            f(left.foldInOrder(identity, f))(value)(right.foldInOrder(identity, f))

        override fun <B> foldPreOrder(identity: B, f: (A) -> (B) -> (B) -> B): B =
            f(value)(left.foldPreOrder(identity, f))(right.foldPreOrder(identity, f))

        override fun <B> foldPostOrder(identity: B, f: (B) -> (B) -> (A) -> B): B =
            f(left.foldPostOrder(identity, f))(right.foldPostOrder(identity, f))(value)

        /**
         * Algorithm for right rotation:
         * - test the left branch for emptyness
         * - if the left branch is empty, return this because rotating right consists
         *   in promoting the left element to root.
         * - if the left element is not empty, it becomes the root, so a new T is
         *   created with left.value as the root. The left branch of the left element
         *   becomes the left branch of the new tree. For the right branch, construct a new tree with
         *   the original root as the root, the right branch of the original left as the left
         *   branch, and the original right as the right branch.
         */
        override fun rotateRight(): Tree<A> = when (left) {
            Empty -> this
            is T -> T(
                left = left.left,
                value = left.value, // new root of the tree
                right = T(
                    left = left.right, // right value of the previous left branch
                    value = value, // old root
                    right = right // current right
                )
            )
        }

        /**
         * Symmetrical to right rotation.
         */
        override fun rotateLeft(): Tree<A> = when (right) {
            Empty -> this
            is T -> T(
                left = T(
                    left = left, // current left
                    value = value, // current root
                    right = right.left // left value of the right tree
                ),
                value = right.value, // new root
                right = right.right,
            )
        }

    }

    companion object {
        operator fun <A : Comparable<A>> invoke(): Tree<A> = Empty

        operator fun <A : Comparable<A>> invoke(vararg az: A): Tree<A> =
            az.foldRight(Empty) { a: A, tree: Tree<A> -> tree.plus(a) }

        operator fun <A : Comparable<A>> invoke(list: List<A>): Tree<A> =
            list.foldLeft(Empty as Tree<A>) { tree: Tree<A> -> { a: A -> tree.plus(a) } }

        fun <A : Comparable<A>> lt(first: A, second: A): Boolean = first < second

        fun <A : Comparable<A>> lt(first: A, second: A, third: A): Boolean =
            lt(first, second) && lt(second, third)

        /**
         * Checks whether the two given trees are ordered, i.e. max value of the left tree
         * is lower than the root of the right tree and the min value of the right tree
         * is higher than the root of the left tree.
         */
        fun <A : Comparable<A>> ordered(
            left: Tree<A>,
            a: A,
            right: Tree<A>
        ): Boolean =
            (left.max().flatMap { lMax ->
                right.min().map { rMin ->
                    lt(lMax, a, rMin)
                }
            }.getOrElse(left.isEmpty() && right.isEmpty()) ||
                    left.min().mapEmpty() // ensure left tree is empty
                        .flatMap {
                            right.min().map { rmin ->
                                lt(a, rmin)
                            }
                        }.getOrElse(false) ||
                    right.min()
                        .mapEmpty() // ensure right tree is empty
                        .flatMap {
                            left.max().map { lMax ->
                                lt(lMax, a)
                            }
                        }.getOrElse(false))

        /**
         * Combines two trees and a given root value and creates a new tree.
         */
        operator fun <A : Comparable<A>> invoke(
            left: Tree<A>,
            a: A,
            right: Tree<A>
        ): Tree<A> = when {
            ordered(left, a, right) -> T(left, a, right)
            ordered(right, a, left) -> T(right, a, left)
            else -> Tree(a).merge(left).merge(right)
        }

        /**
         * This function rotates the tree to the right until the left
         * branch is empty. Then it calls itself recursively to do the same
         * thing to all the right subtrees, after having added the tree value to
         * accumulator list. Eventually the tree parameter is found empty and
         * the function returns the list accumulator.
         */
        private tailrec fun <A : Comparable<A>> unBalanceRight(
            acc: List<A>,
            tree: Tree<A>
        ): List<A> = when (tree) {
            Empty -> acc
            is T -> when (tree.left) {
                // adds the tree to the accumulator list
                Empty -> unBalanceRight(acc.cons(tree.value), tree.right)
                // rotates the tree until the left branch is empty
                is T -> unBalanceRight(acc, tree.rotateRight())
            }
        }
    }

}

fun log2nlz(n: Int): Int = when (n) {
    0 -> 0
    else -> 31 - Integer.numberOfLeadingZeros(n)
}
























