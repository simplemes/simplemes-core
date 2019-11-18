package org.simplemes.eframe.test

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The global Spock extension used to run the reportFailure() method on the specifications.
 */
class GlobalSpecExtension implements IGlobalExtension {

  @Override
  void start() {
  }

  @Override
  void visitSpec(SpecInfo specInfo) {
    if (BaseSpecification.isAssignableFrom(specInfo.reflection)) {
      def reporter = new BaseFailureListener()
      specInfo.addListener(reporter)
      specInfo.allFeatures*.addIterationInterceptor(reporter)
    }
  }

  @Override
  void stop() {
  }
}
