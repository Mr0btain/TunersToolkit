package components.parsers.a2l.ifdata.components

import kotlinx.serialization.Serializable

@Serializable
data class Raster(
    var description: String = "",
    var label: String = "",
    var channelNumber: Int = Int.MIN_VALUE,
    var basicScaleUnit: Int = Int.MIN_VALUE,
    var ecuSampleRate: Long = Long.MIN_VALUE,
    var exclusive: Int = Int.MIN_VALUE
)

fun parseAndFormatRasters(input: String): Raster {
    // TODO: FIX RASTER
    // Find the Raster section in the entire list
    val rast = Raster()
    val pattern = """(?s)/begin RASTER(.*?)\n/end RASTER""".toRegex()
    val matchedAnnotations = pattern.findAll(input)
        .map { it.groupValues[1].trim() }
        .toList()
    matchedAnnotations.forEach { matchLine ->
        val cleaned = matchLine.replace("/begin RASTER", "")
            .replace("/end RASTER", "")
            .split("\n")
        rast.description = cleaned[0]
        rast.label = cleaned[1]
        //rast.channelNumber = cleaned[2].toIntOrNull()
        //rast.basicScaleUnit = cleaned[3].toIntOrNull()
        //rast.ecuSampleRate = cleaned[4].toLongOrNull()
        cleaned.subList(5, cleaned.size).forEach { lowerItem ->
            when (lowerItem.trim()) {
                "EXCLUSIVE" -> {
                    //rast.exclusive = cleaned[index + 1].toIntOrNull()
                }
            }
        }
    }
    return Raster()
}
    /*
    /* Description of the available ECU Sampling Rates (Event Channels)      */
    (block "RASTER" struct {
        char [101];   /* CCP Event Channel Name                               */
        char [9];     /* Short Display Name of the Event Channel Name         */
        uchar;        /* Event Channel No., used for CCP START_STOP)          */
        int;          /* Period definition :  basic scaling unit in CSE
                        as defined in ASAP1b                                 */
        long;         /* ECU sample rate of the event channel,
                        period definition based on the basic scaling unit    */
        taggedstruct {
            ("EXCLUSIVE" uchar )*;
            /* Exclusion of other CCP Event Channels                */
        };
    } )*;
    /begin RASTER
       "time synchronous event channel - 100 ms"
       "100_ms"
    1
    5
    1
   /end RASTER
     */