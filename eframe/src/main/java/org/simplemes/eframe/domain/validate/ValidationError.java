/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain.validate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * A single validation error on a field.  The display value (toString()) uses the messages.properties file
 * to find the matching error string, with replaceable parameters.  The key in the .properties file is
 * "error.$code.message".  The first argument ({0}) is the fieldName for this error.
 */
public class ValidationError implements ValidationErrorInterface {

  /**
   * The message code.
   */
  int code;

  /**
   * The field with the validation error.
   */
  String fieldName;

  /**
   * The args for the formatted message.
   */
  Object[] args;

  /**
   * Basic constructor.
   *
   * @param code      The message code (e.g. 100 will use '100.error' from the messages.properties file(s)).
   * @param fieldName The field with the error.
   */
  public ValidationError(int code, String fieldName, Object... args) {
    this.code = code;
    this.fieldName = fieldName;
    this.args = args;
  }

  /**
   * The name of the field with the validation error.
   *
   * @return The field name.
   */
  @Override
  public String getFieldName() {
    return fieldName;
  }

  /**
   * The message code.
   *
   * @return The message code.
   */
  @Override
  public int getCode() {
    return code;
  }

  public String toString(Locale locale) {
    Object[] argsWithFieldName = new Object[args.length + 1];
    argsWithFieldName[0] = fieldName;
    System.arraycopy(args, 0, argsWithFieldName, 1, args.length);
    return lookup("error." + code + ".message", locale, argsWithFieldName);
  }

  @Override
  public String toString() {
    return toString(null);
  }

  /**
   * The cached lookup method.
   */
  private static Method lookupMethod;

  /**
   * A convenience method to access the GlobalUtils from the groovy code-base.  Uses reflection
   * to avoid Java compile errors.
   *
   * @param key    The lookup key.
   * @param locale The locale.
   * @param args   The arguments.
   * @return The looked up value.
   */
  //@SuppressWarnings("JavaReflectionMemberAccess")
  private String lookup(String key, Locale locale, Object[] args) {
    // return GlobalUtils.lookup(key, locale, args);
    if (lookupMethod == null) {
      // Cache the lookup method
      try {
        Class<?> clazz = Class.forName("org.simplemes.eframe.i18n.GlobalUtils");
        Class<?>[] paramTypes = new Class<?>[3];
        paramTypes[0] = String.class;
        paramTypes[1] = Locale.class;
        paramTypes[2] = Object[].class;
        lookupMethod = clazz.getDeclaredMethod("lookup", paramTypes);

      } catch (ClassNotFoundException | NoSuchMethodException ignored) {
      }

    }
    Object[] args2 = new Object[3];
    args2[0] = key;
    args2[1] = locale;
    args2[2] = args;

    try {
      return (String) lookupMethod.invoke(null, args2);
    } catch (IllegalAccessException | InvocationTargetException ignored) {
      return "Could not find GlobalUtils for message key " + key;
    }
  }
}
