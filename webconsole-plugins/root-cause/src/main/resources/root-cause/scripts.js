/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
$(function () {

  $.widget("custom.rootcause", {
    // default options
    options: {
      rootCause: [],
      // Callbacks
      getRootCause: null
    },

    // The constructor
    _create: function () {
      this.path = this.element.data("rootcause");
      this.componentsPath = this.element.data("components");
      this.findButton = this.element.find(".root-cause-form__find-button");
      this.componentName = this.element.find(".root-cause-form__component-name");
      this.result = this.element.find(".root-cause__result");
      this.loading = this.element.find(".root-cause__loading");

      this.setupAutoComplete();

      // Bind click events on the changer button to the random method
      this._on(this.findButton, {
        // _on won't call random when widget is disabled
        click: "getRootCause"
      });

      if (location.hash) {
        this.getRootCause(null, location.hash.replace("#", ""));
      }
    },
    // setup autocomplete widget
    setupAutoComplete() {
      var that = this;
      $.getJSON(this.componentsPath)
      .done(function (components) {

        that.componentName.autocomplete({
          source: components,
          minLength: 3
        });
       }).fail(function (err) {
        console.error("Could not get components", err);
      })
    },

    // Called when created, and later when changing options
    _refresh: function () {
      var that = this;
      this.result.empty();
      this.options.rootCause
        .filter(Boolean)
        .forEach(function (line, i) {
          line = line || "";
          line = that.highlight(line);
          // https://stackoverflow.com/questions/25823914/javascript-count-spaces-before-first-character-of-a-string
          var leadingSpaceCount = line.search(/\S/) + 1;
          if (i == 0) {
            that.result.append('<h3 class="bold f-14">Search Result:<h3>');
          }
          that.result.append(
            $(
              "<span class=\"inline-block\">" + "&nbsp;&nbsp;".repeat(leadingSpaceCount) + "</span>" +
              "<span class=\"inline-block f-12 root-cause-line\">" + line + "</span></br>"
            )
          );
        })

      // Trigger a callback/event
      this._trigger("change");
    },
    // Highlights string for better readability
    highlight: function (str) {
      if (!str && !str.replace) return;
      return str
        .replace(/\b(unsatisfied|missing)\b/g, '<span class="c-red">$1</span>')
        .replace(/\b(satisfied)\b/g, '<span class="c-green">$1</span>')
        .replace(/([a-zA-Z0-9_]+(\.[a-zA-Z0-9_]+)+)/g, '<span class="c-mediumblue">$1</span>'); // thanks Paul!
    },
    getRootCause: function (event, componentName) {
      var that = this;
      this.result.empty();
      this.loading.show();
      this.result.hide();
      if (componentName) {
        this.componentName.val(componentName);
      } else {
        componentName = this.componentName.val().trim();
        location.hash = "#" + componentName;
      }
      $.getJSON(this.path + "?name=" + componentName)
        .always(function (result) {
          that._setOption("rootCause", result);
          that.loading.hide();
          that.result.show();
        })
    },

    // _setOptions is called with a hash of all options that are changing
    // always refresh when changing options
    _setOptions: function () {
      // _super and _superApply handle keeping the right this-context
      this._superApply(arguments);
      this._refresh();
    },

    // _setOption is called for each individual option that is changing
    _setOption: function (key, value) {
      // prevent invalid option
      if (key !== "rootCause") {
        return;
      }
      this._super(key, value);
      this._refresh();
    }
  });

  $(".root-cause").rootcause();
});
