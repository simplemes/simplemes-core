/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation;

import io.micronaut.context.ApplicationContext;
import org.simplemes.eframe.ast.ASTUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Provides helper methods for the ExtensionPoint annotation.
 */
public class ExtensionPointHelper {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  protected static ExtensionPointHelper instance = new ExtensionPointHelper();

  /**
   * Invokes all pre method extensions for the given class.
   *
   * @param interfaceClass The interface class.  All beans executed will implement this interface.
   * @param methodName     The name of the core method (assumes 'pre' is already be added to the core method name).
   * @param arguments      The runtime arguments from the original method call.
   */
  void invokePre(Class interfaceClass, String methodName, Object... arguments) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    @SuppressWarnings("unchecked")
    Collection beans = getApplicationContext().getBeansOfType(interfaceClass);
    for (Object bean : beans) {
      Class<?> clazz = bean.getClass();
      Class[] parameterTypes = new Class[arguments.length];
      for (int i = 0; i < arguments.length; i++) {
        parameterTypes[i] = arguments[i].getClass();
      }
      //System.out.println("methods:" + Arrays.toString(clazz.getDeclaredMethods()));
      Method method = ASTUtils.findMethod(clazz, methodName, parameterTypes);
      // Force ability to access the method.  The micronaut generated class is protected, so we need to bypass the accessible checks.
      method.setAccessible(true);
      method.invoke(bean, arguments);
    }
  }

  /**
   * Invokes all post method extensions for the given class.
   *
   * @param interfaceClass The interface class.  All beans executed will implement this interface.
   * @param methodName     The name of the core method (assumes 'post' is already be added to the core method name).
   * @param response       The response from the core method.  If null, then void is assumed.
   * @param arguments      The runtime arguments from the original method call.
   * @return The possibly altered response from the extension(s).
   */
  Object invokePost(Class interfaceClass, String methodName, Object response, Object... arguments) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    @SuppressWarnings("unchecked")
    Collection beans = getApplicationContext().getBeansOfType(interfaceClass);
    for (Object bean : beans) {
      Class<?> clazz = bean.getClass();
      int adjustParamCount = 0;
      if (response != null) {
        adjustParamCount = 1;
      }
      Object[] argumentsPlusResponse = new Object[arguments.length + adjustParamCount];
      Class[] parameterTypes = new Class[arguments.length + adjustParamCount];
      if (response != null) {
        parameterTypes[0] = response.getClass();
        argumentsPlusResponse[0] = response;
      }
      for (int i = 0; i < arguments.length; i++) {
        parameterTypes[i + adjustParamCount] = arguments[i].getClass();
        argumentsPlusResponse[i + adjustParamCount] = arguments[i];
      }
      Method method = ASTUtils.findMethod(clazz, methodName, parameterTypes);
      // Force ability to access the method.  The micronaut generated class is protected, so we need to bypass the accessible checks.
      method.setAccessible(true);
      Object methodResponse = method.invoke(bean, argumentsPlusResponse);
      if (methodResponse != null) {
        // Extension wanted to alter the response, so us it for the next execution.
        response = methodResponse;
      }
    }
    return response;
  }

  /**
   * A cached context.
   */
  ApplicationContext applicationContext;

  /**
   * Clear any cached objects.  Mainly used for testing.
   */
  public static void clearCaches() {
    instance.applicationContext = null;
  }

  /**
   * Get the application context from the holders.
   *
   * @return The context.
   */
  public ApplicationContext getApplicationContext() {
    if (applicationContext == null) {
      applicationContext = (ApplicationContext) ASTUtils.invokeGroovyMethod("org.simplemes.eframe.application.Holders", "getApplicationContext");
    }
    return applicationContext;
  }


}
