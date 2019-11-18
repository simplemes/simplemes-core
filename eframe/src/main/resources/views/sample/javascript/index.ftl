<#assign title>Javascript Tester</#assign>
<#include "../../includes/header.ftl" />

<div id="mainDiv"></div>
<div id="result"></div>


<br><br>
<label for="d">script</label>
<textarea id="d" rows="10" cols="70" spellcheck="false">${params.s!''}</textarea>
<button onClick="executeScript();">Execute</button>

<!--suppress JSUnusedLocalSymbols -->
<script>
  // A global variable to hold results of tests.
  var holder = {};

  // Displays a single value in the result div.
  function displayResult(value) {
    document.getElementById("result").innerHTML = value;
  }

  // Reset all display fields/messages.
  function reset() {
    document.getElementById("result").innerHTML = '';
    document.getElementById("messages").innerHTML = '';
  }

  function executeScript() {
    var s = document.getElementById('d').value;
    console.log(s);
    eval(s);
  }
</script>
<br>
<h3>Data Model keys</h3>
<#list .data_model?keys as key>
    ${key}
</#list>


<#include "../../includes/footer.ftl" />


