package app.interactiveschemainferrer.util


typealias IntConstraint = Constraint<Int>

class Constraint<T> where T : Comparable<T>, T : Number {
    lateinit var min: T
        private set
    lateinit var max: T
        private set


    constructor(initial: T) {
        min = initial
        max = initial
    }

    constructor(initialMin: T, initialMax: T) {
        min = initialMin
        max = initialMax
    }

    constructor()


    fun update(newValue: T) {
        min = if (this::min.isInitialized) minOf(min, newValue) else newValue
        max = if (this::max.isInitialized) maxOf(max, newValue) else newValue
    }

    fun asRange(): ClosedRange<T> {

        return min.rangeTo(max)
    }

    operator fun plusAssign(v : T) = update(v)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Constraint<*>) return false

        if (min != other.min) return false
        return max == other.max
    }

    override fun hashCode(): Int {
        var result = min.hashCode()
        result = 31 * result + max.hashCode()
        return result
    }

    operator fun component1(): T = min
    operator fun component2(): T = max


}
