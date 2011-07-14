/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* shuts down server after [num] seconds */
function shutdown(num, formname, elemid) {
	var elem = $('#' + elemid);
	var secs=" second";
	var ellipsis="...";
	if (num > 0) {
		if (num != 1) {
			secs+="s";
		}
	    elem.html(num+secs+ellipsis);
		setTimeout('shutdown('+(--num)+', "'+formname+'", "'+elemid+'")',1000);
	} else {
	    $('#' + formname).submit();
	}
}

/* aborts server shutdown and redirects to [target] */
function abort(target) {
    top.location.href=target;
}

/* displays a date in the user's local timezone */
function localTm(time) {
	return (time ? new Date(time) : new Date()).toLocaleString();
}
/* fill in the data */
$(document).ready(function() {
	if(typeof statData == 'undefined') return;
	for(i in statData) {
		var target = $('#' + i);
		if (target.val()) {
			target.val(statData[i]);
		} else {
			target.text(statData[i]);
		}
	}
	var st = statData.shutdownTimer;
	$('#shutdownform').css('display', st ? 'none' : 'block');
	$('#shutdownform2').css('display', st ? 'block' : 'none');
	$('#shutdown_type').val(statData.shutdownType);
});