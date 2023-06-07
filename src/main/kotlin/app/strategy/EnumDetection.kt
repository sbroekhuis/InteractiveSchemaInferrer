import com.fasterxml.jackson.databind.JsonNode
import com.saasquatch.jsonschemainferrer.EnumExtractor
import com.saasquatch.jsonschemainferrer.EnumExtractorInput
import java.util.*
import javax.swing.JOptionPane

class EnumDetection : EnumExtractor {

    companion object {
        var THRESHOLD: Float = 0.1f
    }

    /**
     * @return The *group* of enums. Note that each group is expected to be not null and not
     * empty. All the elements in each group are expected to come directly from the given
     * samples if possible to ensure [JsonNode.equals] works correctly.
     */
    override fun extractEnums(input: EnumExtractorInput): MutableCollection<MutableCollection<out JsonNode>> {
        val samples: MutableCollection<out JsonNode> = input.samples
        val distinctSize = samples.distinct().size.toFloat()
        val totalSize = samples.size.toFloat()
        val fl = distinctSize / totalSize
        if (fl <= THRESHOLD) {
            println("DETECTED POSSIBLE ENUM");
            println("Distinct: $distinctSize vs Totalsize: $totalSize \t ($fl)")
            println("${samples.distinct().map { it.toPrettyString() }}")
            println()
            val showConfirmDialog = JOptionPane.showConfirmDialog(
                null,
                "Is this an enum? \n ${samples.distinct().map { it.toPrettyString() }}",
                "User-Based Inference",
                JOptionPane.YES_NO_OPTION
            )


            if (showConfirmDialog == 1) {
                return Collections.singleton(samples.distinct().toMutableSet())
            }
        }
        return Collections.emptySet()
    }
}
