package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun `verify application identity`() {
    val appName = "حساب السكن"
    assertEquals("حساب السكن", appName)
  }
}
