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
 * 
	Structure is:
	<div class="propeditor">
		<div>
			<input class="key"/> = 
			<input class="value"/>
			<select>
				<option>byte</option>
				<option>int</option>
				<option>long</option>
				<option>float</option>
				<option>double</option>
				<option>string</option>
				<option>char</option>
				<option>hex</option>
				<option>sha1</option>
				<option>base64</option>
			</select>
		</div>
	</div>
	
	Options:
	validator : function(keyInputField, valInputField, type)
	types     : ['byte', 'int', 'long', 'float', 'double', 'string', 'char', 'hex', 'base64', 'sha1']
*/
(function( $ ){
	var methods = {
		init : function(options) {
			return this.each( function() {
				// If options exist, lets merge them with our default settings
				var settings = {
					validator  : false,
					types      : ['byte', 'int', 'long', 'float', 'double', 'string', 'char', 'byte array']
				};
				if (options) settings = $.extend(settings, options);

				var _this = $(this);
				_this.data('propeditor_settings', settings);
				_this.append(_entry(settings.types));
				_this.addremove(settings);
			})
		},
		reset : function() {
			return this.each( function() {
				$(this).addremove('reset')
					.find('.key').val('').end()
					.find('.val').val('');
			});
		},
		serialize : function() {
			var self = $(this);
			var validator = self.data('propeditor_settings').validator;
			var result = new Array();
			var ok = true;
			var entries = $(this).find('div.addremove');
			if (entries.size() == 1) {
				var k = entries.find('.key').removeClass('ui-state-error').val();
				var v = entries.find('.val').removeClass('ui-state-error').val();
				if (k || v) {
					var data = _check_entry( entries, validator );
					if ( data == false ) ok = false; else result = data;
				}
			} else {
				entries.each(function() {
					var data = _check_entry( $(this), validator );
					if ( data == false ) ok = false; else result = result.concat(data);
				});
			}
			return ok ? result : false;
		},
		setup : function(data, append) {
			var self = $(this);
			if (!append) self.propeditor('reset');
			for (var i in data) {
				self.addremove('add');
				var d = data[i];
				self.find('div.addremove:last')
					.find('.key').val(d.key).end()
					.find('.val').val(d.val).end()
					.find('.typ').val(d.type);
			}
			if (!append) self.find('div.addremove:first').remove();
		}
	};

	$.fn.propeditor = function( method ) {
		// Method calling logic
		if ( methods[method] ) {
		  return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else if ( typeof method === 'object' || ! method ) {
		  return methods.init.apply( this, arguments );
		} else {
		  $.error( 'Method ' +  method + ' does not exist on jQuery.tooltip' );
		} 
	};

	var _el = function(el, clazz) {
		var ret = $(document.createElement(el));
		if (clazz) ret.addClass(clazz);
		return ret;
	}

	var _entry = function(TYPES) {
		var sel = _el('select', 'typ');
		for(var i in TYPES) {
			sel.append( _el('option').text( TYPES[i] ) );
		}
		return _el('span', 'propeditor_entry')
			.append( _el('input', 'key') )
			.append( _el('span').text(' = '))
			.append( _el('input', 'val') )
			.append( sel );
	}

	var _check_entry = function(e, validator) {
		var k = e.find('.key').removeClass('ui-state-error');
		var v = e.find('.val').removeClass('ui-state-error');
		var t = e.find('.typ').val();
		var ok = _check_field(k);
		ok = _check_field(v) && ok;
		ok = ok && _defaultPropertyValidator(k, v, t);
		if (ok && typeof validator == 'function') {
			ok = validator(k, v, t);
		}
		if (ok) {
			return [
				{ 'name' : 'key', 'value' : k.val() },
				{ 'name' : 'val', 'value' : v.val() },
				{ 'name' : 'type', 'value' : t }
			];
			/*
			return {
				'key': k.val(),
				'val': v.val(),
				'type': t
			}*/
		}
		return false;
	}

	var _check_field = function(f) {
		if (!f.val()) {
			f.addClass('ui-state-error');
			return false;
		}
		return true;
	}

	var _range = function(field, isint, min, max) {
		var v = false;
		if (isint) {
			var v = parseInt(field.val());
			var xv = parseFloat(field.val());
			if ( isNaN(v) || isNaN(xv) || xv != v) return false;  // field is actually double
		} else { // double
			v = parseFloat(field.val());
			if (isNaN(v)) return false;
		}

		return v >= min && v <= max;
	}
	
	// key == element, value == element, type == type string
	var _defaultPropertyValidator = function(key, value, type) {
		var v = value.val();
		var ok = true;
		switch(type) {
			case 'byte':
				ok = _range(value, true, -128, 127);
				break;
			case 'int':
				ok = _range(value, true, -2147483648, 2147483647);
				break;
			case 'long':
				ok = _range(value, true, -9223372036854775808, 9223372036854775807);
				break;
			case 'float':
				ok = _range(value, false, 1.4E-45, 3.4E38);
				break;
			case 'double':
				ok = _range(value, false, 4.9E-324, 1.7E308);
				break;
			case 'char':
				ok = v.length == 1;
				break;
		}
		if (!ok) value.addClass('ui-state-error');
		return ok;
	}

})( jQuery );