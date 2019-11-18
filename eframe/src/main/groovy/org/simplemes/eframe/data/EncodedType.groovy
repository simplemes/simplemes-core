package org.simplemes.eframe.data

import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.type.DateType
import org.hibernate.type.StringType
import org.hibernate.usertype.UserType
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.NameUtils

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Provides a hibernate user type for EncodedTypeInterface elements.  Stores as a short ID in a varchar column.
 *
 */
@SuppressWarnings("GrDeprecatedAPIUsage")
class EncodedType implements UserType {

  /**
   * The basic field type for this field.  Used to find the value from the ID stored in the DB column.
   */
  Class type = EncodedTypeInterface

  /**
   * Empty constructor.
   */
  EncodedType() {
  }

  /**
   * Main Constructor.
   * @param clazz The basic field type of the field.  Used to find the value from the encoded ID.
   */
  EncodedType(Class clazz) {
    type = clazz
  }

// TODO: Figure out how to make encodedType fields use maxSize from the constraints.
  /**
   * The type number for this class.  Use a very high number to fake out JDBC mapping.
   */
  int[] sqlTypes() {
    return [Types.VARCHAR]
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
    def clazz = returnedClass()

    if (clazz == EncodedTypeInterface) {
      // This is probably in unit test mode, so we can use the column Name from the meta data to
      // figure out the destination field class.
      // This never happens in production mode since the EframeHibernateMappingContextConfiguration
      // passes the class to this EncodedType. See EframeHibernateMappingContextConfiguration for
      // issue with postgres and column name that forced this feature.
      def metaData = rs.getMetaData()
      def columnName
      for (int i = 1; i <= metaData.columnCount; i++) {
        //println "    col = ${metaData.getColumnName(i)} - ${metaData.getColumnLabel(i)}"
        if (metaData.getColumnLabel(i).equalsIgnoreCase(names[0])) {
          columnName = metaData.getColumnName(i)
          break
        }
      }
      if (columnName) {
        def fieldName = NameUtils.convertFromColumnName((String) columnName)
        def domainClass = owner.getClass()
        clazz = DomainUtils.instance.getFieldType(domainClass, fieldName)
      }
    }

    if (clazz) {
      String s = StringType.INSTANCE.get(rs, names[0], session) // already handles null check
      def value = clazz?.valueOf(s)
      return value
    }

    return null
  }

  /**
   * Implements the setter for hibernate.
   * @param st The prepared statement to set the value in.
   * @param value The value.
   * @param index The column index.
   * @throws SQLException
   */
  void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
    if (value == null) {
      DateType.INSTANCE.set(st, null, index, session)
    } else {
      def id = value.id.toString()
      StringType.INSTANCE.set(st, id, index, session)
    }
  }


  /**
   * Returns the class for this data type.
   */
  Class returnedClass() {
    return type
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

  Object deepCopy(Object o) throws HibernateException {
    if (o == null) {
      return null
    }
    return o
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
