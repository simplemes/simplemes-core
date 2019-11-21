package org.simplemes.eframe.misc

import org.simplemes.eframe.domain.DomainUtils

import java.lang.reflect.Field

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Various utilities that operate on generic types (or classes).
 *
 */
@SuppressWarnings("NonFinalPublicField")
class TypeUtils {

  /**
   * Determines if the given class has the given static method (no arguments).
   * @param clazz The class to check for a given method.
   * @param methodName The name of the property.
   * @return True if the class has the given method.
   */
  static boolean doesClassHaveStaticMethod(Class clazz, String methodName) {
    try {
      def method = clazz.getMethod(methodName)
      return method != null
    } catch (NoSuchMethodException ignored) {
      // Ignored intentionally.
    }
    return false
  }

  /**
   * Gets the static property from the object.
   * @param object The object to get the property from (if it has it). Can be a class or an instance of an object.
   * @param name The name of the property.
   * @return The property value.
   */
  static Object getStaticProperty(Object object, String name) {
    ArgumentUtils.checkMissing(object, 'object')
    if (object instanceof Class) {
      def prop = object.metaClass.getMetaProperty(name)
      if (prop) {
        return object[name]
      }
    }
    if (object.hasProperty(name)) {
      return object[name]
    }
    return null
  }

  /**
   * Find all values of a given property
   * @param c The class to find the property in, searching through parent classes.
   * @param propertyName The static property to find.
   * @return A list of lists of values, top-level parent first.
   */
  static List<List> getStaticPropertyInSuperClasses(Class c, String propertyName) {
    def list = []
    def fields = c.metaClass.properties
    def prop = fields.find() { it.name == propertyName }
    //println "prop = $prop, ${prop.dump()}"
    //println "prop = $prop + has = ${c.metaClass.hasProperty(propertyName)}"
    if (prop?.getter?.declaringClass?.name == c.name) {
      // Only add it if the property comes from the given class.  Avoids pulling in super-class values multiple times.
      def value = c."$propertyName"
      list << value
    }
    if (c.superclass != Object) {
      def parentList = getStaticPropertyInSuperClasses(c.superclass, propertyName)
      if (parentList) {
        list.addAll(parentList)
      }
    }

    if (list.size() > 1) {
      list = list.reverse()
    }
    return list
  }

  /**
   * Find all of the super-classes for the given class.
   * @param c The class.
   * @param list The list to add the super classes to.
   * @return A list of parent classes (not including Object).
   */
  static List<Class> getSuperClasses(Class c, List<Class> list = null) {
    if (list == null) {
      list = []
    }

    if (c.superclass != Object) {
      list << c.superclass
      getSuperClasses(c.superclass, list)
    }

    return list
  }

  /**
   * Generates an indented class hierarchy for a given class.
   * @param clazz The class.
   * @param list The list of super classes.
   * @return The hierarchy.
   */
  static String toClassHierarchy(Class clazz, List<String> list = null) {
    if (list == null) {
      list = []
    }
    list << clazz.name
    if (clazz.superclass) {
      toClassHierarchy(clazz.superclass, list)
    }

    // Now, invert and indent.
    StringBuilder sb = new StringBuilder()
    def indent = 0
    for (int i = list.size() - 1; i >= 0; i--) {
      if (sb.size()) {
        sb << "\n"
      }
      if (indent) {
        sb << ' ' * indent
      }
      sb << list[i]
      indent++
    }
    return sb.toString()
  }

  /**
   * A short string for the given object.  Usage is similar to toString(), but returns just one key identifier.
   * Suitable for drop-down lists, etc.  Will call toShortString() on the object if it exists.  If not, then
   * will attempt to use the primary key (per DomainUtils.getPrimaryKeyField()).  Finally, will fallback to toString()
   * if none of these work.
   * @param object The object.  Domain object of POGO.
   * @param addTitle If true, then add the title to the short string (e.g. 'ABC (title)').  (<b>Default:</b> false).
   * @return The short string representation of this (e.g. primary key value).  Can be null.
   */
  static String toShortString(Object object, Boolean addTitle = false) {
    if (object == null) {
      return null
    }
    if (object instanceof Map) {
      // Special case for unit tests that use maps as dummy domain objects.
      return object.toString()
    }
    def shortString

    // See if the object supports localized toString().
    if (object?.metaClass?.pickMethod('toShortString', [] as Class[])) {
      shortString = object.toShortString()
    } else {
      def keyName = DomainUtils.instance.getPrimaryKeyField(object.class)
      if (keyName && object.hasProperty(keyName)) {
        shortString = object[keyName]
      } else {
        shortString = object.toString()
      }
    }

    // Now, add the title, it found.
    if (addTitle && object?.hasProperty('title')) {
      def title = object.title
      if (title) {
        shortString = shortString + " ($title)"
      }
    }


    return shortString
  }

  /**
   * Returns the field from the class;s declared fields, null if not found.
   * @param clazz The class to check.
   * @param fieldName The field name.
   * @return The field (can be null).
   */
  static Field safeGetField(Class clazz, String fieldName) {
    def fields = clazz.declaredFields
    return fields.find { it.name == fieldName }
  }


  /**
   * Returns the class instance for the name.
   * @param className The fully-qualified name of the class.
   * @return The class.
   */
  @SuppressWarnings("ClassForName")
  static Class loadClass(String className) {
    return Class.forName(className)
  }

}