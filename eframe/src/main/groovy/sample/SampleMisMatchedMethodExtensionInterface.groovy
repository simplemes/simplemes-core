/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample


/**
 * The interface to many sample ExtensionPoint tests.  Has wrong method for the postCoreMethod.  Used to test error checking.
 */
@SuppressWarnings("unused")
interface SampleMisMatchedMethodExtensionInterface {

  void preCoreMethod()

  void postCoreXMethod(Map originalResponse)

  void preCoreMethod2()

  void postCoreMethod2(String argument1)

  void preCoreMethod3()

  String postCoreMethod3(String response, String arg)

  void preCoreMethod4(Integer arg)

  String postCoreMethod4(String response, Boolean arg)

  void preCoreMethod5()

  Integer postCoreMethod5(String response)


}