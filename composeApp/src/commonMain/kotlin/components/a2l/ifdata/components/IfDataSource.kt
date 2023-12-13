package components.a2l.ifdata.components

import components.a2l.objects.IfDataClass

data class IfDataSource(
    val label: String, // Name of the DAQ-List (data acquisition list),
    val baseScaleUnit: Int, // Period definition : Basic scaling unit in
    val rateScaleUnit: Long, /* Period definition : Rate in Scaling Units */
): IfDataClass<IfDataSource> {

    /*
    /begin SOURCE
    "segment synchronous event channel"
    103
    1
    /begin QP_BLOB
    2
    LENGTH 1
    CAN_ID_FIXED 0x7C7
    RASTER 2
    /end QP_BLOB
    /end SOURCE

     */
    override fun figureOut(): IfDataSource {
        TODO("Not yet implemented")
    }

}