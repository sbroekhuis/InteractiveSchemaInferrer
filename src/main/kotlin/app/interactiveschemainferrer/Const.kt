package app.interactiveschemainferrer

/** Constants in the Program */
object Const {
    object Fields {
        const val ITEMS: String = "items"
        const val TYPE: String = "type"
        const val CONST: String = "const"
        const val ANY_OF: String = "anyOf"
        const val ALL_OF: String = "allOf"
        const val CONTAINS: String= "contains"
        const val MIN_CONTAINS: String= "minContains"
        const val MAX_CONTAINS: String= "maxContains"
        const val PREFIX_ITEMS: String= "prefixItems"
        const val DEFAULT: String= "default"
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
