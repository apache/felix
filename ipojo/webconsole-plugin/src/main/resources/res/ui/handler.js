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
function renderHandlersData(handlers)  {
    $(".statline").html(getHandlersStatLine(handlers));
    tableBody.empty();
    for ( var idx in handlers.data ) {
        handlersEntry( handlers.data[idx] );
    }
    $("#plugin_table").trigger("update");
}

function getHandlersStatLine(handlers) {
    return handlers.count + " handlers in total, "
        + handlers.valid_count + " valid handlers, "
        + handlers.invalid_count + " invalid handlers.";
}

function handlersEntry(handler) {
    var name = handler.name;
    var state = handler.state;
    var bundle = handler.bundle;
    var type = handler.type;

    console.log("Create entry : " + handler);

    var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'handler-' + handler.name);

    _.find('td.name').text(name);
    _.find('td.type').text(type);
    _.find('td.bundle').text(bundle);
    _.find('td.state').text(state);
    if (handler.missing) {
        _.find('td.missing').html(handler.missing);    
    } else {
        _.find('td.missing').html('<i>No missing handlers</i>');
    }
    
}


function loadHandlersData() {
    console.log("Load handlers data");
	$.get(pluginRoot + "/handlers.json", null, function(data) {
		renderHandlersData(data);
	}, "json");	
}

function loadInstancesData() {
    window.location = instances_url;
}

function loadFactoriesData() {
    window.location = factories_url;
}

var tableBody = false;
var tableEntryTemplate = false;

$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
	tableEntryTemplate = tableBody.find('tr').clone();

    loadHandlersData();
	
    $(".instancesButton").click(loadInstancesData);
    $(".factoriesButton").click(loadFactoriesData);
    $(".handlersButton").click(loadHandlersData);

	var extractMethod = function(node) {
		var link = node.getElementsByTagName("a");
		if ( link && link.length == 1 ) {
			return link[0].innerHTML;
		}
		return node.innerHTML;
	};
	$("#plugin_table").tablesorter({
		sortList: [[1,0]],
		textExtraction:extractMethod
	});
});

