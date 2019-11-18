package org.simplemes.eframe.data

import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.type.DateType
import org.hibernate.usertype.UserType
import org.simplemes.eframe.date.DateOnly

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Provides a hibernate user type for DateOnly elements.  Defines the type definition for DateOnly elements.
 * These are stored in a standard SQL date column.
 * <p/>
 * To save this in a normal SQL DATE column, the default gorm mapping must be added to your Config.groovy file: <p/>
 * <pre>
 * grails.gorm.default.mapping = &#123;
 *   'user-type'(type: org.simplemes.eframe.data.DateOnlyType, class: org.simplemes.eframe.misc.DateOnly)
 * &#125;
 * </pre>
 *
 * Original Author: mph
 *
 */
@SuppressWarnings("GrDeprecatedAPIUsage")
class DateOnlyType implements UserType {
  /**
   * The type number for this class.  Use a very high number to fake out JDBC mapping.
   */
  //public static final int JDBC_TYPE = 1337

  // Consider adding the required mapping automatically.
  // Tried adding mapping in EframeGrailsPlugin.doWithApplicationContext, but this is run after GORM is initialized.
  // These options did not work.
  // http://grailsrocks.github.io/grails-platform-core/guide/configuration.html#changing_config
  // https://github.com/gpc/grails-joda-time/blob/master/scripts/InstallJodaTimeGormMappings.groovy
  int[] sqlTypes() {
    return [DateType.INSTANCE.sqlType()]
    //return  [JDBC_TYPE]
  }

  /**
   * Implement the get method for hibernate.
   * @param rs The result set to get the value from.
   * @param names The name of the column.
   * @param owner The owner.
   * @return The value (DateOnly).
   * @throws SQLException
   */
  Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
    //def d1 = rs.getDate(names[0])
    //assert names.length == 1;
    //println "rs = ${rs}"
    Date d = DateType.INSTANCE.get(rs, names[0], session) as Date // already handles null check
    //println "d = ${d}, ${d.time}, offset = ${d.timezoneOffset}"
    // Fix the TZ adjustment that the JDBC driver does for us.  (Makes sure the time is midnight, UTC).
    return d == null ? null : new DateOnly(d.time - d.timezoneOffset * 60000)
  }


  /**
   * Implements the setter for hibernate.
   * @param st The prepared statement to set the value in.
   * @param value The value.
   * @param index The column index.
   * @throws SQLException
   */
  void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
    //println "value = $value, ${value?.time}, offset=${value?.timezoneOffset}"
    if (value == null) {
      DateType.INSTANCE.set(st, null, index, session)
    } else {
      Date d = (Date) value
      // Adjust to UTC to make sure the value is stored properly in Date columns.
      d = new Date(d.time + d.timezoneOffset * 60000)
      DateType.INSTANCE.set(st, (Date) d, index, session)
    }
  }


  /**
   * Returns the class for this data type.
   * @return DateOnly.class.
   */
  Class returnedClass() {
    return DateOnly
  }

  /**
   * Compares two DateOnly elements.
   * @param o1 One Date.
   * @param o2 Other Date.
   * @return True if equal.
   * @throws HibernateException
   */
  @Override
  boolean equals(Object o1, Object o2) throws HibernateException {
    return o1 == o2
    //return false
  }

  /**
   * Calculates the hash code for the date.
   * @param o The date.
   * @return The hash code.
   * @throws HibernateException
   */
  int hashCode(Object o) throws HibernateException {
    return o.hashCode()
  }

  /**
   * Performs a deep copy.  Creates a new DateOnly with the same time.
   * @param o The date only.
   * @return The copy.
   * @throws HibernateException
   */

  Date deepCopy(Object o) throws HibernateException {
    if (o == null) {
      return null
    }
    return new DateOnly((long) o.time)
    //return null
  }

  /**
   * Disassembles the object for storage.  Just does a deep copy.
   * @param o The date only.
   * @return The copy.
   * @throws HibernateException
   */
  Serializable disassemble(Object o) throws HibernateException {
    return (Serializable) deepCopy(o)
  }

  /**
   * Assembles a DateOnly from storage. Just does a deep copy.
   * @param serializable The object to assemble.
   * @param owner The owner.
   * @return The copy.
   * @throws HibernateException
   */
  Object assemble(Serializable serializable, Object owner) throws HibernateException {
    //println "serializable = ${serializable}"
    return deepCopy(serializable)
  }

  /**
   * During merge, replace the existing (target) value in the entity we are merging to with a new (original) value from the detached entity we are merging.
   * @param original The original date.
   * @param target The target date.
   * @param owner The owner.
   * @return A deep copy of the the original.
   * @throws HibernateException
   */
  Object replace(Object original, Object target, Object owner) throws HibernateException {
    return deepCopy(original)
  }

  /**
   * DateOnly objects are mutable.
   * @return True
   */
  boolean isMutable() {
    return true
  }

}
