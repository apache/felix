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


function renderData( subsystemData ) {
	$('.statline').html(i18n.status.msgFormat(subsystemData.data.length));
	
	tableBody.empty();
	for (var idx in subsystemData.data) {
		entry( subsystemData.data[idx] );
	}	

	initStaticWidgets();
}

function entry( dataEntry ) {
	var id = dataEntry.id;
	var name = dataEntry.name;
	var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'entry' + id);
	
	_.find('td:eq(0)').text(id);
	_.find('td:eq(1)').text(name);
	_.find('td:eq(2)').text(dataEntry.version);
	_.find('td:eq(3)').text(dataEntry.state);
	
	// setup buttons
	if (dataEntry.state === "ACTIVE") {
		_.find('li:eq(1)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(id, 'stop') });
	} else {
		_.find('li:eq(0)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(id, 'start') });
	}
	_.find('li:eq(2)').click(function() { changeDataEntryState(id, 'uninstall') });
}

function changeDataEntryState(id, action) {
	$.post(pluginRoot + '/' + id, {'action': action}, function(data) {
		renderData(data);
		
		// This is a total hack, but it's the only way in which I could get the 
		// table to re-sort itself. TODO remove the next line and find a proper way.
		window.location.reload();
	}, 'json');
}

var tableBody = false;
var tableEntryTemplate = false;
var pluginTable = false;
var uploadDialog = false;

$(document).ready(function(){
	$('.installButton').click(function() {
		uploadDialog.dialog('open');
		return false;
	});	
	
	pluginTable = $('#plugin_table');
	tableBody = pluginTable.find('tbody');
	tableEntryTemplate = tableBody.find('tr').clone();
	
	// upload dialog
	var uploadDialogButtons = {};
	uploadDialogButtons["Install"] = function() {
		$(this).find('form').submit();
	}
	uploadDialog = $('#uploadDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '50%',
		buttons : uploadDialogButtons
	});	

	renderData(ssData);

	$('.reloadButton').click(document.location.reload);

	pluginTable.tablesorter({
		headers: {
			0: { sorter:'digit'},
			4: { sorter: false }
		},
		sortList: [[1,0]],
		textExtraction:mixedLinksExtraction
	});
});

