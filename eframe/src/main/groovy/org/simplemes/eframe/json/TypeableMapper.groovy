/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * The JSON formatter/parser that handles arbitrary list or map of objects in a single JSON string.
 * The JSON is formatted as an array with a 2 or 3 entries for each object.
 * <p>
 * For lists, the entries are:<p>
 * <ul>
 *   <li><b>0</b> - The class name of the object. </li>
 *   <li><b>1</b> - The object. </li>
 * </ul>
 *
 * <p>
 * For maps, the entries are:<p>
 * <ul>
 *   <li><b>0</b> - The class name of the object. </li>
 *   <li><b>1</b> - The key for the map (a string). </li>
 *   <li><b>2</b> - The object. </li>
 * </ul>
 * <p>
 *
 * This mapper only supports a specific white-list of allowed classes.  The allowed classes include: <p>
 * <ul>
 *   <li><b>Domain Entities</b> - Must have the @MappedEntity annotation. </li>
 *   <li><b>Implements TypeableJSONInterface</b> - Specific classes marked with this interface </li>
 * </ul>
 * <p>
 * There is an alternate method writing the JSON as a sequence of calls:
 * <p> 1. start(writer)
 * <p> 2. writeOne(writer, object)  (repeated)
 * <p> 3. finish(writer)
 */
class TypeableMapper {
  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static TypeableMapper instance = new TypeableMapper()


  /**
   * Writes the given list of objects to the given writer.  This includes the class name of the object to make
   * sure it can be read with the {@link #read(java.io.Reader)} method.
   * @param writer The writer to write the JSON to.
   * @param list The list of objects.  Null not allowed.
   */
  void writeList(Writer writer, List list) {
    ArgumentUtils.checkMissing(list, 'list')
    def mapper = Holders.objectMapper
    def listWithTypes = []
    for (o in list) {
      ArgumentUtils.checkMissing(o, 'o')
      if (!isValidClass(o.getClass())) {
        throw new IllegalArgumentException("Class ${o.getClass()} is not allowed in a TypeableMapper.  Only @MappedEntity or TypeableJSONInterface class allowed.")
      }
      listWithTypes << o.getClass().name
      listWithTypes << o
    }

    mapper.writeValue(writer, listWithTypes)
  }

  /**
   * Starts the output of the JSON.  <p>
   *   <b>Note:</b> Do not use with writeList().
   * @param writer The writer to write the JSON to.
   */
  void start(Writer writer) {
    writer << '['
  }

  /**
   * Finishes the output of the JSON.  <p>
   *   <b>Note:</b> Do not use with writeList().
   * @param writer The writer to write the JSON to.
   */
  void finish(Writer writer) {
    writer << ']'
  }

  /**
   * Writes a single element.  You must call start() before and finished() after the objects are written.
   * @param writer The writer to write the JSON to.
   * @param object The object to write.  Null not allowed.
   * @param first If true, then this is the first element written. Used to make sure the right commas are used between elements.
   */
  void writeOne(Writer writer, Object object, boolean first) {
    ArgumentUtils.checkMissing(object, 'object')
    def mapper = Holders.objectMapper

    if (!first) {
      // Need a comma between rows
      writer << ","
    }
    writer << "\"${object.getClass().name}\""

    def stringWriter = new StringWriter()
    mapper.writeValue(stringWriter, object)

    writer << ","
    writer << stringWriter.toString()

  }

  /**
   * Reads multiple elements from the input reader and creates the original objects.  Uses the type (class name)
   * from the JSON array.
   * @param reader The reader for the JSON source.
   * @return The objects.
   */
  @SuppressWarnings(["GroovyAssignabilityCheck"])
  List read(Reader reader) {
    def res = []
    def mapper = Holders.objectMapper
    def list = mapper.readValue(reader, List)
    def count = list.size()
    if (count == 0) {
      return res
    }
    if ((count % 2) == 1) {
      throw new IllegalArgumentException("Input JSON has odd number of elements ($count) in array.")
    }
    for (int i = 0; i < count; i = i + 2) {
      String className = list[i]
      def clazz = TypeUtils.loadClass(className)
      if (!isValidClass(clazz)) {
        throw new IllegalArgumentException("Class ${clazz.name} is not allowed in a TypeableMapper.  Only @MappedEntity or TypeableJSONInterface class allowed.")
      }
      def o = mapper.convertValue(list[i + 1], clazz)
      res << o
    }

    return res
  }

  /**
   * Determines if the given class is valid for de-serialization using this mapper.
   * @param clazz The class
   * @return True if this class is valid.
   */
  static boolean isValidClass(Class clazz) {
    return (clazz.isAnnotationPresent(MappedEntity)) || (TypeableJSONInterface.isAssignableFrom(clazz))
  }

}
