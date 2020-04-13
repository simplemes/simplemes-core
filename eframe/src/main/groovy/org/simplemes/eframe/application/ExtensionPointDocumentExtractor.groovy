/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application


import org.simplemes.eframe.custom.annotation.ExtensionPoint
import org.simplemes.eframe.misc.FileUtils
import org.simplemes.eframe.misc.TypeUtils

import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * A utility application that will extract the ExtensionPoints defined in the current application's
 * build/classes/main/groovy directory.  Used to generate the  'build/generated/extensions.adoc' file
 * for use in your documentation.
 * <p>
 * This utility ignores .class files with a $ in the the name.
 * <p>
 * Use the gradle task generateExtensionDoc to execute this utility.
 */
class ExtensionPointDocumentExtractor {

  /**
   * The directory to search for extension points.
   */
  static final String CLASSES_DIR = 'build/classes/groovy/main'

  /**
   * The Ascii Doctor file to create with the extension points found.
   */
  static final String ADOC_FILE = 'build/generated/extensions.adoc'


  static void main(String[] args) {
    processClasses(new File(CLASSES_DIR), new File(ADOC_FILE))
  }

  /**
   * Processes the given folder for .classes that provide the ExtensionPoint and writes them to the given doc
   * output file.
   * @param folder The .class folder.
   * @param outputFile The output file.
   */
  static void processClasses(File folder, File outputFile) {
    def lines = []
    folder.eachFileRecurse { file ->
      if (file.isFile() && file.name.endsWith('.class') && !file.name.contains('$')) {
        def className = getClassName(file)
        def found = checkClass(TypeUtils.loadClass(className))
        if (found) {
          lines.addAll(found)
        }
      }
    }

    if (outputFile.exists()) {
      outputFile.delete()
    }
    lines = lines.sort { a, b -> a <=> b }
    def writer = outputFile.newWriter()
    for (line in lines) {
      writer.write(line + "\n")
    }
    writer.close()

  }

  /**
   * Converts the given file name into a class package/name.
   * @param file The file
   * @return The package/name for the class.
   */
  static String getClassName(File file) {
    def rootDir = FileUtils.convertToOSPath("$CLASSES_DIR/")
    def s = file.path
    s = s - rootDir
    s = s - "$CLASSES_DIR/"  // Remove the non-OS form of the prefix, mainly for unit tests.
    s = s - '.class'
    def slash = File.separator
    if (slash == '\\') {
      slash = '\\\\'
    }
    return s.replaceAll(slash, '.').replace('/', '.')
  }

  /**
   * Finds all extension points for this class.
   * @param clazz The clazz.
   * @return The list of outputs found.
   */
  static List<String> checkClass(Class clazz) {
    def res = []
    for (method in clazz.methods) {
      def annotation = method.getAnnotation(ExtensionPoint)
      if (annotation) {
        res << buildDocText(method, annotation)
      }
    }
    return res
  }

  /**
   * Builds the adoc line for a given method.
   * @param method
   * @param annotation
   * @return
   */
  static String buildDocText(Method method, ExtensionPoint annotation) {
    String comment = annotation.comment() ?: "(no comment)"
    Class interfaceType = annotation.value()

    String path = buildPathForGroovyDocLink(method.getDeclaringClass().getName())
    String methodAndParams = buildMethodAndParameters(method)
    String methodName = method.getDeclaringClass().simpleName + "." + method.getName()
    String methodLink = String.format("link:groovydoc%s.html#%s[%s()^] icon:share-square-o[role=\"link-blue\"]",
                                      path, methodAndParams, methodName)
    //System.out.println("methodLink:" + methodLink)

    String interfacePath = buildPathForGroovyDocLink(interfaceType.getName())
    String interfaceName = interfaceType.simpleName
    String interfaceLink = String.format("link:groovydoc%s.html[%s^] icon:share-square-o[role=\"link-blue\"]",
                                         interfacePath, interfaceName)
    //System.out.println("interfaceLink:" + interfaceLink)
    //link:groovydoc/sample/service/OrderService.html#release(sample.domain.Order)[OrderService.release()^] icon:share-square-o[role="link-blue"]

    return "* $methodLink - ${comment}. $interfaceLink"
  }

  /**
   * Builds the groovyDoc style path to the given class (/sample/Class).
   *
   * @param className The class/path name.
   * @return The path.
   */
  private static String buildPathForGroovyDocLink(String className) {
    return "/" + className.replace('.', '/')
  }

  /**
   * Builds the groovyDoc style method/parameters string.
   *
   * @param method The method.
   * @return The groovydoc format of the method/params.
   */
  private static String buildMethodAndParameters(Method method) {
    // Build the parameters (if any)
    StringBuilder sb = new StringBuilder()
    for (Parameter parameter : method.getParameters()) {
      if (sb.length() > 0) {
        sb.append(",%20")
      }
      sb.append(parameter.getType().getName())
    }

    return method.getName() + "(" + sb.toString() + ")"
  }


}