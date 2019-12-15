package org.simplemes.eframe.data


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
// TODO: Replace with non-hibernate alternative

@SuppressWarnings("GrDeprecatedAPIUsage")
class EncodedType /*implements UserType*/ {

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
/*
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
*/

  /**
   * Implements the setter for hibernate.
   * @param st The prepared statement to set the value in.
   * @param value The value.
   * @param index The column index.
   * @throws SQLException
   */
/*
  void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
    if (value == null) {
      DateType.INSTANCE.set(st, null, index, session)
    } else {
      def id = value.id.toString()
      StringType.INSTANCE.set(st, id, index, session)
    }
  }
*/


  /**
   * Returns the class for this data type.
   */
  Class returnedClass() {
    return type
  }


}
