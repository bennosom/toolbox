package io.engst.launcher.ui.grid

/**
 * Move an item in a list from one index to another, shifting other items to make room.
 * If [fromIndex] == [toIndex], the original list is returned.
 */
fun <T> moveItem(list: List<T>, fromIndex: Int, toIndex: Int): List<T> {
   if (fromIndex == toIndex) return list
   require(fromIndex in list.indices) { "fromIndex out of bounds: $fromIndex not in 0..${list.lastIndex}" }
   val mutable = list.toMutableList()
   val item = mutable.removeAt(fromIndex)
   val target = toIndex.coerceIn(0, mutable.size)
   mutable.add(target, item)
   return mutable.toList()
}
