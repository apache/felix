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
function renderFactoryDetails(data)  {
    $(".statline").html(getFactoriesStatLine(data));
    createDetail( data.data );
    //$("#plugin_table").trigger("update");
}

function getFactoriesStatLine(factories) {
    return factories.count + " factories in total, "
        + factories.valid_count + " valid factories, "
        + factories.invalid_count + " invalid factories.";
}

function createDetail(factory) {
    console.log("Create details");
    var name = factory.name;
    var state = factory.state;
    var bundle = factory.bundle;
    var service = "No provided services"
    
    console.log("Create entry : " + factory);

    var _ = tableBody;
    
    // Set the name
    _.find('td.Vname').html(factory.name);
    // Set the bundle //TODO we can create a link here ?
    _.find('td.Vbundle').text(factory.bundle);
    // Set the state
    _.find('td.Vstate').text(factory.state);
    
    // Set the services
    // If define, use a list
    if (factory.services) {
        var list = $('<ul>');
        for (var s in factory.services) {
            list.append($('<li>').append(factory.services[s]));
        }
        _.find('td.Vservices').html(list);
    } else { // Undefined
        _.find('td.Vservices').html('<i>No provided service specifications</i>');
    }
    
    // Set the properties
    $(tablePropBody).empty();
    if (factory.properties) {
        for (var s in factory.properties) {
            var prop = factory.properties[s];
            // For each properties clone the template
            var entry = propEntryTemplate.clone().appendTo(tablePropBody).attr('id', 'property-' + prop.name);
            entry.find('td.name').text(prop.name);
            entry.find('td.type').text(prop.type);
            entry.find('td.mandatory').text(prop.mandatory);
            entry.find('td.immutable').text(prop.immutable);
            if (prop.value) {
                entry.find('td.value').text(prop.value);
            } else {
                entry.find('td.value').html('<i>No default value</i>');
            }
        }
    } else {
        // Hide the table
        $('table.properties').hide();
    }
        
    // Set the required handlers.
    if (factory.requiredHandlers) {
        var list = $('<ul>');
        for (var s in factory.requiredHandlers) {
            list.append($('<li>').append(factory.requiredHandlers[s]));
        }
        _.find('td.VrequiredHandlers').html(list);
    } else { // Undefined
        _.find('td.VrequiredHandlers').html('<i>No required handlers</i>');
    }
    
    // Set the missing handlers.
    if (factory.missingHandlers) {
        var list = $('<ul>');
        for (var s in factory.missingHandlers) {
            list.append($('<li>').append(factory.missingHandlers[s]));
        }
        _.find('td.VmissingHandlers').html(list);
    } else { // Undefined
        _.find('td.VmissingHandlers').html('<i>No missing handlers</i>');
    }
    
    // Set the created instances.
    if (factory.instances) {
        var list = $('<ul>');
        for (var s in factory.instances) {
            var link = $('<a href=\'' + instances_url + '/' + factory.instances[s] +'\'>' + factory.instances[s] + '</a>');
            list.append($('<li>').append(link));
        }
        _.find('td.VcreatedInstances').html(list);
    } else { // Undefined
        _.find('td.VcreatedInstances').html('<i>No created instances</i>');
    }

    _.find('pre.architecture_content').text(factory.architecture);
}

function retrieveDetails() {
    $.get(pluginRoot + '/factories/' + factory_name + '.json', null, function(data) {
        renderFactoryDetails(data);
    }, "json");
}

function loadFactoriesData() {
    window.location = factories_url;	
}

function loadInstancesData() {
    window.location = instances_url;
}

function loadHandlersData() {
    window.location = handlers_url;
}

var tableBody = false;
var tablePropBody = false;
var propEntryTemplate = false;

$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
    
    tablePropBody = $('.properties tbody');
    propEntryTemplate = tablePropBody.find('tr').clone();
    
    retrieveDetails();
	
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
	
});

