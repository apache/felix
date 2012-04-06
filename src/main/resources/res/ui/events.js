/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var eventsTable = false;

/* displays a date in the user's local timezone */
function printDate(time) {
    var date = time ? new Date(time) : new Date();
    return date.toLocaleString();
}

function renderData( eventData )  {
	$('.statline').html(eventData.status); // FIXME:

	// append table view
	eventsBody.empty();
    for ( var i in eventData.data ) entry( eventData.data[i] );
	eventsTable.trigger('update').trigger('applyWidgets');

	// append timeline view
	timeline.empty();
    for ( var i in eventData.data ) entryTimeline( eventData.data[i] );
}


function entryTimeline( /* Object */ dataEntry ) {
	var txt = '<div class="event event' + dataEntry.category + '" style="width:' + dataEntry.width + '%">' +
		'<b>' + dataEntry.offset + '</b>&nbsp;<b>' + dataEntry.topic + '</b>';
	if ( dataEntry.info )  txt += '&nbsp;:&nbsp;' + dataEntry.info;
    txt += '</div>';
	timeline.prepend(txt);	
}

function entry( /* Object */ dataEntry ) {
    var properties = dataEntry.properties;

    var propE;
    if ( dataEntry.info ) {
    	propE = text(dataEntry.info);
    } else {
	    var bodyE = createElement('tbody');
	    for( var p in dataEntry.properties ) {
	    	bodyE.appendChild(tr(null, null, [ 
				td('propName', null, [text(p)]),
				td('propVal' , null, [text(dataEntry.properties[p])])
			]));
	    }
	    propE = createElement('table', 'propTable', null, [ bodyE ]);
    }

	$(tr( null, { id: 'entry' + dataEntry.id }, [
		td( 'time', null, [ text( printDate(dataEntry.received) ) ] ),
		td( 'topic', null, [ text( dataEntry.topic ) ] ),
		td( 'detailes', null, [ propE ] )
	])).appendTo(eventsBody);
}

var timeline = false;
var timelineLegend = false;
$(document).ready(function(){
	eventsTable = $('#eventsTable');
	eventsBody  = eventsTable.find('tbody');
	timeline = $('#timeline');
	timelineLegend = $('#timelineLegend');

	$('#clear').click(function () {
		$.post(pluginRoot, { 'action':'clear' }, renderData, 'json');
	});
	$('#switch').click(function() {
		var timelineHidden = timeline.hasClass('ui-helper-hidden');
		if (timelineHidden) {
			$(this).text(i18n.displayList);
			timeline.removeClass('ui-helper-hidden');
			timelineLegend.removeClass('ui-helper-hidden');
			eventsTable.addClass('ui-helper-hidden');
		} else {
			$(this).text(i18n.displayTimeline);
			timeline.addClass('ui-helper-hidden');
			timelineLegend.addClass('ui-helper-hidden');
			eventsTable.removeClass('ui-helper-hidden');
		}
	});
	$('#reload').click(function() {
		$.get(pluginRoot + '/data.json', null, renderData, 'json');
	}).click();

	function sendData(action) {
		// check topic
		var topic = sendTopic.val();
		var topicOk = topic.match(/^[\w-]+(\/[\w-]+)*$/g) != null;
		if (topicOk) {
			sendTopic.removeClass('ui-state-error');
		} else {
			sendTopic.addClass('ui-state-error');
		}
		var data = sendProperties.propeditor('serialize');
		if (topicOk && typeof data != 'boolean') {
			$.post(pluginRoot,
				data.concat([
					{name : 'action', value : action},
					{name : 'topic', value : topic}
				]),
				renderData,
				'json'
			);
			sendDialog.dialog("close");
		}
	}

	/* send dialog code */
	var sendButtons = {};
	sendButtons[i18n.close] = function() {
		$(this).dialog("close");
	}
	sendButtons[i18n.reset] = function() {
		sendTopic.val('');
		sendProperties.propeditor('reset');
	}
	sendButtons[i18n.send] = function() {
		sendData('send');
	}
	sendButtons[i18n.post] = function() {
		sendData('post');
	}
	var sendDialog = $('#sendDialog').dialog({
		autoOpen: false,
		modal   : true,
		width   : '40%',
		buttons : sendButtons,
		open    : function() {
			//sendTopic.val('');
			//sendProperties.propeditor('reset');
		}
	});
	var sendTopic = $('#sendTopic');
	var sendProperties = $('#sendProperties').propeditor({
		add: function(el) {
			el.find('select').addClass('dynhover');
			initStaticWidgets(el);
		}
	});
	$('#sendButton').click(function() {
		sendDialog.dialog('open');
	});

});
