/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import groovy.util.logging.Slf4j
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Error
import io.micronaut.security.authentication.AuthorizationException
import io.micronaut.views.ViewsRenderer
import net.sf.jasperreports.engine.JRRuntimeException
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.controller.GlobalErrorController
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.misc.HTMLUtils
import org.simplemes.eframe.misc.LogUtils

import java.lang.reflect.InvocationTargetException
import java.security.Principal

/**
 * Provides the standard error handler definition for a normal controller.
 * This sends a response from the controller with a JSON encoded MessageHolder as the response
 * content.
 *
 * <b>Note:</b> This is a base class instead of the easier to use than a trait.  For some reason, the groovy compiler fails
 *       with an error 'canonicalization NoSuchMethodException: java.lang.annotation.Annotation.equals()'
 *       during compilation with the trait.   So, we clone this in the local project to avoid the issue.  The problem
 *       is traced to the use of @Error in a trait.  Groovy states that traits and AST transforms aren't guaranteed to work.

 */
@Slf4j
@SuppressWarnings("unused")
abstract class BaseController {

  /**
   * The most recent error message returned to the client.  This is mainly used in
   * unit tests since the Micronaut client throws an exception when a BAD_REQUEST status is returned.
   * There is no way to detect the real exception message while in a unit test.
   */
  protected MessageHolder lastErrorMessage

  /**
   * Handles uncaught exceptions in this controller.
   * Builds a generic HTML page for HTML requests.  Otherwise, returns a BAD_REQUEST with
   * the message.
   * @param request The request.
   * @param throwable The exception.
   * @return A response based on the request type (Accept header).
   */
  @Error
  HttpResponse error(HttpRequest request, Throwable throwable) {
    if (log.debugEnabled) {
      log.debug("Throwable caught by controller")
    }
    try {
      if (log.traceEnabled) {
        log.debug("Throwable.stacktrace = {}", throwable.stackTrace)
        log.debug("Throwable.toString() = {}", throwable.toString())
      }
      if (throwable instanceof AuthorizationException) {
        // Use the standard login re-direct.
        return GlobalErrorController.buildLoginRedirect(request)
      }
      if (throwable.cause) {
        throwable = throwable.cause
      }
      if (throwable instanceof InvocationTargetException) {
        if (throwable.hasProperty('target')) {
          //noinspection GroovyAccessibility
          throwable = throwable.target
        }
      }
      if (Holders.environmentDev || Holders.environmentTest) {
        LogUtils.logStackTrace(log, throwable, null)
        if (throwable instanceof IllegalArgumentException) {
          // Trigger an infinite exception case for a special exception.
          if (throwable.toString().contains('an infinite exception loop')) {
            throw new IllegalArgumentException('simulated error handler exception - possible infinite loop - test mode only')
          }
        }
      }
      def accept = request.headers?.get(HttpHeaders.ACCEPT)
      if (accept?.contains(MediaType.TEXT_HTML)) {
        def modelAndView = new StandardModelAndView('home/error', null, this)
        modelAndView[StandardModelAndView.FLASH] = throwable.toString()
        if (Holders.environmentDev || Holders.environmentTest) {
          modelAndView[StandardModelAndView.FLASH_DETAILS] = HTMLUtils.formatExceptionForHTML(throwable)
        }
        def renderer = Holders.applicationContext.getBean(ViewsRenderer)
        Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())

        return HttpResponse.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(writable)
      } else {
        def holder = new MessageHolder(text: throwable.toString())
        //println "(base) throwable = $throwable"
        log.trace("error(): Handling exception {}", throwable)
        return HttpResponse.status(HttpStatus.BAD_REQUEST).body(Holders.objectMapper.writeValueAsString(holder))
      }
    } catch (Throwable ex) {
      if (log.debugEnabled) {
        log.debug("Exception caught while trying to return as HTTP response. ")
      }
      // Make sure this error handler doesn't throw an infinite loop of exceptions.
      def msg = "Unknown - Exception thrown during error handling."
      def exceptionClass = '???'
      try {
        exceptionClass = ex?.getClass()
        def s = ex.toString()
        msg = """
          {
            "message": {
             "level": "error",
             "text": "Infinite exception loop detected: $s",
             "otherMessages": [ ]
            }
          }      
        """
        LogUtils.logStackTrace(log, ex, null)
      } catch (Throwable ignored) {
        if (log.debugEnabled) {
          //noinspection GroovyUnusedCatchParameter
          try {
            if (throwable instanceof JRRuntimeException) {
              log.trace("Original exception is JRRuntimeException")
              log.trace("Original messageKey ${throwable.messageKey}, ${throwable.args}")
              log.trace("Original stack ${throwable.stackTrace}")
            }
            log.debug("Exception caught while trying print nested exception. $exceptionClass.  ")
          } catch (Throwable ignored2) {
          }
        }
        if (!msg) {
          msg = """
          {
            "message": {
             "level": "error",
             "text": "Infinite exception loop detected: could not call toString() on exception (class = $exceptionClass).",
             "otherMessages": [ ]
            }
          }      
          """
        }
      }
      return HttpResponse.status(HttpStatus.BAD_REQUEST).body(msg)
    }
  }

  /**
   * Builds an HTTP Response with an error message in the standard format.
   * @param msg The error message.
   * @return The response (BAD_REQUEST).
   */
  HttpResponse buildErrorResponse(String msg) {
    def holder = new MessageHolder(text: msg)
    return HttpResponse.status(HttpStatus.BAD_REQUEST).body(Holders.objectMapper.writeValueAsString(holder))
  }

  /**
   * Builds an HTTP Response with the given content object as JSON string.
   * @param content The response content (<b>Default</b>: null).
   * @return The response (Ok).
   */
  HttpResponse buildOkResponse(Object content = null) {
    if (content) {
      return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(content))
    } else {
      return HttpResponse.status(HttpStatus.OK)
    }
  }

  /**
   * Builds an HTTP Forbidden Response to with an error message in the standard format.
   * If the Accept parameter indicates the client is a browser (accepts text/html), then the denied page view
   * will be displayed.
   * @param request The request.
   * @param msg The message to display.
   * @param principal The user making the request.
   * @return The response (FORBIDDEN or OK with the denied view rendered).
   */
  static HttpResponse buildDeniedResponse(HttpRequest request, String msg, Principal principal) {
    def accept = request.headers?.get(HttpHeaders.ACCEPT)
    if (accept?.contains(MediaType.TEXT_HTML)) {
      def modelAndView = new StandardModelAndView('home/denied', principal, this)
      modelAndView[StandardModelAndView.FLASH] = msg
      def renderer = Holders.applicationContext.getBean(ViewsRenderer)
      Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())

      return HttpResponse.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML).body(writable)
    } else {
      // Default to JSON
      return HttpResponse.status(HttpStatus.FORBIDDEN)
    }
  }

  /**
   * Parses the request's body as JSON and creates an object of the given clazz from the contents.
   * Uses the standard ObjectMapper for this.
   * @param request The HTTP Request.
   * @param clazz The class of the object the body is expected to contain.
   * @return The body de-serialized as an object.  Null if body is missing.
   */
  Object parseBody(HttpRequest request, Class clazz) {
    def res = null
    String body
    if (request.body) {
      body = request.body.get()
      res = Holders.objectMapper.readValue(body, clazz)
    }

    return res
  }

}