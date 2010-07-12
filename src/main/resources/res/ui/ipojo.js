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
function renderInstancesData(instances)  {
    $(".statline").html(getInstancesStatLine(instances));
    tableBody.empty();
    for ( var idx in instances.data ) {
        instancesEntry( instances.data[idx] );
    }
    $("#plugin_table").trigger("update");
}

function getInstancesStatLine(instances) {
    return instances.count + " instances in total, "
        + instances.valid_count + " valid instances, "
        + instances.invalid_count + " invalid instances.";
}

function instancesEntry(instance) {
    var name = instance.name;
    var state = instance.state;
    var factory = instance.factory;

    var _ = tableEntryTemplate.clone().appendTo(tableBody).attr('id', 'instance-' + instance.name);

    _.find('td.name').html('<a href="' + instances_url + '/' + name + '">' + name + '</a>');
    _.find('td.factory').html('<a href="' + factories_url + '/' + factory + '">' + factory + '</a>');;
    _.find('td.state').text(state);
}


function loadInstancesData() {
	$.get(pluginRoot + "/instances.json", null, function(data) {
		renderInstancesData(data);
	}, "json");	
}

function loadFactoriesData() {
    window.location = factories_url; 
}

function loadHandlersData() {
    window.location = handlers_url;
}

var tableBody = false;
var tableEntryTemplate = false;

$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
	tableEntryTemplate = tableBody.find('tr').clone();

    loadInstancesData();
	
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

