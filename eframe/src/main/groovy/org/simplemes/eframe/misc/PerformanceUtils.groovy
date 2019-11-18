package org.simplemes.eframe.misc

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Utility methods for performance investigations.  Provides simple elapsed timing method.
 *
 */
class PerformanceUtils {

  /**
   * The system time since the last time point.
   */
  static long lastTime = 0

  /**
   * Calculates the elapsed time since the last elapsed time point and prints it to std out.
   * <p>Example Usage:
   * <pre>
   * PerformanceUtils.elapsedPrint()
   *     . . . // Actions to calc elapsed time for
   * PerformanceUtils.elapsedPrint('to UserListPage')
   *   </pre>
   * @param timePoint The name of the time point.  Call with null to start the first elapsed time point.
   * @return A string with the elapsed time since the last point.
   */
  static void elapsedPrint(String timePoint = null) {
    if (lastTime && timePoint) {
      System.out.println(elapsed(timePoint))
    } else {
      elapsed(timePoint)
    }
  }

  /**
   * Calculates the elapsed time since the last elapsed time point.
   * <p>Example Usage:
   * <pre>
   * PerformanceUtils.elapsed()
   *     . . . // Actions to calc elapsed time for
   * println(PerformanceUtils.elapsed('to UserListPage'))
   *   </pre>
   * @param timePoint The name of the time point.  Call with null to start the first elapsed time point.
   * @return A string with the elapsed time since the last point.
   */
  static String elapsed(String timePoint = null) {
    if (timePoint) {
      if (lastTime == 0) {
        throw new IllegalArgumentException("No previous elapsed() was made for $timePoint. Call PerformanceUtils.elasped() to start the timer.")
      }
      def now = System.currentTimeMillis()
      def res = "$timePoint: ${now - lastTime}ms elapsed."
      lastTime = now
      return res
    } else {
      lastTime = System.currentTimeMillis()
      return 'elapsed...'
    }
  }

}
