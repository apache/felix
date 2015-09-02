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
function renderData( eventData )  {
	switch(eventData.status) {
		case -1: // no event admin
			$('.statline').html(i18n.stat_no_service);
			$('#scr').addClass('ui-helper-hidden');
			break;
		case  0: // no components
			$('.statline').html(i18n.stat_no_components);
			$('#scr').addClass('ui-helper-hidden');
			break;
		default:
			$('.statline').html(i18n.stat_ok.msgFormat(eventData.status));
			$('#scr').removeClass('ui-helper-hidden');

			tableBody.empty();
			for ( var idx in eventData.data ) {
				entry( eventData.data[idx] );
			}
			if ( drawDetails ) renderDetails(eventData);
			initStaticWidgets();
	}
}

function getEntryId(/* Object */ dataEntry) {
    var id = dataEntry.id;
    if (id < 0) {
        id = dataEntry.name;
        if (dataEntry.pid) {
            id += '/' + dataEntry.pid;
        }
    }
    return id;
}

function entry( /* Object */ dataEntry ) {
	var idPath = getEntryId(dataEntry);
	var id = idPath.replace(/[./-]/g, '_');
	var name = dataEntry.name;
	var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'entry' + id);

	_.find('.bIcon').attr('id', 'img' + id).click(function() {
		showDetails(idPath);
	}).after(drawDetails ? name : ('<a href="' + pluginRoot + '/' + idPath + '">' + name + '</a>'));

	_.find('td:eq(0)').text( dataEntry.id );
	_.find('td:eq(2)').text( dataEntry.state );

	// setup buttons
	if ( dataEntry.stateRaw == -1 ) { // disabled or disabling
		_.find('li:eq(0)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(dataEntry.name, 'enable') });
	} else {
		_.find('li:eq(1)').removeClass('ui-helper-hidden').click(function() { changeDataEntryState(idPath, 'disable') });
	}
	if ( dataEntry.configurable ) _.find('li:eq(2)').removeClass('ui-helper-hidden').click(function() { // configure
		changeDataEntryState(dataEntry.pid, 'configure');
	});	
}

function changeDataEntryState(/* long */ id, /* String */ action) {
	if ( action == 'configure') {
		window.location = appRoot + '/configMgr/' + id;
		return;
	}
	$.post(pluginRoot + '/' + id, {'action':action}, function(data) {
		renderData(data);
	}, 'json');	
}

function showDetails( id ) {
	$.get(pluginRoot + '/' + id + '.json', null, function(data) {
		renderDetails(data);
	}, 'json');
}

function hideDetails( id ) {
	var __test__ = $('#img' + id);
	$('#img' + id).each(function() {
		$('#pluginInlineDetails').remove();
		$(this).
			removeClass('ui-icon-triangle-1-w').//left
			removeClass('ui-icon-triangle-1-s').//down
			addClass('ui-icon-triangle-1-e').//right
		    attr('title', 'Details').
			unbind('click').click(function() {showDetails(id)});
	});
}

function renderDetails( data ) {
	data = data.data[0];
	var id = getEntryId(data).replace(/[./-]/g, '_');
	$('#pluginInlineDetails').remove();
	var __test__ = $('#entry' + id);
	$('#entry' + id + ' > td').eq(1).append('<div id="pluginInlineDetails"/>');
	$('#img' + id).each(function() {
		if ( drawDetails ) {
			var ref = window.location.pathname;
			ref = ref.substring(0, ref.lastIndexOf('/'));
			$(this).
				removeClass('ui-icon-triangle-1-e').//right
				removeClass('ui-icon-triangle-1-s').//down
				addClass('ui-icon-triangle-1-w').//left
				attr('title', 'Back').
				unbind('click').click(function() {window.location = ref});
		} else {
			$(this).
				removeClass('ui-icon-triangle-1-w').//left
				removeClass('ui-icon-triangle-1-e').//right
				addClass('ui-icon-triangle-1-s').//down
				attr('title', 'Hide Details').
				unbind('click').click(function() {hideDetails(id)});
		}
	});
	$('#pluginInlineDetails').append('<table border="0"><tbody></tbody></table>');
	var details = data.props;
	for (var idx in details) {
		var prop = details[idx];
		var key = i18n[prop.key] ? i18n[prop.key] : prop.key; // i18n

		var txt = '<tr><td class="aligntop" noWrap="true" style="border:0px none">' + key + '</td><td class="aligntop" style="border:0px none">';
		if (prop.value) {
			if ( $.isArray(prop.value) ) {
				var i = 0;
				for(var pi in prop.value) {
					var value = prop.value[pi];
					if (i > 0) { txt = txt + '<br/>'; }
					var span;
					if (value.substring(0, 2) == '!!') {
						txt = txt + '<span style="color: red;'> + value + '</span>';
					} else {
						txt = txt + value;
					}
					i++;
				}
			} else {
				txt = txt + prop.value;
			}
		} else {
			txt = txt + '\u00a0';
		}
		txt = txt + '</td></tr>';
		$('#pluginInlineDetails > table > tbody').append(txt);
	}
}

var tableBody = false;
var tableEntryTemplate = false;
var pluginTable = false;

$(document).ready(function(){
	pluginTable = $('#plugin_table');
	tableBody = pluginTable.find('tbody');
	tableEntryTemplate = tableBody.find('tr').clone();

	renderData(scrData);

	$('.reloadButton').click(document.location.reload);

	pluginTable.tablesorter({
		headers: {
			0: { sorter:'digit'},
			3: { sorter: false }
		},
		sortList: [[1,0]],
		textExtraction:mixedLinksExtraction
	});
});

