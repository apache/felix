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
function renderInstanceDetails(data)  {
    $(".statline").html(getInstancesStatLine(data));
    createDetail( data.data );
}

function getInstancesStatLine(instances) {
    return instances.count + " instances in total, "
        + instances.valid_count + " valid instances, "
        + instances.invalid_count + " invalid instances.";
}

function createDetail(instance) {
    console.log("Create details");
    var service = "No provided services"
    
    var _ = tableBody;
    
    // Set the name
    _.find('td.Vname').html(instance.name);
    
    // Set the state
    _.find('td.Vstate').text(instance.state);
    
    // Set the factory
    _.find('td.Vfactory').html('<a href="' + factories_url + '/' + instance.factory + '">' + instance.factory + '</a>');
    
    if (instance.services) {
        $(tableServiceBody).empty();
        for (var s in instance.services) {
            var service = instance.services[s];
            // For each service clone the template
            var entry = serviceEntryTemplate.clone().appendTo(tableServiceBody).attr('id', 'service-' + service.specification);
            entry.find('td.name').text(service.specification);
            entry.find('td.state').text(service.state);
            
            if (service.id) {
                entry.find('td.id').text(service.id);
            } else {
                entry.find('td.id').html('<i>not registered</i>');
            }
            
            if (service.properties) {
                var list = $('<ul>');
                for (var x in service.properties) {
                    list.append($('<li>').append(service.properties[x].name + ' = ' + service.properties[x].value));
                }
                entry.find('td.properties').html(list);
            } else {
                entry.find('td.properties').html('<i>not properties</i>');
            }
        }
    } else {
        // Hide the table
        $('services').hide();
        _.find('td.Vservices').html("No provided services");    
    }
    
   if (instance.req) {
        $(tableReqBody).empty();
        for (var s in instance.req) {
            var service = instance.req[s];
            console.dir(service);
            // For each service clone the template
            var entry = reqEntryTemplate.clone().appendTo(tableReqBody).attr('id', 'req-' + service.id);
            entry.find('td.name').text(service.specification);
            if (service.filter) {
                entry.find('td.filter').text(service.filter);    
            } else {
                entry.find('td.filter').html('<i>no filter</i>');
            }
            
            entry.find('td.state').text(service.state);
            
            entry.find('td.policy').text(service.policy);
            entry.find('td.optional').text(service.optional);
            entry.find('td.aggregate').text(service.aggregate);
            
            
            if (service.matching) {
                var list = $('<ul>');
                for (var x in service.matching) {
                    if (service.matching[x].instance) {
                        var text = service.matching[x].instance + ' [' + service.matching[x].id + ']';
                        var link = $('<a href=\'' + instances_url + '/' + service.matching[x].instance +'\'>' + text + '</a>');
                        list.append($('<li>').append(link));
                    } else {
                        list.append($('<li>').append(service.matching[x].id));
                    }
                }
                entry.find('td.matching').html(list);
            } else {
                entry.find('td.matching').html('<i>no matching services</i>');
            }
            
             if (service.used) {
                var list = $('<ul>');
                for (var x in service.used) {
                    if (service.used[x].instance) {
                        var text = service.used[x].instance + ' [' + service.used[x].id + ']';
                        var link = $('<a href=\'' + instances_url + '/' + service.used[x].instance +'\'>' + text + '</a>');
                        list.append($('<li>').append(link));
                    } else {
                        list.append($('<li>').append(service.used[x].id));
                    }
                }
                entry.find('td.used').html(list);
            } else {
                entry.find('td.used').html('<i>no used services</i>');
            }
            
        }
    } else {
        // Hide the table
        $('reqServices').hide();
        _.find('td.VreqServices').html("No required services");    
    }
   

    _.find('pre.architecture_content').text(instance.architecture);
}

function retrieveDetails() {
    $.get(pluginRoot + '/instances/' + instance_name + '.json', null, function(data) {
        renderInstanceDetails(data);
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
var tableServiceBody = false;
var serviceEntryTemplate = false;

var tableReqBody = false;
var reqEntryTemplate = false;


$(document).ready(function(){
	tableBody = $('#plugin_table tbody');
    
    tableServiceBody = $('.services tbody');
    serviceEntryTemplate = tableServiceBody.find('tr').clone();
    
    tableReqBody = $('.reqServices tbody');
    reqEntryTemplate = tableReqBody.find('tr').clone();
    
    
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

