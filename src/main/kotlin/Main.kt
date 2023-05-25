import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer
import com.saasquatch.jsonschemainferrer.RequiredPolicies
import com.saasquatch.jsonschemainferrer.SpecVersion

val mapper: ObjectMapper = jacksonObjectMapper()


fun main(args: Array<String>) {


//    var json = mapper.readTree(URL("https://api.opensource.org/licenses/"))
//    var json = mapper.readTree(object {}.javaClass.getResourceAsStream("/pokemon-go/pokedex.json"))
    var json = mapper.readTree(object {}.javaClass.getResourceAsStream("/legends-of-runeterra/set1-en_us.json"))
//    var expected = mapper.readTree(object {}.javaClass.getResourceAsStream("/pokemon_schema.json"))

    var builder =
        JsonSchemaInferrer.newBuilder().setSpecVersion(SpecVersion.DRAFT_07).addEnumExtractors(EnumDetection())

            .addGenericSchemaFeatures(DescriptionProvider())
            .setRequiredPolicy(RequiredPolicies.nonNullCommonFields()).build()
    var schema = builder.inferForSamples(json.toList())


    println(schema.toPrettyString())

}
