package org.simplemes.eframe.date;/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Stub for format.
 */
public class ISODate {

  public static String format(Date date) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    sdf.setLenient(false);
    // Format and add the trailing colon in the TZ section for proper ISO format.
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    return sdf.format(date);

  }
}
