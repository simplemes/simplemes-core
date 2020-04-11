/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample


/**
 * The interface to many sample ExtensionPoint tests.  Simple argument types.
 */
interface SampleExtensionInterface {

  /**
   * The extension method executed before the core method.
   * @param argument1 The argument(s) passed to the core method.
   * @param argument2 The argument(s) passed to the core method.
   */
  void preCoreMethod(String argument1, Integer argument2)

  /**
   * The extension method executed after the core method.
   * @param originalResponse The returned value from the core method.
   * @param argument1 The argument(s) passed to the core method.
   * @param argument2 The argument(s) passed to the core method.
   * @return The new response (if not null).
   */
  Map postCoreMethod(Map originalResponse, String argument1, Integer argument2)


}