var DEBUG = false
var debug = function (obj) { if(DEBUG && console) console.debug(obj) }

function isEmpty(obj) { 
  for (var key in obj) return false 
  return true
}
function grep(source, f) {
  var greped = {}
  for (s in source) if (match(s, f.trim())) greped[s] = source[s]
  return greped
}

function match(s,f) {
  if (f.charAt(0) == "!") 
    return (s.indexOf(f.replace("!","")) < 0)
  else 
    return (s.indexOf(f) >= 0)
}


var SD = {
  json: {},
  grapher: {},

  draw: function(source) {
    this.grapher.draw(source)
  },

  loadServices: function (cmd, grapher) {
    $("#warning").html("")
    $("#canvas").html("Loading data. Please wait...")
    $.ajax({
      url: "servicegraph/"+cmd,
      dataType: "json",
      success: function(json){
        debug("Got services")
        debug(json)
        SD.json = json
        SD.grapher = grapher
        grapher.draw(json)
      },
      error: function() {
        alert("request failed on servicegraph/"+cmd)
      }
    })
  },

  doFilter: function () {
    var filter = $("#filter").val()
    if (filter) {
      var split = filter.split(" ")
      var greped = {}
      var action = ""
      $(split).each(function(i,e) {
        var tok = e.trim()
        if (tok == "&" || tok == "|") {
          action = tok
        }
        else if (action == "&") {
          greped = grep(greped, tok)
        }
        else if (action == "|") {
          var grepf = grep(SD.json, tok)
          for (s in grepf) greped[s] = grepf[s]
        }
        else greped = grep(SD.json, tok)
      })
      SD.draw(greped)
    } else {
      SD.draw(SD.json)
    }
  },

  showGraffle: function (g) {
      debug(g)

      /* layout the graph using the Spring layout implementation */
      new Graph.Layout.Spring(g).layout()
      
      /* draw the graph using the RaphaelJS draw implementation */
      $("#canvas").empty()
      new Graph.Renderer.Raphael('canvas', g, width, height).draw()
      
      $("#filterdiv").show()
  }
}

Unavail = {
  load: function() {
    var withOpt = ""
    if ($("#optionals").attr("checked")) withOpt = "?optionals=true"
    SD.loadServices("notavail"+withOpt, this)
  },

  draw: function(json) {
    $("#legend").html("Bubbles are components, dotted squares are missing required dependencies.")
    var g = new Graph()

    var empty = true
    notavail = json.notavail
    for (s in notavail) {
      empty = false
      for (i = 0; i < notavail[s].length; i++) {
        // point unregistered service to dependency name
        var dep = notavail[s][i]
        g.addNode(dep, {
          getShape : function(r,x,y) {
            // create a dashed square shape to differentiate the missing dependency
            return r.rect(x-30, y-13, 62, 33, 5).attr({
              "fill": "#f00", 
              "stroke": "gray", 
              "stroke-width": 2, 
              "stroke-dasharray": "--"
            })
          }
        })
        g.addEdge(s, dep, { directed : true } )
      }
    }
    // warn unresolved
    if (json.unresolved && !isEmpty(json.unresolved)) 
      $("#warning").html("circular dependencies detected! <a href='javascript:SD.grapher=Loops; Loops.draw(SD.json)'>(show)</a>")
    else 
      $("#warning").html("") //clear previous

    if (empty) {
      $("#canvas").empty().append($("<h1>").html("Service Registry status OK: No unresolved service found."))
    } 
    else SD.showGraffle(g)
  }
}


Loops = {
  draw: function(json) {
    $("#legend").html("Bubbles are unresolvable components linked to each other.")
    var g = new Graph()
    var unresolved = json.unresolved
    for (s in unresolved) {
      for (i = 0; i < unresolved[s].length; i++) {
        g.addEdge(s, unresolved[s][i], { directed : true } )
      }
    }
    SD.showGraffle(g)
  }
}

Users = {
  load: function() {
    SD.loadServices("users", this)
  },

  draw: function(json) {
    $("#legend").html("Black squares are bundles, pointing to the services they use.")
    var g = new Graph()

    var empty = true
    for (s in json) {
      empty = false
      for (i = 0; i < json[s].length; i++) {
        // point using bundle to service name
        var bundle = json[s][i]
        g.addNode(bundle, {
          getShape : function(r,x,y) {
            // create a square shape to differentiate bundles from services
            return r.rect(x-30, y-13, 62, 33, 5).attr({"fill": "#f00", "stroke-width": 2})
          }
        })
        g.addEdge(bundle, s, { directed : true } )
      }
    }

    if (empty) {
      $("#canvas").empty().append($("<h1>").html("Service Registry empty: no service found."))
    }
    else SD.showGraffle(g)
  }
}

Providers = {
  load: function() {
    SD.loadServices("providers", this)
  },

  draw: function(json) {
    $("#legend").html("Black squares are bundles, pointing to the services they provide.")
    var g = new Graph()

    var empty = true
    for (bundle in json) {
      empty = false
      g.addNode(bundle, {
        getShape : function(r,x,y) {
          // create a square shape to differentiate bundles from services
          return r.rect(x-30, y-13, 62, 33, 5).attr({"fill": "#f00", "stroke-width": 2})
        }
      })
      for (i = 0; i < json[bundle].length; i++) {
        // point bundle to service name
        var service = json[bundle][i]
        g.addEdge(bundle, service, { directed : true } )
      }
    }

    if (empty) {
      $("#canvas").empty().append($("<h1>").html("Service Registry empty: no service found."))
    }
    else SD.showGraffle(g)
  }
}

B2B = {
  load: function() {
    SD.loadServices("b2b", this)
  },

  draw: function(json) {
    $("#legend").html("Black squares are bundles, pointing to the bundles they use for their services.")
    var g = new Graph()

    var empty = true
    for (provider in json) {
      empty = false
      g.addNode(provider, {
        getShape : function(r,x,y) {
          // create a square shape to differentiate bundles from services
          return r.rect(x-30, y-13, 62, 33, 5).attr({"fill": "#f00", "stroke-width": 1})
        }
      })
      for (i = 0; i < json[provider].length; i++) {
        // point using bundle to provider bundle
        var user = json[provider][i]
        g.addNode(user, {
          getShape : function(r,x,y) {
            // create a square shape to differentiate bundles from services
            return r.rect(x-30, y-13, 62, 33, 5).attr({"fill": "#f00", "stroke-width": 2})
          }
        })
        g.addEdge(user, provider, { directed : true } )
      }
    }

    if (empty) {
      $("#canvas").empty().append($("<h1>").html("Service Registry empty: no service found."))
    }
    else SD.showGraffle(g)
  }
}
