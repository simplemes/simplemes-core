package org.simplemes.eframe.date

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Factory
import io.micronaut.core.convert.ConversionContext
import io.micronaut.core.convert.TypeConverter

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Convert to/from SQL Date for DateOnly.  Needs to adjust for automatic JDBC time zone adjustment.
 */
@Factory
@CompileStatic
class DateOnlyConverters {

  @Singleton
  TypeConverter<DateOnly, Date> dateOnlyToDateConverter() {
    return { DateOnly dateOnly, Class targetType, ConversionContext context ->
      def adj = TimeZone.default.getOffset(dateOnly.time)
      //println "adjFrom = $adj, orig = ${dateOnly.time}, target $targetType"
      // Need to adjust since JDBC automatically adjust for the local TZ.
      Optional.of(new Date(dateOnly.time - adj))
    } as TypeConverter<DateOnly, Date>
  }

  @Singleton
  TypeConverter<Date, DateOnly> dateToDateOnlyConverter() {
    return { Date date, Class targetType, ConversionContext context ->
      //println "dateToDateOnlyConverter ms = $date.time"
      def adj = TimeZone.default.getOffset(date.time)
      //println "adjTo = $adj ${date.time} fixed = ${date.time+adj}"
      // Need to adjust since JDBC automatically adjust for the local TZ.
      Optional.of(new DateOnly(date.time + adj))
    } as TypeConverter<Date, DateOnly>
  }
}
