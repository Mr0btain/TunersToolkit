package org.mr0btain.tunerstoolkit.core.calibration

import org.mr0btain.tunerstoolkit.core.image.AddressRange
import org.mr0btain.tunerstoolkit.core.image.MemoryImage

/**
 * One stretch of bytes that differs between two images, and the objects with parts in it (none when it's outside
 * every object)
 */
class ChangedRegion(val range: AddressRange, val objects: List<String>)

/**
 * How two images differ
 * @property changedBytes Bytes that differ, counting bytes only one image has
 * @property changedObjects Objects with at least one differing byte, in address order
 */
class ImageComparison(val regions: List<ChangedRegion>, val changedObjects: List<CalibrationObject>) {
    val changedBytes: Long get() = regions.sumOf { it.range.size }

    /** Changed regions no object covers */
    val outsideObjects: List<ChangedRegion> get() = regions.filter { it.objects.isEmpty() }
}

object Compare {
    /** Compares [mine] with [theirs], naming the objects of [index] that changed */
    fun images(mine: MemoryImage, theirs: MemoryImage, index: ObjectIndex? = null): ImageComparison {
        val regions = mine.diff(theirs).map { range ->
            ChangedRegion(range, index?.overlapping(range)?.map { it.obj.name }?.distinct().orEmpty())
        }
        val changed = regions.flatMap { it.objects }.toSet()
        val objects = index?.objects?.filter { it.name in changed }.orEmpty()
        return ImageComparison(regions, objects)
    }

    /**
     * Copies [obj]'s own bytes (its parts in [index]) from [from] into [into], as one undo step. Only bytes both
     * images have are copied
     * @return How many bytes were copied
     */
    fun copyObject(obj: CalibrationObject, index: ObjectIndex, from: MemoryImage, into: MemoryImage): Int {
        var copied = 0
        into.edit {
            for (part in index.parts(obj.name)) {
                val size = part.range.size.toInt()
                val bytes = from.readOrNull(part.range.start, size) ?: continue
                if (into.contains(part.range.start, size) && write(part.range.start, bytes)) copied += size
            }
        }
        return copied
    }
}
