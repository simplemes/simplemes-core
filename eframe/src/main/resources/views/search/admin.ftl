<#--@formatter:off-->
<#assign title><@efTitle label="searchAdmin.title"/></#assign>

<#include "../includes/header.ftl" />

<@efForm id="search" height="85%">
    <@efField field="status" css="${searchStatus.statusCSSClass}" readOnly=true label="status.label" value='${searchStatus.localizedStatus}' labelWidth="30%"/>
    <@efField field="pendingRequests" readOnly=true label="searchStatus.pendingRequests.label" value='${searchStatus.pendingRequests}' labelWidth="30%"/>
    <@efField field="finishedRequests" readOnly=true label="searchStatus.finishedRequests.label" value='${searchStatus.finishedRequestCount}' labelWidth="30%"/>
    <@efField field="failedRequests" readOnly=true label="searchStatus.failedRequests.label" value='${searchStatus.failedRequests}' labelWidth="30%"/>

    <@efField field="bulkIndexStatus" readOnly=true label="searchStatus.bulkIndexStatus.label" value='${searchStatus.localizedBulkIndexStatus}' labelWidth="40%"/>
    <@efField field="totalBulkRequests" readOnly=true label="searchStatus.totalBulkRequests.label" value='${searchStatus.totalBulkRequests}' labelWidth="40%"/>
    <@efField field="pendingBulkRequests" readOnly=true label="searchStatus.pendingBulkRequests.label" value='${searchStatus.pendingBulkRequests}' labelWidth="40%"/>
    <@efField field="finishedBulkRequests" readOnly=true label="searchStatus.finishedBulkRequests.label" value='${searchStatus.finishedBulkRequests}' labelWidth="40%"/>
    <@efField field="bulkIndexErrorCount" readOnly=true label="searchStatus.bulkIndexErrorCount.label" value='${searchStatus.bulkIndexErrorCount}' labelWidth="40%"/>
    <@efButtonGroup>
        <@efButton id='searchResetCounters' label="searchResetCounters.label" click="resetCounters()" spacer="before after"/>
        <@efButton id='searchRebuildIndices' label="search.rebuild.label" click="rebuildIndices()" spacer="before after"/>
    </@efButtonGroup>
</@efForm>

<@efPreloadMessages codes="search.rebuild.dialog.content,search.rebuild.dialog.title,ok.label,cancel.label"/>
<script>
  var defaultRefreshTime = 60000;
  var rebuildRefreshTime = 2000; // The time to wait between refreshes when rebuilding indices.
  var autoRefreshDelay = defaultRefreshTime;  // The number of ms to wait for the next auto-refresh update of the status.

  // Test mode increases the update frequency for quicker tests.
  // Also displays a counter to detect when update has finished.
  var testMode = '${searchStatus.testMode}';
  if (testMode=='T') {
    defaultRefreshTime = 100;
    rebuildRefreshTime = 100;
    autoRefreshDelay = 100;
  }
  if (testMode=='D') {
    defaultRefreshTime = 5000;
    rebuildRefreshTime = 5000;
    autoRefreshDelay = 5000;
  }

  function resetCounters() {
    eframe.clearMessages();
    ef.post("/search/clearStatistics", {},
      function (responseText) {
        clearTimeout(currentTimeout);
        autoRefreshDelay = rebuildRefreshTime;
        updateStatus();
      }
    );
  }
  function rebuildIndices() {
    eframe.clearMessages();
    ef.displayQuestionDialog({
      title: "search.rebuild.dialog.title", question: ef.lookup('search.rebuild.dialog.content'),
      buttons: ['ok','cancel'],
      ok: function (dialogID,button) {
        ef.post("/search/startBulkIndex", {deleteAllIndices:true},
          function (responseText) {
            // Switch to faster update times and re-start the refresh process.
            clearTimeout(currentTimeout);
            autoRefreshDelay = rebuildRefreshTime;
            updateStatus();
          }
        );
        return true;
      }
    });
  }

  function updateStatus() {
    ef.get("/search/status", {},
      function (responseText) {
        displayStatus(JSON.parse(responseText))
      }
    );
    // Trigger a new refresh.
    currentTimeout = setTimeout(updateStatus, autoRefreshDelay);
  }

  function displayStatus(searchStatus) {
    displayBulkStatusIfPresent(searchStatus.bulkIndexStatus);

    tk._updateFieldValue('status',searchStatus.localizedStatus);
    tk._updateFieldValue('pendingRequests',searchStatus.pendingRequests);
    tk._updateFieldValue('finishedRequests',searchStatus.finishedRequestCount);
    tk._updateFieldValue('failedRequests',searchStatus.failedRequests);
    tk._updateFieldValue('bulkIndexStatus',searchStatus.localizedBulkIndexStatus);
    tk._updateFieldValue('totalBulkRequests',searchStatus.totalBulkRequests);
    tk._updateFieldValue('pendingBulkRequests',searchStatus.pendingBulkRequests);
    tk._updateFieldValue('bulkIndexErrorCount',searchStatus.bulkIndexErrorCount);
    tk._updateFieldValue('finishedBulkRequests',searchStatus.finishedBulkRequests);

    // See if we need to reduce the update interval because the rebuild finished.
    if (searchStatus.bulkIndexStatus == 'completed') {
      autoRefreshDelay = defaultRefreshTime;
    }
  }


  function displayBulkStatusIfPresent(status) {
    if (status == '' || status == undefined) {
      setBulkDisplay(false);
    } else {
      setBulkDisplay(true);
    }
  }

  function setBulkDisplay(display) {
    tk._setFieldDisplayStyle('bulkIndexStatus',display);
    tk._setFieldDisplayStyle('totalBulkRequests',display);
    tk._setFieldDisplayStyle('pendingBulkRequests',display);
    tk._setFieldDisplayStyle('finishedBulkRequests',display);
    tk._setFieldDisplayStyle('bulkIndexErrorCount',display);
  }

  // Utility to stop the periodic update.  Used mainly by GUI tests.
  function disableStatusUpdates() {
    clearTimeout(currentTimeout);
  }

  // Start the periodic update of the the status.
  var currentTimeout = setTimeout(updateStatus, autoRefreshDelay);
  displayBulkStatusIfPresent("${searchStatus.bulkIndexStatus!''}");

</script>
<#include "../includes/footer.ftl" />

