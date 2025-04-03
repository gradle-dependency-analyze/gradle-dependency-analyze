package com.github.rusio.gda

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GreetingsTest {

  @Test
  fun hello() {
    assertEquals("Hey ho, let's go :)", Greetings.hello())
  }

}
