/* your java script code here */

var userTree = false;
var selectedRole = false;
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
	data : {
		type : 'json',
		opts : { 'static' : [] }
	},
	ui       : { theme_name : 'themeroller' },
	rules    : { multiple : false, valid_children: ['root'] },
	types    : {
		root : { valid_children: ['t0', 't1', 't2'] },
		t2   : { valid_children: ['t0', 't1', 't2'] },
		t1   : { valid_children: 'none' },
		t0   : { valid_children: 'none' }
	},
	callback : {
		onselect : function(node) {
			var _role = $(node).attr('role');
			if (_role) {
				var role = JSON.parse( _role );
				onSelectNode(role.name);
				$(node).children('a').addClass('ui-priority-primary');
			} else {
				roleDetailsHelp.removeClass('ui-helper-hidden');
				roleDetailsTable.addClass('ui-helper-hidden');
			}
		},
		onparse : function (s, t) {
			return $(s)
					.find('li[rel=t2] > a > ins').addClass('ui-icon ui-icon-contact').end()
					.find('li[rel=t1] > a > ins').addClass('ui-icon ui-icon-person').end()
					.find('li[rel=t0] > a > ins').addClass('ui-icon ui-icon-bullet').end();
		},
		ondeselect : function(node) { $(node).children('a').removeClass('ui-priority-primary') },
		ondblclk   : function(node, tree_obj) {
			var n = $(node);
			var pp = tree_obj.parent(node);
			var r = roleObj(n);
			var g = roleObj(pp);
			console.log(r, g);
			if (r && g) {
				if( isInMemberArray(r, g.members, 1) ) {
					$.post(pluginRoot, { action: 'removeMember', role: r.name, group: g.name });
					$.post(pluginRoot, { action: 'addRequiredMember', role: r.name, group: g.name }, function(data) {
						pp.attr('role', JSON.stringify(data));
					}, 'json');
					n.addClass('required');
				} else if( isInMemberArray(r, g.rmembers, 1) ) {
					$.post(pluginRoot, { action: 'removeMember', role: r.name, group: g.name });
					$.post(pluginRoot, { action: 'addMember', role: r.name, group: g.name }, function(data) {
						pp.attr('role', JSON.stringify(data));
					}, 'json');
					n.removeClass('required');
				}
			}
		},
		beforemove : function(node, ref_node, type, tree_obj, is_copy) {
			var _ = dragObj(node, ref_node, type, tree_obj);
			// --- check if the move is valid:
			// don't move things around the same/root level
			if (_.to == false && _.from == false) return false;
			// no copy to the root folder
			if (is_copy && _.to == false) return false;
			// no rearrange withing the folder
			if (_.to != false && _.from != false && _.to.name == _.from.name) return false;
			// already contains such a member
			if (_.to != false && isMember(_.node, _.to)) return false;

			// do copy-move
			// unassign from the old group, if it is move
			if (!is_copy && _.from) $.post(pluginRoot, {'action': 'removeMember', 'role' : _.node.name, 'group' : _.from.name} , function(data) {}, 'json');
			// assign to the new group
			if (_.to) $.post(pluginRoot, {'action': 'addMember', 'role' : _.node.name, 'group' : _.to.name} , function(data) {}, 'json');

			return true;
		}
	}
}

$(function() {
	userTree = $('#userTree');
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

	// top-frame buttons
	$('#newRole').click( function() {
		newDialogRole.dialog('open');
		return false;
	});
	$('#reload').click( function() {
		$.post(pluginRoot, {'action': 'list'} , function(data) {
			roleDetailsHelp.removeClass('ui-helper-hidden');
			roleDetailsTable.addClass('ui-helper-hidden');

			var sortedGroups = sortGroups(data);
			var treeRoot = buildTree(sortedGroups);

			treeSettings.data.opts['static'] = treeRoot;
			userTree.empty().tree(treeSettings);
		}, 'json');
		return false;
	}).click();
});

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
				var hash =  CryptoJS[t.substring(9)](v).toString(CryptoJS.enc.Hex);
				v = hashToArray(hash);
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
		data : i18n.root,
		state: 'open',
		attributes : { 'rel' : 'root' },
		children: []
	};
	var treeNode = function(name, role, parent, req) {
		if (!role) return;
		if (!parent) parent = treeRoot.children;
		var node = {
			data  : role.name,
			attributes : {
				'rel'   : 't' + role.type,
				'role'  : JSON.stringify(role)
			}
		}
		if (req) node.attributes['class'] = 'required';
		parent.push(node);
		if (role.type == 2) {
			node['children'] = [];
			node = node['children'];
			if (role.members) $.each(role.members, function(idx, role) {
				treeNode(role.name, role, node, 0);
			});
			if (role.rmembers) $.each(role.rmembers, function(idx, role) {
				treeNode(role.name, role, node, 1);
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

function onSelectNode(role) {
	$.post(pluginRoot, {'action': 'get', 'role' : role} , function(data) {
		selectedRole = role;
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

function dragObj(node, ref_node, type, tree_obj) {
    // determine the destination folder
	var _role = false;
	if ('inside' == type) {
		_role = $(ref_node).attr('role');
	} else {
		_role = tree_obj.parent(ref_node)
		_role = _role.attr ? _role.attr('role') : false;
	}
	var to = _role ? JSON.parse(_role) : false;
	// determine object to move
	_role = $(node).attr('role');
	var source =  JSON.parse(_role);
	// determine the previous location (in case it is move, not copy)
	_role = tree_obj.parent(node);
	var from = _role.attr && _role.attr('role') ? JSON.parse(_role.attr('role')) : false;

	return {
		'to' : to,
		'from' : from,
		'node' : source
	}
}
