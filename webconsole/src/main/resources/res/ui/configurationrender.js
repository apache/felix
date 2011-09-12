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
var _tabs = false;

function pad(value) {
	if ( value < 10 ) {
		return "0" + value;
	}
	return "" + value;
}

function setSelectedTab() {
	var anchor =  window.location.hash;
	if (anchor) {
		anchor = pluginRoot + '/' + anchor.substring(1) + '.nfo';
		_tabs.find('ul > li > a').each( function(index, element) {
			if (anchor == $(element).data('href.tabs')) {
				_tabs.tabs('select', index);
				return false;
			}
			return true;
		});
	}
}

function downloadDump(ext) {
	var now = new Date();
	var name = "configuration-status-" + now.getUTCFullYear() + pad(now.getUTCMonth() + 1) + pad(now.getUTCDate()) + "-" + pad(now.getUTCHours()) + pad(now.getUTCMinutes()) + pad(now.getUTCSeconds()) + ".";
    location.href = location.href + "/" + name + ext;
}

$(document).ready(function() {
	$(window).bind( 'hashchange', setSelectedTab);
	var dlg = $('#waitDlg').dialog({
		modal    : true,
		autoOpen : false,
		draggable: false,
		resizable: false,
		closeOnEscape: false
	});

	_tabs = $('#tabs').tabs({ajaxOptions: {
		beforeSend : function() { dlg.dialog('open') },
		complete   : function() { dlg.dialog('close')},
	}}).tabs('paging');

	setTimeout(setSelectedTab, 1000);

	$('.downloadTxt').click(function() { downloadDump('txt')});
	$('.downloadZip').click(function() { downloadDump('zip')});
});