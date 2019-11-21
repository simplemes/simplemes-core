package org.simplemes.mes.tracking.service

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.ArchiverFactoryInterface
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.mes.tracking.ProductionLogArchiveRequest
import org.simplemes.mes.tracking.ProductionLogRequest
import org.simplemes.mes.tracking.domain.ProductionLog

import javax.inject.Singleton

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This service provided methods to log production activity for long-term tracking.
 * This works with the ProductionLOg domain object to record the important shop floor
 * activity.  This log is typically used for production and yield reports.
 *
 */
@Slf4j
@Singleton
class ProductionLogService {

  /**
   * Logs a given production event.
   *
   * @param request The request to log.
   */
  @Transactional
  void log(ProductionLogRequest request) {
    ArgumentUtils.checkMissing(request, 'request')
    ArgumentUtils.checkMissing(request.action, 'request.action')

    def pl = new ProductionLog()
    pl.action = request.action
    pl.userName = request.user?.userName ?: SecurityUtils.currentUserName
    pl.order = request.order?.order
    pl.lsn = request.lsn?.lsn
    def product = request.product ?: request.order?.product
    pl.product = product?.product
    pl.masterRouting = product?.masterRouting?.routing
    pl.operationSequence = request.operationSequence
    pl.workCenter = request.workCenter?.workCenter
    pl.qty = request.qty
    pl.qtyStarted = request.qtyStarted
    pl.qtyCompleted = request.qtyCompleted
    pl.dateTime = request.dateTime ?: new Date()
    pl.startDateTime = request.startDateTime ?: pl.dateTime
    pl.elapsedTime = pl.dateTime.time - pl.startDateTime.time

    pl.save()
  }

  /**
   * Archives/deletes old records, using the given configuration.
   * <p>
   * <b>Note:</b> This method creates new transactions when archiving each batch of records.
   *              If called from within an existing transaction, then no new transactions will be created.
   *
   *  <h3>Logging</h3>
   * The logging for this class that can be enabled:
   * <ul>
   *   <li><b>info</b> - Performance timing. </li>
   *   <li><b>debug</b> - Logs the configuration used for the archive check. </li>
   *   <li><b>trace</b> - Logs the each batch archived/deleted. </li>
   * </ul>

   *
   * @param request The configuration to use for this archive run.
   * @return The list of archive references, if delete=false.
   */
  @SuppressWarnings("GroovyMissingReturnStatement")
  List<String> archiveOld(ProductionLogArchiveRequest request) {
    def res = []
    ArgumentUtils.checkMissing(request, 'request')
    ArgumentUtils.checkMissing(request.ageDays, 'request.ageDays')

    def start = System.currentTimeMillis()

    // Archive/delete records in small batches/transactions until no more eligible records are found.
    def now = new Date()
    long offset = (long) (DateUtils.MILLIS_PER_DAY * request.ageDays)
    def ageDate = new Date(now.time - offset)
    def batchSize = request.batchSize ?: 500
    log.debug("archiveOld: request = {}, ageDate = {}", request, ageDate)

    long totalCount = 0
    def batchCount = 0
    def archive = !request.delete

    while (true) {
      def recCount = 0
      def archiver

      // Archive one batch, in a transaction.
      ProductionLog.withTransaction { txnStatus ->
        try {
          //println "list1 = ${ProductionLog.list().size()}:${ProductionLog.list()*.dateTime}"
          def recs = ProductionLog.findAllByDateTimeLessThan(ageDate, [max: batchSize])
          recCount = recs.size()
          //println " recs = ${recs*.dateTime}"

          if (recCount) {
            if (archive) {
              def factory = Holders.applicationContext.getBean(ArchiverFactoryInterface)
              archiver = factory.archiver
            }
            for (rec in recs) {
              if (archive) {
                archiver.archive(rec)
              } else {
                rec.delete()
              }
              totalCount++
            }
            batchCount++
            if (archive) {
              def fileRef = archiver.close()
              res << fileRef
              log.trace("archiveOld: archived {} records in batch {} to {}", recs.size(), batchCount, fileRef)
            } else {
              log.trace("archiveOld: deleted {} records in batch {}", recs.size(), batchCount)
            }
          }
        } catch (Exception e) {
          log.error("Exception during Archive.", e)
          LogUtils.logStackTrace(log, e, 'ProductionLogService')
          txnStatus.setRollbackOnly()
          // Force an exit of the loop to avoid re-trying the execution.
          recCount = 0
        }
      }
      if (!recCount) {
        // No more records.
        break
      }
    }

    def elapsedTime = System.currentTimeMillis() - start
    log.info("archiveOld: archived {} records in {} batches in {}ms", totalCount, batchCount, elapsedTime)

    return res
  }

}
