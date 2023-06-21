package app.interactiveschemainferrer.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import javafx.util.StringConverter

class JsonStringConverter : StringConverter<JsonNode>() {
    /**
     * Converts the object provided into its string form.
     * Format of the returned string is defined by the specific converter.
     * @param object the object of type `T` to convert
     * @return a string representation of the object passed in.
     */
    override fun toString(`object`: JsonNode?): String =
        if (`object` != null) `object`.toPrettyString() else ""

    /**
     * Converts the string provided into an object defined by the specific converter.
     * Format of the string and type of the resulting object is defined by the specific converter.
     * @param string the `String` to convert
     * @return an object representation of the string passed in.
     * @throws JsonMappingException
     */
    @Throws(JsonMappingException::class)
    override fun fromString(string: String?): JsonNode {
        if (string == null) {
            return NullNode.instance
        }
        return ObjectMapper().readTree(string)
    }
}
