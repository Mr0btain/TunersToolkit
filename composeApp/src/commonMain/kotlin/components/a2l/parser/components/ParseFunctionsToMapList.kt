package components.a2l.parser.components

import components.a2l.functions.A2LFunction
import components.utilitytab.maplisttab.classes.MapDataContainer

/**
 * Parses the functions to a map list used for our MapListTab
 */
fun parseFunctionsToMapList(functions: Map<String, A2LFunction>): List<MapDataContainer> {
    val mMapList = mutableListOf<MapDataContainer>()

    functions.forEach { (_, function) ->
        val mResultChildren = mutableListOf<String>()
        mResultChildren.addAll(function.getDefCharacteristics())

        if (mResultChildren.isNotEmpty()) {
            mMapList.add(
                MapDataContainer(
                    title = function.name,
                    children = mResultChildren
                )
            )
        }
    }

    return mMapList
}