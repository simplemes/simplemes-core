/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain

import io.micronaut.data.model.Pageable
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.annotation.DomainEntityHelper

import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Common utility methods used to access SQL data directly. Use with caution.
 */
class SQLUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static SQLUtils instance = null

  static SQLUtils getInstance() {
    if (!instance) {
      instance = new SQLUtils()
    }
    return instance
  }

  /**
   * Executes the given SQL as a prepared statement with the given arguments.
   * The row offset/limit defaults to offset 0, 100 rows max.
   * @param sql The SQL.
   * @param domainClass The domain class to bind the result set to or Map.  If Map, then the results will be a list of Maps.
   *        The Map element names are the lower-case form of the column names from the DB.
   * @param args Optional arguments for the query.  The first element can be a Pageable for row limits.
   * @return The list of records found.
   */
  @SuppressWarnings("GrUnnecessaryPublicModifier")
  public <T> List<T> executeQuery(String sql, Class<T> domainClass, Object... args) {
    def list = []

    // If first argument is a Pageable, use if for the row limits.
    def addedArgs = []
    Pageable pageable = null
    if (args && args[0] instanceof Pageable) {
      pageable = args[0] as Pageable
    }
    if (!pageable) {
      // Default row limits if not given.
      pageable = Pageable.from(0, 100)
    }

    def pageSize = pageable.size
    def rowStart = pageable.offset

    def sqlLowerCase = sql.toLowerCase()
    if (!sqlLowerCase.contains(' limit ') || !sqlLowerCase.contains(' offset ')) {
      sql = sql + " LIMIT ? OFFSET ?"
      addedArgs << pageSize
      addedArgs << rowStart
    }

    PreparedStatement ps = null
    ResultSet rs = null
    try {
      ps = getPreparedStatement(sql)
      //ps.setString(1, order.getUuid().toString())
      def lastArgIndex = 1

      // Add the caller's args first.
      for (arg in args) {
        if (!(arg instanceof Pageable)) {
          setArg(ps, lastArgIndex, arg)
          lastArgIndex++
        }
      }

      // Now, add any additional args (row limits).
      for (addedArg in addedArgs) {
        setArg(ps, lastArgIndex, addedArg)
        lastArgIndex++
      }
      ps.execute()
      rs = ps.getResultSet()
      while (rs.next()) {
        if (domainClass == Map) {
          def map = [:]
          for (int i = 1; i <= rs.metaData.columnCount; i++) {
            String name = rs.getMetaData().getColumnName(i).toLowerCase()
            map[name] = rs.getObject(i)
          }
          list << map
        } else {
          list << bindResultSet(rs, domainClass)
        }
      }
    } finally {
      try {
        rs?.close()
      } catch (Exception ignored) {
      }
      ps?.close()
    }
    return list
  }

  /**
   * Sets the argument in the given statement.
   * @param ps The statement.
   * @param argIndex The index to set the value at.
   * @param value The value.
   */
  private setArg(PreparedStatement ps, int argIndex, Object value) {
    if (value instanceof DateOnly) {
      ps.setDate(argIndex, new java.sql.Date(((DateOnly) value).time))
    } else {
      ps.setObject(argIndex, value)
    }
  }


  /**
   * Creates a prepared statement for the given SQL.
   * @param sql The SQL.
   * @return The statement.
   */
  PreparedStatement getPreparedStatement(String sql) {
    checkSQL(sql)
    return DomainEntityHelper.instance.getPreparedStatement(sql)
  }

  /**
   * Checks the SQL for possible issues.  Mainly checks for security holes such as single and double quotes.
   * Will throw an exception if invalid.
   * @param sql The SQL.
   */
  void checkSQL(String sql) {
    if (sql?.contains("'") || sql?.contains('"')) {
      throw new IllegalArgumentException("Dynamic SQL Statements can't contain quotes: $sql")
    }
  }

  /**
   * Binds the current row of the given result set to an instance of the given domain class.
   * @param resultSet The result set.
   * @param domainClass The domain class to bind to.
   * @return The resulting object.
   */
  Object bindResultSet(ResultSet resultSet, Class domainClass) {
    return DomainBinder.bindResultSet(resultSet, domainClass)

  }

}
