//import com.machinepublishers.jbrowserdriver.JBrowserDriver
//import com.machinepublishers.jbrowserdriver.Timezone
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions

import java.util.logging.Level
import java.util.logging.Logger

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines the GEB testing environments supported.
 */
System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true")
System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "build\\browser.log")
Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING)
// https://github.com/MachinePublishers/jBrowserDriver
environments {

/*
  // run via “./gradlew -Dgeb.env=jbrowser test”
  jbrowser {
    driver = {
      new JBrowserDriver(com.machinepublishers.jbrowserdriver.Settings.builder().
                           timezone(Timezone.AMERICA_NEWYORK).logTrace(false).build())
    }
  }
*/

  // run via “./gradlew -Dgeb.env=chrome test”
  chrome {
    ChromeOptions o = new ChromeOptions()
    o.addArguments("--lang=${System.getProperty('geb.lang') ?: 'en-US'}")
    //new ChromeDriver(o)
    driver = { new ChromeDriver(o) }
  }

  // run via “./gradlew -Dgeb.env=chromeHeadless test”
  chromeHeadless {
    driver = {
      ChromeOptions o = new ChromeOptions()
      o.addArguments('headless')
      new ChromeDriver(o)
    }
  }

  firefoxHeadless {
    driver = {
      FirefoxOptions o = new FirefoxOptions()
      o.addArguments('-headless')
      new FirefoxDriver(o)
    }
  }

  // run via “./gradlew -Dgeb.env=firefox test”
  // Option for language -Dgeb.lang=de-DE  (Default en-US)
  firefox {
    FirefoxOptions options = new FirefoxOptions()
    //options.addArguments(["--lang=de-DE"])
    options.addPreference('intl.accept_languages', System.getProperty('geb.lang') ?: 'en-US')
    driver = { new FirefoxDriver(options) }
  }
}


reportsDir = "build/geb-reports"