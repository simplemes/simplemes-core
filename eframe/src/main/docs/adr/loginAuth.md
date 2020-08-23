## Login Authorization Design Decisions

The core Micronaut system supports basic login/authorization logic.  The enterprise framework supports
this logic, but a few non-standard decisions were made to make development simpler.

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

