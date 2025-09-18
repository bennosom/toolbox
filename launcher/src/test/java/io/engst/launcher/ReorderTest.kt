package io.engst.launcher

import io.engst.launcher.ui.grid.moveItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {

   @Test
   fun move_forward_shifts_left() {
      val input = listOf(1, 2, 3, 4, 5)
      val out = moveItem(input, fromIndex = 1, toIndex = 3)
      assertEquals(listOf(1, 3, 4, 2, 5), out)
   }

   @Test
   fun move_backward_shifts_right() {
      val input = listOf(1, 2, 3, 4, 5)
      val out = moveItem(input, fromIndex = 3, toIndex = 1)
      assertEquals(listOf(1, 4, 2, 3, 5), out)
   }

   @Test
   fun move_to_same_returns_same() {
      val input = listOf("a", "b", "c")
      val out = moveItem(input, fromIndex = 2, toIndex = 2)
      assertEquals(input, out)
   }

   @Test
   fun move_to_out_of_bounds_clamps() {
      val input = listOf(1, 2, 3)
      val out = moveItem(input, fromIndex = 0, toIndex = 999)
      assertEquals(listOf(2, 3, 1), out)
   }
}
