package app.interactiveschemainferrer

/** Constants in the Program */
object Const {
    object Fields {
        const val CONST: String = "const"
        const val ANY_OF: String = "anyOf"
    }

    /**
     * Type names
     */
    object Types {
        const val OBJECT = "object"
        const val ARRAY = "array"
        const val STRING = "string"
        const val BOOLEAN = "boolean"
        const val INTEGER = "integer"
        const val NUMBER = "number"
        const val NULL = "null"
        val NUMBER_TYPES = setOf(NUMBER, INTEGER)
        val CONTAINER_TYPES = setOf(OBJECT, ARRAY)
    }

}
