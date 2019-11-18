package org.simplemes.eframe.test

import java.text.SimpleDateFormat

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A utility class to report on the un-covered lines of a series of coverage reports by package.
 */
class CoverageReportSummary {

  @SuppressWarnings(["GroovyAssignabilityCheck", "SystemOutPrint"])
  static void main(String[] argv) {
    def folders = new File('tmp').listFiles({ it.name.startsWith('coverage') } as FileFilter)
    folders = folders.sort { a, b -> a.lastModified() <=> b.lastModified() }
    //println "folders = $folders"

    // Read each by Line coverage .html file
    def reports = []
    for (folder in folders) {
      def f = new File(folder, 'index_SORT_BY_LINE.html')
      def lines = f.readLines()
      //println "$f lines = ${lines.size()}"

      def sdf = new SimpleDateFormat('MMM dd')
      def header = sdf.format(new Date(folder.lastModified()))

      def oneReport = [fileName: folder.name, header: header]
      reports << oneReport
      for (int i = 0; i < lines.size(); i++) {
        def line = lines[i]
        if (line.contains('<td class="name">')) {
          //    <td class="name"><a href="org.simplemes.eframe.data/index_SORT_BY_LINE.html">org.simplemes.eframe.data</a></td>
          def start = line.indexOf('<a')
          start = line.indexOf('>', start) + 1
          def end = line.indexOf('<', start + 1) - 1
          def name = line[start..end]
          i += 22
          def countLine = lines[i]
          //println "countLine = $countLine"

          def s = countLine.replaceAll(/[\(\)\/]/, '')
          def counts = s.tokenize()
          def covered = Integer.valueOf(counts[0])
          def total = Integer.valueOf(counts[1])
          def unCovered = total - covered
          s = "(${percent(unCovered, total)})"
          oneReport[name] = unCovered
          oneReport["${name}Percent"] = s

          //println "  $name count ${s} "
        }
      }
      //if (reports.size() > 5) {
      //break
      //}
    }
    //println "reports = $reports"
    //for (report in reports) {
    //println "${report.fileName} ${report.header} ${report['all classes']} ${report['all classesPercent']}"
    //}

    // Now, sort the packages in the most recent report by descending row count
    Map lastReport = (Map) reports[reports.size() - 1]
    def keys = lastReport.keySet()
    def m = [:]
    for (key in keys) {
      if (lastReport[key] != null && lastReport[key] instanceof Integer) {
        m[key] = lastReport[key]
      }
    }
    keys = m.keySet()
    //println "keys = $keys"
    def sortedPackageNames = keys.sort { a, b -> lastReport[b] <=> lastReport[a] }
    //for (packageName in sortedPackageNames) {
    //println "  $packageName ${lastReport[packageName]}"
    //}

    def style = """
    <style>
    table {
      font-family: arial, sans-serif;
      border-collapse: collapse;
      width: 100%;
    }
    
    td, th {
      border: 1px solid #dddddd;
      text-align: left;
      padding: 8px;
    }
    
    tr:nth-child(even) {
      background-color: #dddddd;
    }
    </style>
    """

    // Now, write the summary report
    def sb = new StringBuilder()
    sb << """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <title>Coverage Summary Report</title>
        $style
      </head>
      <body>
        <h1>Coverage Summary Report</h1>  
      </body>  
    """

    sb << "<table border=1>\n"

    // Just display the last 15 reports
    def start
    if (reports.size() > 15) {
      start = reports.size() - 15
      reports = reports[start..-1]
    }

    // Check the last report for an increase in the number of un-covered lines.
    def lastReportIndex = reports.size() - 1
    def secondToLastReportIndex = lastReportIndex - 1
    if (lastReportIndex >= 0 && secondToLastReportIndex >= 0) {
      // Check each package for an increase
      for (p in sortedPackageNames) {
        def previousValue = reports[secondToLastReportIndex][p]
        if (previousValue != null) {
          def delta = reports[lastReportIndex][p] - previousValue
          if (delta > 0) {
            reports[lastReportIndex][p] = reports[lastReportIndex][p] + "(+$delta)"
          }
        }
      }
    }


    // Write the headers
    sb << '<th>Package</th>\n'
    for (report in reports) {
      //sb << "$report.header<br>\n"
      sb << "<th>$report.header</th>\n"
    }

    // Now, write each package
    for (p in sortedPackageNames) {
      sb << "<tr>\n"
      sb << "<td>$p</td>\n"
      // Now, write the values for each package in a single row
      for (report in reports) {
        def s = report[p] ?: ''
        if (s) {
          // Turn into a hyperlink
          def href = report.fileName + '/' + p + '/index_SORT_BY_LINE.html'
          if (p == 'all classes') {
            href = report.fileName + '/index_SORT_BY_LINE.html'
          }
          s = "<a href='${href}'>$s</a>"
        }
        sb << "<td>${s}</td>\n"
      }

      sb << "</tr>\n"

    }


    sb << "</table>\n"
    sb << '</body>'

    def file = new File('tmp/summary.html')
    file.text = sb.toString()

    System.out.println "Wrote file:///${file.absolutePath.replaceAll('\\\\', '/')}"
  }

  static String percent(int top, int bottom) {
    def percent = 100.0 * top / bottom

    return "${(int) percent}%"

  }
}
