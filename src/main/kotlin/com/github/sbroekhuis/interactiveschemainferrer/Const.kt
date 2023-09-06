package com.github.sbroekhuis.interactiveschemainferrer

/** Constants in the Program */
@Suppress("unused")
object Const {
    object Fields {
        const val MULTIPLE_OF = "multipleOf"
        const val ITEMS: String = "items"
        const val TYPE: String = "type"
        const val CONST: String = "const"
        const val ANY_OF: String = "anyOf"
        const val ALL_OF: String = "allOf"
        const val CONTAINS: String = "contains"
        const val MIN_CONTAINS: String = "minContains"
        const val MAX_CONTAINS: String = "maxContains"
        const val MIN_LENGTH: String = "minLength"
        const val MAX_LENGTH: String = "maxLength"
        const val MINIMUM: String = "minimum"
        const val MAXIMUM: String = "maximum"
        const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"
        const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"
        const val MIN_PROPERTIES: String = "minProperties"
        const val MAX_PROPERTIES: String = "maxProperties"
        const val MIN_ITEMS: String = "minItems"
        const val MAX_ITEMS: String = "maxItems"
        const val PREFIX_ITEMS: String = "prefixItems"
        const val DEFAULT: String = "default"
        const val UNIQUE_ITEMS: String = "uniqueItems"
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
