## GUI/Client Design Decisions

Clean, functional GUIs are critical to any web application's success. SimpleMES is no different.

### Alternatives

Over the years, the development team has investigated and worked with a number of GUI toolkits. These toolkits were
implemented completely and found lacking in some areas:

* Kendo - Javascript Generation of HTML.
* jqWidgets - Javascript Generation of HTML.
* Webix - Javascript Generation of HTML.

These are all great toolkits, but had issues that eventually caused us to look for a better solution.

* Kendo - Closed source. Selenium/Webdriver/Geb testing was unreliable.
* jqWidgets - Selenium/Webdriver/Geb testing was unreliable.
* Webix - Responsive support is limited and API was very non-standard.

### Solution

To overcome these issues, we decided to use Vue with PrimeVue components. This provides the best user experience with a
larger developer base than the other choices.

#### Pros

The main benefits of this solution include:

* Large developer base.
* Extensive tutorials available.
* Responsive UI easily implemented.
* Modern look/feel.

#### Cons

The main drawbacks of this solution include:

* Client development is different from the server-side.  (Javascript/node vs. Java/Groovy).
* Integrating the two environments causes issues.
* Vue makes dynamic, end-user GUIs more difficult.
* Vue makes re-use of code more difficult. Inheritance is discouraged. This means some common features (like
  localization) is copied to each page manually.

### Guidelines

To work around the drawbacks of this approach, some conventions will be followed.

#### Development Process

In particular, all pages will be generated with the Vue/JS world. This means all UI features are built/configured in the
node/JS world. The node/npm world will generate the client assets which will be served up by the server-side.

This means most development will be done while running the npm/VueCLI in development mode, then doing a build to create
the assets for the server-side core to serve.

#### Integration During Development

The npm/VueCLI will run the app in developer mode. The access to the server-side will be to another process on a
different port. This is done to keep the development mode code the same as the production as much as possible.

The axios module use a proxy for development mode to access http://localhost:8080/ while the client code is run
on http://localhost:8081/ 



