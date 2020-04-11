/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides helper methods for the ExtensionPoint annotation.
 */
public class ExtensionPointHelper {

  /**
   * The logger.
   */
  private static final Logger log = LoggerFactory.getLogger(ExtensionPointHelper.class);

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  protected static ExtensionPointHelper instance = new ExtensionPointHelper();

  /**
   * Invokes all pre method extensions for the given class.
   *
   * @param interfaceClass The interface class.  All beans executed will implement this interface.
   * @param arguments      The runtime arguments from the original method call.
   */
  void invokePre(Class interfaceClass, String methodName, Object... arguments) {
    //getBeans(marker)
    //call preMethod() on all
  }

  /**
   * Invokes all pre method extensions for the given class.
   *
   * @param interfaceClass The interface class.  All beans executed will implement this interface.
   * @param arguments      The runtime arguments from the original method call.
   */
  Object invokePost(Class interfaceClass, String methodName, Object response, Object... arguments) {
    //getBeans(marker)
    //call preMethod() on all
    return null;
  }


  /*
ExtensionPointHelper.instance.invokePost(Class bean,response,arguments)

   */

}
