/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import org.spockframework.runtime.AbstractRunListener
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.ErrorInfo
import org.spockframework.runtime.model.MethodKind

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
        spec.reportFailure(error?.method?.name)
      } catch (Exception ignored) {
        //ignore
      }
    }
  }
}
