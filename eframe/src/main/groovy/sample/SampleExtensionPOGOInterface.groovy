/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample

import sample.pogo.SampleAlternatePOGO
import sample.pogo.SamplePOGO

/**
 * The interface to many sample ExtensionPoint tests.  POGO argument and return value case.
 */
interface SampleExtensionPOGOInterface {

  /**
   * The extension method executed before the core method.
   * @param argument The argument(s) passed to the core method.
   */
  void preCoreMethod(SamplePOGO argument)

  /**
   * The extension method executed after the core method.
   * @param originalResponse The returned value from the core method.
   * @param argument The argument(s) passed to the core method.
   * @return The new response (if not null).
   */
  SampleAlternatePOGO postCoreMethod(SampleAlternatePOGO originalResponse, SamplePOGO argument)


}