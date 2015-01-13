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
// fix for jQuery tree to work with this version of jQuery
jQuery.curCSS = jQuery.css;

var userTree = false;
var selectedRole = false;
var selectedParent = false;
var newDialogRole = false;
var roleDetails = false;
var roleDetailsHelp = false;
var roleDetailsTable = false;
var roleDetailsBody = false;
var roleDetailsTemplate = false;
var roleDetailsTemplateP = false;
var roleDetailsTemplateC = false;

function roleObj(node) {
	node = node && node.attr ? node.attr('role') : false;
	return node ? JSON.parse(node) : false;
}

var treeSettings = {
	core    : {
		data           : [], // will be set on load
		multiple       : false,
		themes         : { stripes : true },
		check_callback : function (operation, node, node_parent, node_position, more) {
			// disable copy to root node
			if ('#' === node_parent.id) return false;
			if (operation === 'copy_node' && 'root' === node_parent.id) return false;

			if (operation === 'move_node' || operation === 'copy_node') {
				// don't copy/move things around the same/root level
				if (node.parent === node_parent.id) return false;

				// don't copy/move if target alreay contains the same member
				var target = node_parent.original.role;
				if (target && isMember(node.original.role, target)) return false;
			}
			return true;
		}
	},
	plugins : [ 'dnd', 'types', 'sort' ],
	types   : {
		root : { valid_children: ['t0', 't1', 't2'], icon : pluginRoot + '/res/book-2.png' },
		t2   : { valid_children: ['t0', 't1', 't2'], icon : pluginRoot + '/res/group.png' },
		t1   : { valid_children: [], icon : pluginRoot + '/res/user.png'  },
		t0   : { valid_children: [], icon : pluginRoot + '/res/role.png' }
	}
}

function initTree(data) {
	// show help message
	roleDetailsHelp.removeClass('ui-helper-hidden');
	roleDetailsTable.addClass('ui-helper-hidden');

	var openNodes = [];

	// recreate tree, because reload doesn't work
	var userTreeRef = $.jstree.reference('#userTree');
	if (userTreeRef) {
		userTreeRef.destroy();

		// save state
		$.each(userTreeRef.get_node('root').children_d, function(idx, child) {
			var node = userTreeRef.get_node(child);
			if ( node.state.opened ) openNodes.push(node.text);
		});
	}

	// prepare data
	var sortedGroups = sortGroups(data);
	treeSettings.core['data'] = buildTree(sortedGroups);

	// build tree
	userTree = $('#userTree')
		.on('select_node.jstree', function(e, data) {
			var role = data.node.original.role;
			if (role) {
				var parent = data.node.parent;
				var parent_name = parent === '#' || parent === 'root' ? false : data.instance.get_node(parent).text;
				onSelectNode(role.name, parent_name);
			} else {
				roleDetailsHelp.removeClass('ui-helper-hidden');
				roleDetailsTable.addClass('ui-helper-hidden');
			}
		})
		.on('move_node.jstree', function(e, data) {
			var role = data.node.original.role;
			var parent = data.parent;
			var parent_name = parent === '#' || parent === 'root' ? false : data.instance.get_node(parent).text;
			var old_parent = data.old_parent;
			var old_parent_name = old_parent === '#' || old_parent === 'root' ? false : data.instance.get_node(old_parent).text;

			if (parent_name) {
				//console.log('move: adding role', role, 'to group', parent);
				$.post(pluginRoot, {'action': 'addMember', 'role' : role.name, 'group' : parent_name});
			}
			if (old_parent_name) {
				//console.log('move: removed role', role, 'to group', old_parent);
				$.post(pluginRoot, {'action': 'removeMember', 'role' : role.name, 'group' : old_parent_name});
			}
			$('#reload').click();
		})
		.on('copy_node.jstree', function(e, data) {
			var role = data.original.original.role;
			var parent_name = data.instance.get_node(data.parent).text;

			if (parent_name) {
				//console.log('copy: copying role', role, 'to group', parent);
				$.post(pluginRoot, {'action': 'addMember', 'role' : role.name, 'group' : parent_name});
			}
			$('#reload').click();
		})
		.on('ready.jstree', function(e, data) { // restore state
			var _ = data.instance;
			if (openNodes.length) $.each(_.get_node('root').children_d, function(idx, child) {
				var node = _.get_node(child);
				if ($.inArray(node.text, openNodes) > -1) _.open_node(node, false, false);
			});

		}).jstree(treeSettings);
}

$(function() {
	// read the available digest algorithms
	$.ajax({
		type    : "POST",
		url     : pluginRoot,
		async   : false,
		data    : {'action': 'getDigestAlgorithms' },
		dataType: 'json',
		success : function(data) {
			var _select  = $('select.propertyType');
			$.each(data, function(id, alg) {
				_select.append('<option value="password-{0}">{1} {2}</option>'.msgFormat(alg, i18n.paswd, alg));
			});
		}
	});

	roleDetails = $('#roleDetails');
	roleDetailsTable = roleDetails.find('table');
	roleDetailsHelp = roleDetails.find('#roleDetailsHelp');
	roleDetailsBody = roleDetailsTable.find('tbody');
	roleDetailsTemplateP = roleDetailsBody.find('tr:eq(0)');
	roleDetailsTemplateC = roleDetailsBody.find('tr:eq(1)');
	roleDetailsTemplate = roleDetailsBody.find('tr:eq(2)').clone();
	roleDetailsBody.find('tr').not('.header').remove();

	// add new property/credential code
	$('tr.header span.ui-icon-plus').click(function() {
		$(this).parent().parent().parent().parent().after(newProp());
	});

	// new role dialog
	var _buttons = {};
	_buttons[i18n.close] = function() {
		$(this).dialog('close');
	}
	_buttons[i18n.add] = function() {
		var _ = newDialogRole;
		var n = _.find('input');
		if (!n.val()) {
			n.addClass('ui-state-error');
			return false;
		} else n.removeClass('ui-state-error');
		var t = _.find('select').val();
		$.post(pluginRoot, {'action': 'set', 'data' : JSON.stringify({'name': n.val(), 'type': new Number(t)})} , function(data) {
			_.dialog('close');
			$('#reload').click();
		}, 'json');
	}
	newDialogRole = $("#newDialogRole").dialog({
		autoOpen : false,
		modal    : true,
		open     : function() { $(this).find('input').val('').removeClass('ui-state-error') },
		closeText: i18n.abort,
		buttons  : _buttons
	});

	// role info buttons
	$('#delRole').click( function() {
		if (selectedRole) $.post(pluginRoot, {'action': 'del', 'role' : selectedRole}, function() {
			$('#reload').click();
		});
	});
	$('#savRole').click( doSaveRole );
	$('#toggleRequiredRole').click( function() {
		if (selectedRole && selectedParent)
		$.post(pluginRoot, { action: 'toggleMembership', role: selectedRole, group: selectedParent }, function() {
			$('#reload').click()
		});
	});

	// top-frame buttons
	$('#newRole').click( function() {
		newDialogRole.dialog('open');
		return false;
	});
	$('#reload').click( function() {
		$.post(pluginRoot, {'action': 'list'} , initTree, 'json');
		return false;
	}).click();
});

function digest(val, alg) {
	var _ret = false;
	$.ajax({
		type    : "POST",
		url     : pluginRoot,
		async   : false,
		data    : {
			'action': 'digest',
			'data' : val,
			'algorithm' : alg
		},
		dataType: 'json',
		success : function(data) {
			_ret = data['encoded'];
		}
	});
	return _ret;
}
function newProp() {
	var tr = roleDetailsTemplate.clone()
		.find('li').click( function() {
			tr.remove();
		}).end()
		.find('select').change( function(evt) {
			tr.find('.v').replaceWith('<input class="v" '+ ($(this).val().indexOf('password') == 0 ? 'type="password"' : '') + '/>');
			initStaticWidgets(tr);
		}).end()
	initStaticWidgets(tr);
	return tr;
}
function hashToArray(s) {
    var r = [];
    while(s.length > 0) {
        r.push(parseInt(s.substring(0, 2), 16));
        s = s.substring(2);
    }
    return r;
}
function strToArray(s) {
    var r = [];
    var el = s.split(',');
    for(var i=0;i<el.length;i++) r.push( parseInt(el[i], 10) );
    return r;
}
function doSaveRole() {
	if (!selectedRole) return;
	var doProps = true;
	var data = {
		name : selectedRole,
		properties : {},
		credentials : {}
	};
	roleDetailsBody.find('tr').each( function() {
		var _ = $(this);
		if (_.hasClass('header-props')) doProps = true;
		else if (_.hasClass('header-cred')) doProps = false;
		else {
			var k = _.find('.k').val();
			var v = _.find('.v').val();
			var t = _.find('select').val();

			if (t.indexOf('password-') == 0) {
				var hash = digest(v, t.substring(9));
				v = hash;
			} else if (t == 'byte[]') {
				v = strToArray(v);
			}

			if (doProps) data.properties[k] = v;
			else data.credentials[k] = v;
		}
	});
	$.post(pluginRoot, {'action': 'set', 'data' : JSON.stringify(data)});
}

function isInMemberArray(role, g) {
	if(g) for(var i in g) if (g[i].name == role.name) return true;
	return false;
}

function isMember(role, group) {
	if (!role) return false;
	if (!group) return false;
	if (isInMemberArray(role, group.members)) return true;
	if (isInMemberArray(role, group.rmembers)) return true;
}

function buildTree(sortedGroups) {
	var treeRoot = {
		text : i18n.root,
		id   : 'root',
		type : 'root',
		state: { opened : true },
		children: []
	};
	var treeNode = function(name, role, parent, req) {
		if (!role) return;
		if (!parent) parent = treeRoot.children;
		var node = {
			'text' : role.name,
			'type' : 't' + role.type,
			'role' : role,
		}
		if (req) node.li_attr = { 'class' : 'required' };
		parent.push(node);
		if (role.type == 2) {
			node['children'] = [];
			node = node['children'];
			if (role.members) $.each(role.members, function(idx, xrole) {
				treeNode(xrole.name, xrole, node, 0);
			});
			if (role.rmembers) $.each(role.rmembers, function(idx, xrole) {
				treeNode(xrole.name, xrole, node, 1);
			});
		}
	}

	$.each(sortedGroups, treeNode);
	return treeRoot;
}

function sortGroups(data) {
	var rootGroups = {}; // only root groups - without parents
	var unassigned = {}; // non-groups, not assigned to any group
	var processed = {}; // all processed entries
	var u = 0;
	var g = 0;
	var r = 0;

	var _st = function(map, n, role) {
		if (typeof map[n] == 'undefined') { // not added - add
			map[n] = role;
		} else if (map[n] != false) { // already added
			map[n] = false; // mark for removal
		}
	}

	var groupF = function(i1, role) {
		var n = role.name;
		var t = role.type;

		if (t == 2) { // group
			// don't process twice
			if (processed[n]) {
				rootGroups[n] = false;
				return true;
			}
			processed[n]=role;

			_st(rootGroups, n, role);

			if (role.members) $.each(role.members, groupF);
			if (role.rmembers) $.each(role.rmembers, groupF);
			g++;
		} else { // role or user
			if (t == 1) u++; else r++;
			_st(unassigned, n, role);
		}
	}

	$.each(data, groupF);
	$('.statline').text( i18n.status.msgFormat(g,u,r) );

	return $.extend(rootGroups, unassigned);
}

function onSelectNode(role, parent) {
	if (parent) {
		$('#toggleRequiredRole').removeClass('ui-state-disabled');
	} else {
		$('#toggleRequiredRole').addClass('ui-state-disabled')
	}

	$.post(pluginRoot, {'action': 'get', 'role' : role} , function(data) {
		selectedRole = role;
		selectedParent = parent;
		roleDetailsHelp.addClass('ui-helper-hidden');
		roleDetailsTable.removeClass('ui-helper-hidden');
		roleDetailsBody.find('tr').not('.header').remove();

		var target = false;
		var _append = function(k,v) {
			var t = $.isArray(v) ? 'byte[]' : 'string';
			target.after(newProp()
				.data('k', k)
				.find('input.k').val(k).end()
				.find('input.v').val(v).end()
				.find('select').val(t).end()
			);
		}
		var x = data.properties;
		if (x) {
			target = roleDetailsTemplateP;
			$.each(x, _append);
		}
		x = data.credentials;
		if (x) {
			target = roleDetailsTemplateC;
			$.each(x, _append);
		}
		// show/user credentials view if user/or not (respectively)
		x = roleDetailsBody.find('.header-cred');
		if (data.type != 1) x.addClass('ui-helper-hidden');
		else x.removeClass('ui-helper-hidden');
	}, 'json')
	return false;
}

