package org.simplemes.eframe.test

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.MethodKind

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The failure listener for Spock specifications.  Used to perform actions on failures.
 * This will execute the reportFailure() method, if possible.
 */
class BaseFailureListener extends AbstractRunListener implements IMethodInterceptor {
  private BaseSpecification spec

  void intercept(IMethodInvocation invocation) throws Throwable {
    if (invocation.instance instanceof BaseSpecification) {
      spec = invocation.instance as BaseSpecification
      invocation.proceed()
    }
  }

  void error(ErrorInfo error) {
    if (error.method.kind == MethodKind.FEATURE) {
      try {
        spec.reportFailure()
      } catch (Exception ignored) {
        //ignore
      }
    }
  }
}
