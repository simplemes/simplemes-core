/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample


/**
 * The interface to many sample ExtensionPoint tests.  No arguments.
 */
@SuppressWarnings("unused")
interface SampleNoArgumentExtensionInterface {

  /**
   * The extension method executed before the core method.
   */
  void preCoreMethod()

  /**
   * The extension method executed after the core method.
   * @param originalResponse The returned value from the core method.
   * @return The new response (if not null).
   */
  Map postCoreMethod(Map originalResponse)


}