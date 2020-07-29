package org.simplemes.eframe.date;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

/**
 * Tests.
 */
class ISODateTest {

  @Test
  void testFormat() {
    Date date = new Date(1276560000000L);
    assertEquals(ISODate.format(date),"2010-06-15T00:00:00.000");
  }
}