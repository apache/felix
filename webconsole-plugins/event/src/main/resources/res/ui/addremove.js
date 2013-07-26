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
	<div class="my-element-container">
		<div class="multiInput">
			<div id="myElement" /> + -
		</div>
	</div>
	
	Options:
	add : function(element) - called AFTER add
	remove : function(element) - called BEFORE remove
*/
(function( $ ){

	var methods = {
		init : function(options) {
			return this.each( function() {
				// If options exist, lets merge them with our default settings
				var settings = {
					add    : false,
					remove : false
				};
				if (options) settings = $.extend(settings, options);
				
				var _this = $(this);
				var template = _init_template( _this );
				_this.data('addremove_settings', settings);
				_new_entry(template, _this);
			})
		},
		reset : function() {
			return this.each( function() {
				var self = $(this);
				self.find('div.addremove').not(':first').each( function() {
					$(this).find('button.rem').click();
				});
			});
		},
		add : function(count) {
			return this.each( function() {
				var self = $(this);
				var addfn = self.find('div.addremove:last button.add');
				if (addfn.size()) {
					var num = count ? count : 1;
					for(var i=0; i<num; i++) addfn.click();
				}
			});
		},
		count : function() {
			var self = $(this);
			return $(this).find('div.addremove').size();
		}
	};

	$.fn.addremove = function( method ) {
		// Method calling logic
		if ( methods[method] ) {
		  return methods[ method ].apply( this, Array.prototype.slice.call( arguments, 1 ));
		} else if ( typeof method === 'object' || ! method ) {
		  return methods.init.apply( this, arguments );
		} else {
		  $.error( 'Method ' +  method + ' does not exist on jQuery.addremove' );
		} 
	};
	
	var _new_entry = function(template, container) {
		var settings = container.data('addremove_settings');
		var _entry = template.clone()
			.find('button.add').click( function() {
				_new_entry(template, container);
				return false;
			}).end()
			.find('button.rem').click( function() {
				if (container.addremove('count') > 1) {
					if (typeof settings.remove == 'function') {
						settings.remove(_entry);
					}
					_entry.remove();
				}
				return false;
			}).end()
			.appendTo(container);
		if (typeof settings.add == 'function') settings.add(_entry);
	}

	var _init_template = function(entry) {
		return _el('div', 'addremove')
			.append(entry.children())
			.append(_el('button', 'add').text('+'))
			.append(_el('button', 'rem').text('-'));
	}

	var _el = function(el, clazz) {
		var ret = $(document.createElement(el));
		if (clazz) ret.addClass(clazz);
		return ret;
	}

})( jQuery );