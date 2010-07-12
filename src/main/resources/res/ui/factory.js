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
function renderFactoriesData(factories)  {
    $(".statline").html(getFactoriesStatLine(factories));
    tableBody.empty();
    for ( var idx in factories.data ) {
        factoriesEntry( factories.data[idx] );
    }
    $("#plugin_table").trigger("update");
}

function getFactoriesStatLine(factories) {
    return factories.count + " factories in total, "
        + factories.valid_count + " valid factories, "
        + factories.invalid_count + " invalid factories.";
}

function factoriesEntry(factory) {
    var name = factory.name;
    var state = factory.state;
    var bundle = factory.bundle;

    console.log("Create entry : " + factory);

    var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'factory-' + factory.name);

    _.find('td.name').html('<a href="' + factories_url + '/' + name + '">' + name + '</a>');
    _.find('td.bundle').text(bundle);
    _.find('td.state').text(state);
}


function loadFactoriesData() {
    console.log("Load factories data");
	$.get(pluginRoot + "/factories.json", null, function(data) {
		renderFactoriesData(data);
	}, "json");	
}

function loadInstancesData() {
    window.location = instances_url;
}

function loadHandlersData() {
    window.location = handlers_url;
}

var tableBody = false;
var tableEntryTemplate = false;

$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
	tableEntryTemplate = tableBody.find('tr').clone();

    loadFactoriesData();
	
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

