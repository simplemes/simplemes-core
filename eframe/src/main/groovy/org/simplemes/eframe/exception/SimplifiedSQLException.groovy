/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.exception

import org.postgresql.util.PSQLException

import java.sql.SQLException

/**
 * Defines a wrapper exception that simplifies (clarifies) the message for the SQL exception.
 * This hides the dialect-specific parsing of exception internals to produce
 * a more readable message for the end-user.
 * <p>
 * This relies on internals of the dialect to find the user-friendly message.
 */
class SimplifiedSQLException extends RuntimeException {

  // TODO: Implement tests using mocks for H2 and Postgres.
  /**
   * The original exception.
   */
  Throwable cause

  String message

  SimplifiedSQLException(Exception original) {
    cause = original
    message = cause.toString()
    if (cause?.cause) {
      cause = cause.cause
    }
    if (cause instanceof PSQLException) {
      // Postgres variant
      //noinspection GroovyAccessibility
      Map messageParts = cause.serverError.mesgParts
      Character c = Character.valueOf((char) 'C')
      def serverCode = messageParts[c]
      if (serverCode == "23505") {
        // Key already exists.
        Character d = Character.valueOf((char) 'D')
        message = messageParts[d]
      }
    } else if (cause instanceof SQLException) {
      simplifyH2Exception(cause)
    }
  }

  /**
   * Handles H2 exceptions to strip out the important stuff (if possible).
   */
  void simplifyH2Exception(SQLException e) {
    def className = e.class.name
    if (className == "org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException") {
      message = e.message
      def i = message.indexOf("\n")
      if (i > 0) {
        message = message[0..(i - 1)]
      }
    }


  }

  @Override
  String toString() {
    return message
  }

}
