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

/* init table sorter defaults */
if ( $.tablesorter ) {
	$.tablesorter.defaults.cssAsc  = 'headerSortUp ui-state-focus';
	$.tablesorter.defaults.cssDesc = 'headerSortDown ui-state-focus';
	$.tablesorter.defaults.header  = 'header ui-widget-header';
	$.tablesorter.defaults.widgets = ['zebra'];
	$.tablesorter.defaults.widgetZebra = {
		css : ["odd ui-state-default", "even ui-state-default"]
	};
}

/* initializes static widgets */
function initStaticWidgets(elem) {
	// hover states on the static widgets - form elements
	var el = elem ? $(elem) : $(document);
	el.find('button, input[type!=checkbox], .dynhover').hover(
		function() { $(this).addClass('ui-state-hover'); },
		function() { $(this).removeClass('ui-state-hover'); }
	).addClass('ui-state-default ui-corner-all');
	// fix attribute selector in IE
	el.find('input[type=text], input[type=password], input[type=file]').addClass('inputText');

	// make buttones nicer by applying equal width - not working in IE ;(
	el.find('button, input[type=submit], input[type=reset], input[type=button]').not(".ui-dialog-titlebar-close").each(function(i) {
		var txt = $(this).text();
		var apply = txt && txt.length > 1;
		if (apply) $(this).css('min-width', '8em');
	});

	// add default table styling - colors and fonts from the theme
	el.find('table.nicetable').addClass('ui-widget');
	el.find('table.nicetable th').addClass('ui-widget-header');

	// add default styling for table sorter
	el.find("table.tablesorter tbody").addClass("ui-widget-content");

	// add theme styling to the status line
	el.find('.statline').addClass('ui-state-highlight')

	el.find('table.tablesorter').trigger("update").trigger("applyWidgets");
}

/* automatically executed on load */
$(document).ready(function() {
	// init table-sorter tables - only once!
	var tables = $('table.tablesorter:not(.noauto)');
	if (tables.size() > 0) tables.tablesorter();
	
	// init navigation
	$('#technav div.ui-state-default').hover(
		function() { $(this).addClass('ui-state-hover'); },
		function() { $(this).removeClass('ui-state-hover'); }
	);

	// register global ajax error handler
	$(document).ajaxError( function(xevent, req) {
		var _t = '';
		if (req.responseText) {
			_t = req.responseText;
		} else if (req.statusText) {
			_t = req.statusText;
		} else if (req.responseXML) {
			_t = req.responseXML;
		}
		Xalert('The request failed: <br/><pre>' + _t + '</pre>', 'AJAX Error');
	});

	initStaticWidgets();
});

/* A helper function, used together with tablesorter, when the cells contains mixed text and links. As example:
	elem.tablesorter({
		textExtraction: mixedLinksExtraction
	});
*/
function mixedLinksExtraction(node) {
	var l = node.getElementsByTagName('a');
	return l && l.length > 0 ? l[0].innerHTML : node.innerHTML;
};

/* Java-like MessageFormat method. Usage:
	'hello {0}'.msgFormat('world')
*/
String.prototype.msgFormat = function(/* variable arguments*/) {
	var i=0; var s=this;
	while(i<arguments.length) s=s.replace('{'+i+'}',arguments[i++]);
	return s;
}


/* replacement for confirm() method, needs 'action' parameter to work.
 * if action is not set - then default confirm() method is used. */
function Xconfirm(/* String */text, /* Callback function */action, /* String */title) {
	if (!$.isFunction(action)) return confirm(text);
	if (title === undefined) title = "";

	Xdialog(text).dialog({
		modal: true,
		title: title,
		buttons: {
			"Yes": function() {
				$(this).dialog('close');
				action();
			},
			"No": function() {
				$(this).dialog('close');
			}
		}
	});
	return false;
}
function Xalert(/* String */text, /* String */title) {
	if (title === undefined) title = "";

	Xdialog(text).dialog({
		modal: true,
		title: title,
		width: '70%',
		buttons: {
			"Ok": function() {
				$(this).dialog('close');
			}
		}
	});
	return false;
}
/* a helper function used by Xconfirm & Xalert */
function Xdialog(text) {
	var dialog = $('#dialog'); // use existing dialog element

	if ( dialog.size() == 0 ) { // doesn't exists
		var element = document.createElement( 'div' );
		$('body').append(element);
		dialog = $(element);
	}

	// init dialog
	dialog.html(text); // set text & reset dialog
	if (dialog.is( ":data( 'dialog' )" )) dialog.dialog('destroy');
	return dialog;
}


/* String */ function wordWrap( /* String */ msg ) {
	if (msg) {
		var userAgent = navigator.userAgent.toLowerCase();
		var isMozilla = /mozilla/.test( userAgent ) && !/(compatible|webkit)/.test( userAgent );
	
		return isMozilla ? msg.split('').join(String.fromCharCode('8203')) : msg;
	} else {
		return '';
	}
}


/* content of the old ui.js */

/* Element */ function clearChildren( /* Element */ element ) {
	while (element.firstChild) {
		element.removeChild(element.firstChild);
	}
	return element;
}

/* String */ function serialize( /* Element */ element ) {
	var result = "";

	if (element) {
		if (element.nodeValue) {
			result = element.nodeValue;
		} else {
			result += "<" + element.tagName;
			
			var attrs = element.attributes;
			for (var i=0; i < attrs.length; i++) {
				if (attrs[i].nodeValue) {
					result += " " + attrs[i].nodeName + "='" + attrs[i].nodeValue + "'";
				}
			}

			var children = element.childNodes;
			if (children && children.length) {
				result += ">";

				for (var i=0; i < children.length; i++) {
					result += serialize( children[i] );
				}
				result += "</" + element.tagName + ">";
			} else {
				result += "/>";
			}
		}
	}

	return result;
}

/* Element */ function th( /* String */ cssClass, /* Map */ attrs, /* Element[] */ children ) {
	return createElement( "th", cssClass, attrs, children );
}

/* Element */ function tr( /* String */ cssClass, /* Map */ attrs, /* Element[] */ children ) {
	return createElement( "tr", cssClass, attrs, children );
}

/* Element */ function td( /* String */ cssClass, /* Map */ attrs, /* Element[] */ children ) {
	return createElement( "td", cssClass, attrs, children );
}

/* Element */ function text( /* String */ textValue ) {
	return document.createTextNode( textValue );
}

/* Element */ function createElement( /* String */ name, /* String */ cssClass, /* Map */ attrs, /* Element[] */ children  ) {
	var sb = ["<", name];

	if (attrs && attrs.name) {
		sb.push(" name='", attrs.name, "'");
	}

	if (attrs && attrs.type) {
		sb.push(" type='", attrs.type, "'");
	}

	sb.push(">");

    if (name == "textarea") {
        sb.push("</textarea>");
    }

	var el = $(sb.join(""));

	if (cssClass) {
		el.addClass(cssClass);
	}
	if (attrs) {
		for (var lab in attrs) {
			if ("style" == lab) {
				var styles = attrs[lab];
				for (var styleName in styles) {
					el.css(styleName, styles[styleName]);
				}
			} else if ("name" == lab || "type" == lab) {
				//skip
			} else {
				el.attr( lab, attrs[lab] );
			}
		}
	}

	var element = el.get()[0];

	if (children && children.length) {
		for (var i=0; i < children.length; i++) {
		    if (children[i]) {
		        element.appendChild( children[i] );
		    }
		}
	}

	return element;
}

/* Element */ function addText( /* Element */ element, /* String */ textValue ) {
	if (element && textValue) {
		element.appendChild( text( textValue ) );
	}
	return element;
}

/**
 * Sets the name cookie at the appRoot (/system/console by default) path
 * to last for 20 years.
 * @param name The name of the cookie
 * @param value The value for the cookie
 */
function setCookie( /* String */name, /* String */value) {
	var date = new Date();
	date.setFullYear(date.getFullYear() + 20);
	$.cookies.del("felix-webconsole-" + name);
	$.cookies.set("felix-webconsole-" + name, value, {
		expiresAt : date,
		path : appRoot
	});
}

/**
 * Returns the value of the name cookie or nothing if the cookie does
 * not exist or is not accessible.
 * @param name The name of the cookie
 */
/* String */ function getCookie(/*String */name) {
    return $.cookies.get("felix-webconsole-" + name);
}

// language selection element
var langSelect = false;
$(document).ready(function() {
	langSelect = $('#langSelect').hover( 
		function() { $(this).find('.flags').show('blind') },
		function() { $(this).find('.flags').hide('blind') });
	langSelect.find('.flags img').click(function() {
		setCookie("locale", $(this).attr('alt'));
		location.reload();
	});
	var locale = getCookie("locale");
	if (locale) {
		if ( !$.datepicker.regional[locale] ) locale = '';
		$.datepicker.setDefaults($.datepicker.regional[locale]);
	}
});
