package app.strategy

import com.fasterxml.jackson.databind.node.ObjectNode
import com.saasquatch.jsonschemainferrer.GenericSchemaFeature
import com.saasquatch.jsonschemainferrer.GenericSchemaFeatureInput

class DescriptionProvider : GenericSchemaFeature {
    /**
     * Get the add-on result to be merged in with the schema
     */
    override fun getFeatureResult(input: GenericSchemaFeatureInput): ObjectNode? {
        println(input.schema.toPrettyString())
//        println(
//            "Do you want to provide a description (yes/no) : \n ${
//                input.samples.stream().limit(5).map(JsonNode::toPrettyString).toList()
//            }"
//        )
//        if (readln() == "yes") {
//            return JsonNodeFactory.instance.objectNode().apply {
//                this.put("description", readln())
//            }
//        }
        return null;
    }
}
