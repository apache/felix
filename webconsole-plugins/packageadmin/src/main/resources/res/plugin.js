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

var findField = false;
var findButton = false;
var findTable = false;
var findTableBody = false;
var findTableTemplate = false;
var dupsTable = false;
var dupsTableBody = false;
var dupsTableTemplate = false;
var statline = false;

function linkBundle(bnd) { return '<a href="{0}/bundles/{1}">{2} ({3})</a>'.msgFormat(appRoot, bnd.bid, bnd.bsn, bnd.bid) }

$(function() {
	findField = $('#findField');
	statline = $('.statline');

	findTable = $('#findTable').tablesorter({
		headers:{
			1:{sorter: false},
			3:{sorter: false}
		},
		textExtraction:mixedLinksExtraction
	});
	findTableBody = findTable.find('tbody');
	findTableTemplate = findTableBody.find('tr').clone();
	findTableBody.empty();

	dupsTable = $('#dupsTable');
	dupsTableBody = dupsTable.find('tbody');
	dupsTableTemplate = dupsTableBody.find('tr').clone();

	$('#findButton').click(function() {
		if(!findField.val()) {
			findField.addClass('ui-state-error');
		} else {
			findField.removeClass('ui-state-error');

			$.post(pluginRoot, { 'action': 'deps', 'plugin.find' : findField.val() }, function(response) {
				dupsTable.addClass('ui-helper-hidden')
				findTable.removeClass('ui-helper-hidden');
				findTableBody.empty();
				if (response.packages) for(var i in response.packages) {
					var pkg = response.packages[i];
					if (pkg.exporters) for(var i in pkg.exporters) {
						var exp = pkg.exporters[i];
						var tr = findTableTemplate.clone()
							.find('td.pkg').text(pkg.name).end()
							.find('td.ver').text(exp.version).end()
							.find('td.bnd').html(linkBundle(exp)).end()
							.appendTo(findTableBody);
						if (response.maven && response.maven[exp.bid]) {
							var mvn = response.maven[exp.bid];
							mvn['scope'] = 'provided';
							var txt = ''; for (var p in mvn) {
								txt += '\t<' + p + '>' + mvn[p] + '</' + p + '>\n';
							}
							tr.find('td.mvn').text('<dependency>\n' + txt + '</dependency>');
						}
					} else {
						var tr = findTableTemplate.clone()
							.find('td.pkg').text(pkg.name).end()
							.appendTo(findTableBody);
					}
				}
				statline.text(i18n.statusFind.msgFormat(response.packages ? response.packages.length : 0));
				findTable.trigger('update').trigger('applyWidgets')
			}, 'json');
		}
		return false;
	});

	$('#findDups').click(function() {
		$.post(pluginRoot, { 'action': 'dups' }, function(response) {
			findTable.addClass('ui-helper-hidden');
			dupsTable.removeClass('ui-helper-hidden')
			dupsTableBody.empty();
			if (response) for(var i in response) {
				var pkg = response[i];
				if (pkg.entries) for (var i in pkg.entries) {
					var exp = pkg.entries[i];
					var td = dupsTableTemplate.clone()
						.find('td.pkg').text(pkg.name).end()
						.find('td.ver').text(exp.version).end()
						.find('td.exp').html(linkBundle(exp)).end();
					if (exp.importers) {
						var txt = ''; for(var j in exp.importers) txt += linkBundle(exp.importers[j]) + '<br/>';
						td.find('td.imp').html( txt );
					}
					if (i==0) {
						td.find('td.pkg').attr('rowspan', pkg.entries.length);
					} else {
						td.find('td.pkg').remove();
					}
					td.appendTo(dupsTableBody);
				}
			}
			statline.text(i18n.statusDups.msgFormat(response ? response.length : 0));
		}, 'json');

		return false;
	});

})
