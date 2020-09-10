## Login Authorization Design Decisions

The core Micronaut system supports basic login/authorization logic.  The enterprise framework supports
this logic, but a few non-standard decisions were made to make development simpler.

### JWT Access Token Cookie vs. Sessions

This application is a browser-based user application that requires security to access most of the application/data.
This means we need some sort of Authentication mechanism to enforce this.  We have chosen to use JWT Access Cookies
instead of traditional session ID/cookies. Why?

We wanted to have a similar mechanism for REST API access as the browser clients.  We also needed a configurable 
'session' timeout to make the user experience simpler.   This means the administrators will need some way to configure
a session timeout with a wide range of values (15 minutes to 2 weeks).

Once we decided that a long timeout is needed, this made both solutions similar in terms of risk.  A JWT Access/Refresh
Token scheme provides the same risk of leaking access as a long-timeout session mechanism.

One other key criteria is that the session mechanism is stateful.  Some server-side state must be maintained for the session.
We wanted to avoid a stateful server-side session.  We chose to store this state on the client and provide ways to 
extend access without the user re-logging in before the 'session' timeout. 

One other key risk reduction is to use single use JWT Refresh Token cookies.  
Each refresh token is replaced after each use.  This prevents two clients from using the same refresh token at the same 
time.  If a token is used twice, then all refresh tokens for that user will be invalidated and a message will be logged. 

### JWT Token/Refresh Token Use - SingleUseRefreshToken

Authentication of most requests is via JWT Cookies submitted with the HTTP request.  This simplifies the 
browser-based page interactions and only mildly complicates the REST API usage.

Along with this JWT cookie use for authentication, we also support the use of JWT Refresh tokens using
the a refresh controller _'/login/refresh'_.  This is used in the background to refresh 
the JWT cookie shortly before it is due to expire.  Each request returns a cookie 
(_Silent-JWT-Refresh_) to tell the browser page logic (_eframe.js_) to request a token refresh after this amount
of time.

This mechanism uses an iframe refresh to update the JWT access cookie when it is about to timeout.  
This refreshes the access token and updates to a new refresh token. 
This approach allows a user to have multiple tabs open in a single browser or to use multiple browsers/clients to 
access the server over HTTP. 

REST API clients will need to use explicitly request the new access cookie as needed.

Part of the security protocol makes use of a _SingleUseRefreshToken_ instead of the normal re-usable tokens.  This
reduces the chance of a refresh token being leaked to un-authorized users.  This dooes complicate the client-side
refresh logic.  The browser page logic (_eframe.js_) uses some local storage settings top reduce the chance of two 
legitimate browser pages from requesting a refresh from the same token.  

If a second attempt is made to re-use a token, then a warning is logged and all tokens for the user are revoked.
 
 

### Base Class BaseCruController and Secure Annotations

Since the framework uses base classes to implement CRUD and REST logic for most controllers, we have
to use an alternative to the normal Micronaut login/redirect logic.   To make this work, we use 
the `GlobalErrorController` to catch attempts to access forbidden pages.  This error handler will 
redirect the user to the _/login/auth_ page to make it easier for the user to login. 

See the _Controller Security_ section of the [documentation](https://simplemes.github.io/simplemes-core/) for details.

We attempted to use the Micronaut login/redirect logic, but it does not support use of the _@Secured_ annotation 
at the sub-class level.   Our use of the _BaseCrudController_ parent class to implement common logic prevents
the use of @Secured annotation on those implementation methods (e.g. `index()`, `list()`, `show()`, etc).
 
Any controller that does not sub-class _BaseController_ will trigger the standard Micronaut redirect logic for login.
Sub-classes of _BaseController_ bypass this due to the exception handler in _BaseController_.    
This means the framework handles the redirect itself.  See the `error()` method in _BaseController_.

