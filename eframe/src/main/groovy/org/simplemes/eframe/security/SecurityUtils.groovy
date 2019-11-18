package org.simplemes.eframe.security

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import org.simplemes.eframe.application.Holders

import java.security.Principal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Utility methods to access the system's security features.
 */
@Slf4j
class SecurityUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static SecurityUtils instance = new SecurityUtils()


  /**
   * The generic test user for the test mode return values for method in this class.  Value: 'TEST'.
   */
  public static final String TEST_USER = 'TEST'

  /**
   * The generic test user for the API tests.  ('admin').
   */
  public static final String API_TEST_USER = 'admin'

  /**
   * The generic test user for the GUI tests.  ('admin').
   */
  public static final String GUI_TEST_USER = 'admin'

  /**
   * If true, then in test mode, there us no simulated unit test user.
   * This allows unit tests to simulate no user logged in.
   */
  static boolean simulateNoUserInUnitTest = false

  /**
   * The user to return as current user.  This is only allow in unit tests.
   */
  static String currentUserOverride


  // TODO: Move to singleton access.
  /**
   * A Convenience method to return the user name for the user currently logged in for the current HTTP request.
   * @return The user name.  Can be null.
   */
  static String getCurrentUserName() {
    if (Holders.environmentTest) {
      if (simulateNoUserInUnitTest) {
        return null
      }
      if (currentUserOverride) {
        return currentUserOverride
      }
    }

    Optional<Principal> principal = Holders.currentRequest?.userPrincipal
    if (principal) {
      return principal.get().name
    }

    return null
  }

  /**
   * Verifies that the sub-class @Secure annotation is valid for this given user principal.
   * Makes sure the user has at least one of the roles required by the parent controller sub-class.
   * <p>
   * <b>Note:</b> This should only be used by controller super-classes.
   * <p>
   * This works around an issue with the core Micronaut security not checking the sub-class's @Secured annotation
   * for methods provided by the parent class.
   * @param controller The controller class.
   * @param principal The user principal.
   * @returns The forbidden response if the user does not have the correct role.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  HttpResponse checkRoleFromSubClass(Object controller, Principal principal) {
    def roles
    if (principal?.hasProperty('attributes')) {
      roles = principal?.attributes?.roles ?: []
    } else {
      // Some unit tests don't have the attributes that the AuthenticationJWTClaimsSetAdapter does.
      roles = []
    }
    def clazz = controller.class
    def annotation = clazz.getAnnotation(Secured)
    if (!annotation) {
      log.error('checkRoleFromSubClass() No @Secured annotation on controller {}.  You must define security on all controllers.',
                clazz)
      return HttpResponse.status(HttpStatus.FORBIDDEN)
    }

    for (requiredRole in annotation.value()) {
      if (roles.contains(requiredRole)) {
        // User has at least one role.
        log.trace('checkRoleFromSubClass() Found role {} for controller {}', requiredRole, clazz)
        return null
      } else if (requiredRole == SecurityRule.IS_ANONYMOUS) {
        log.trace('checkRoleFromSubClass() Is Anonymous() found for controller {}', clazz)
        return null
      } else if (requiredRole == SecurityRule.IS_AUTHENTICATED) {
        if (principal) {
          log.trace('checkRoleFromSubClass() Is Authenticated() found for controller {}', clazz)
          return null
        }
      }
    }

    log.trace('checkRoleFromSubClass() No role found matching {} for controller {}', roles, clazz)
    return HttpResponse.status(HttpStatus.FORBIDDEN)
  }

  /**
   * Returns true if any role in the given list is not granted to the given user.
   * @param roles The roles (comma-delimited list)
   * @param principal The user principal.
   * @return False if any of the roles is not granted.
   */
  boolean isAllGranted(String roles, Principal user) {
    if (!roles) {
      // No input required roles, so assume the user is allowed.
      return true
    }

    def grantedRoles
    if (user?.hasProperty('attributes')) {
      grantedRoles = user?.attributes?.roles ?: []
    } else {
      // Some unit tests don't have the attributes that the AuthenticationJWTClaimsSetAdapter does.
      grantedRoles = []
    }

    // See if any required roles are missing for the given user.
    def requiredRoles = roles.split(',')
    def missing = requiredRoles.findAll { !grantedRoles.contains(it) }
    return !missing
  }
}
