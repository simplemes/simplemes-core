package webix

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import io.micronaut.views.View

import javax.annotation.Nullable
import java.security.Principal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
@Controller("/")
class HomeController {

  @Get("/")
  @View("index")
  Map<String, Object> index(@Nullable Principal principal) {
    println "index() principal = $principal"
    Map<String, Object> data = new HashMap<>()
    data.put("loggedIn", principal != null)
    if (principal != null) {
      data.put("username", principal.getName())
    }
    return data
  }


  @Error
  HttpResponse error(HttpRequest request, Exception exception) {
    println "request = $request, $exception"
    exception.printStackTrace()
    return HttpResponse.status(HttpStatus.BAD_REQUEST, exception.toString())
  }
}
