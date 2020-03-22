/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.dashboard.controller

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Requires
import io.micronaut.core.io.Writable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.views.ViewsRenderer
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.BaseController
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.test.UnitTestRenderer
import sample.StartRequest
import sample.StartResponse

import javax.annotation.Nullable
import java.security.Principal

/**
 * Test controller for dashboard testing.  This is only exposed in test mode.
 * Provides ability to display arbitrary dashboard activities and to handle request from the dashboard.
 * Also provides some simple dashboard activities that help with GUI testing.
 * <p>
 * <b>Note:</b> This is only exposed as a controller in Test mode.
 *
 */
@Slf4j
@Secured(SecurityRule.IS_ANONYMOUS)
@Requires(env = ["test", "dev"])
@Controller("/test/dashboard")
@SuppressWarnings("unused")
final class DashboardTestController extends BaseController {

  /**
   * A simple dashboard activity that will display dashboard events (as JSON) as they are received.
   */
  public static final EMPTY_ACTIVITY = '''
      <@efForm id="empty" dashboard=true>
      <@efHTML>
        <h4"">Empty</h4>
      </@efHTML>
      </@efForm>
    '''
  /**
   * A simple dashboard activity that will display dashboard events (as JSON) as they are received.
   */
  public static final DISPLAY_EVENT_ACTIVITY = '''
      <@efForm id="eventDisplay" dashboard=true>
      <@efHTML>
        <h4"">Events</h4>
        <span id="events"></span>
      </@efHTML>
      </@efForm>
      ${params._variable}.handleEvent = function(event) { 
        document.getElementById("events").innerHTML += JSON.stringify(event)+"<br>";
      }
    '''

  /**
   * A simple dashboard activity that will parameters provided by the dashboard when displayed.
   * This is suitable for use as a button activity.
   */
  public static final DISPLAY_PARAMETERS_ACTIVITY = '''
      <@efForm id="parameterDisplay" dashboard=true>
      <@efHTML>
        <h4"">Parameters</h4>
        <span id="parameters">
          <#list .data_model.params?keys as key>
            ${key}=${params[key]}<br>
          </#list>
        </span>
      </@efHTML>
      </@efForm>
    '''

  /**
   * A simple dashboard activity with a single input field that will trigger a dashboard event.
   * The event is created from the content of the input field (JSON).
   * Also displays any events received.
   */
  public static final TRIGGER_EVENT_ACTIVITY = '''
      <@efForm id="eventTrigger" dashboard=true>
        <@efHTML>
          <h4"">Trigger Events</h4>
        </@efHTML>
        <@efField field="eventSource" id="eventSource" maxLength=999/>
        <@efButtonGroup>
          <@efButton id="triggerEvent" label="Trigger" click="${params._variable}.sendEvent()"/>
        </@efButtonGroup>
        <@efHTML>
          <h4"">Events Sent</h4>
          <div id="eventsSent"></div>
          <h4"">Events</h4>
          <div id="events"></div>
        </@efHTML>
      </@efForm>
      ${params._variable}.handleEvent = function(event) { 
        document.getElementById("events").innerHTML += JSON.stringify(event)+"<br>";
      }
      ${params._variable}.sendEvent = function() {
        var s = document.getElementById("eventSource").value;
        document.getElementById("eventsSent").innerHTML += s+"<br>";
        var event = JSON.parse(s);  
        dashboard.sendEvent(event);
      }
      
    '''


  /**
   * Serves up a page for the dashboard testers. This is served from an .ftl file.
   *
   * <h3>Parameters</h3>
   * The HTTP Get parameters supported:
   * <ul>
   *   <li><b>view</b> - The view to display (e.g. 'sample/dashboard/wcSelection').
   *   </li>
   * </ul>
   * @param request The request.
   * @param principal
   * @return The page.
   */
  @Get("/page")
  @Produces('application/javascript')
  HttpResponse page(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    if (!params.view) {
      throw new IllegalArgumentException('Missing parameter view')
    }
    def modelAndView = new StandardModelAndView((String) params.view, principal, this)
    modelAndView.model.get().put('timeStamp', System.currentTimeMillis().toString())
    def renderer = Holders.applicationContext.getBean(ViewsRenderer)
    Writable writable = renderer.render(modelAndView.view.get(), modelAndView.model.get())
    return HttpResponse.status(HttpStatus.OK).body(writable)
  }

  /**
   * Serves up a page from static memory for the dashboard testers. See setMemoryPage.
   *
   * <h3>Parameters</h3>
   * The HTTP Get parameters supported:
   * <ul>
   *   <li><b>page</b> - The page number from the static memory.
   *   </li>
   * </ul>
   * @param request The request.
   * @param principal
   * @return The page.
   */
  @SuppressWarnings("unused")
  @Get("/memory")
  @Produces('application/javascript')
  HttpResponse memory(HttpRequest request, @Nullable Principal principal) {
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    if (!params.page) {
      throw new IllegalArgumentException('Missing parameter page')
    }

    def options = [source   : memory[(String) params.page], controllerClass: DashboardTestController,
                   uri      : '/test/dashboard/memory',
                   dataModel: [params: params, timeStamp: System.currentTimeMillis().toString()]]
    log.trace('memory(): options {}', options)
    def s = new UnitTestRenderer(options).render()
    return HttpResponse.status(HttpStatus.OK).body(s)
  }

  /**
   * The static memory holder for unit test pages.
   */
  static Map<String, String> memory = [:]

  /**
   * Utility method to clear any memory pages.
   */
  static void clearMemoryPages() {
    memory = [:]
  }

  /**
   * Sets the content of the memory pages available.  Used for testing dashboard logic only.
   * @param page The page ID.
   * @param content The page content.
   */
  static void setMemoryPages(String page, String content) {
    memory[page] = content
  }

  /**
   * Sample test method to process a POST request with a JSON body and echo the JSON in the response as a message.
   * <p>
   * <h3>Options</h3>
   * These options can be passed in to trigger specific behavior.
   * <ul>
   *   <li><b>throwException</b> - Throws an exception. </li>
   * </ul>
   * @param request The HTTP Request.  The Body is the JSON needed to create the record.
   * @param principal The user logged in.
   * @return A message holder with the content as a message.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Post("/echo")
  HttpResponse echo(HttpRequest request, @Nullable Principal principal) {
    String body = null
    if (request.body) {
      body = request.body.get()
      log.debug("echo() body {}", body)
      def values = Holders.objectMapper.readValue(body, Map)
      if (values.throwException) {
        throw new IllegalArgumentException((String) values.throwException)
      }
    }
    def holder = new MessageHolder(level: MessageHolder.LEVEL_INFO, text: body)
    return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(holder))
  }

  /**
   * Some internal counters to use for undo testing.  Can be incremented/decremented.
   * Contains a start and undo counter for each 'order' start/undo used.
   */
  static Map startCounters = [:]

  /**
   * Clears the in memory counters for testing purposes.
   */
  static void clearStartCounters() {
    startCounters = [:]
  }

  /**
   * Sample test method to process a POST request for a sample start.  Updates a counter
   * <p>
   * @param request The HTTP Request.  A JSON start request.
   * @param principal The user logged in.
   * @return A message holder with the content as a message.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Post("/start")
  HttpResponse start(HttpRequest request, @Nullable Principal principal) {
    String body = request.body.get()
    log.debug("echo() body {}", body)
    def params = ControllerUtils.instance.convertToMap(request.parameters)
    def startRequest = Holders.objectMapper.readValue(body, StartRequest)
    Integer counter = (Integer) startCounters[startRequest.order] ?: Integer.valueOf(0)
    counter++
    startCounters[startRequest.order] = counter
    def response = new StartResponse(order: startRequest.order, counter: counter)
    def s
    if (params.listResponse) {
      // Wrap the response in a list to simulate multiple response from the server.
      s = Holders.objectMapper.writeValueAsString([response])
    } else {
      s = Holders.objectMapper.writeValueAsString(response)
    }
    return HttpResponse.status(HttpStatus.OK).body(s)
  }

  /**
   * Sample test method to process a POST request for a sample undo start.  Updates a counter
   * <p>
   * @param request The HTTP Request.  A JSON undoStart request.
   * @param principal The user logged in.
   * @return A message holder with the content as a message.
   */
  @SuppressWarnings("unused")
  @Secured(SecurityRule.IS_ANONYMOUS)
  @Produces(MediaType.APPLICATION_JSON)
  @Post("/undoStart")
  HttpResponse undoStart(HttpRequest request, @Nullable Principal principal) {
    String body = request.body.get()
    log.debug("echo() body {}", body)
    def startRequest = Holders.objectMapper.readValue(body, StartRequest)
    Integer counter = (Integer) startCounters[startRequest.order] ?: Integer.valueOf(0)
    counter--
    startCounters[startRequest.order] = counter
    def holder = new MessageHolder(level: MessageHolder.LEVEL_WARN, text: "UndoStart on order '$startRequest.order', counter=$counter")
    return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(holder))
/*
    def response = new StartResponse(order: startRequest.order,counter: counter)
    //println "response = $response"
    return HttpResponse.status(HttpStatus.OK).body(Holders.objectMapper.writeValueAsString(response))
*/
  }


}
