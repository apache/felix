//Codemirror editor
var inputEditor
var outputEditor

function sendData(url, data) {
    $.ajax({
        type:"POST",
        url:url,
        data:data,
//        dataType:"json",
        beforeSend:function () {
            $('#ajaxSpinner').show();
        },
        /* error: function() {
         $('#status').text('Update failed�try again.').slideDown('slow');
         },*/
        complete:function () {
            $('#ajaxSpinner').hide();
        },
        success:function (data) {
            renderData(data)
        }
    });
}

function renderData(data){
    $('#code-output').show();
    outputEditor.setValue(data)
}

function setUpCodeMirror() {
    CodeMirror.modeURL = pluginRoot + "/res/ui/codemirror/mode/%N/%N.js";
    inputEditor = CodeMirror.fromTextArea(document.getElementById("code"), {
        lineNumbers:true,
        extraKeys: {
            'Ctrl-Q':clearOutput,
            'Ctrl-F9':executeScript
        }
    });
    outputEditor = CodeMirror.fromTextArea(document.getElementById("result"), {
            lineNumbers:true,
            readOnly:true
        });
}

function updateWithOption(opt){
    setLangMode(inputEditor,getProp(opt,'langMode'))
    $('[name=lang]').val(opt.val())
}

function setLangMode(editor, modeName) {
    if(!modeName){
        modeName = "text/plain"
    }else{
        CodeMirror.autoLoadMode(inputEditor, modeName);
    }
    editor.setOption("mode", modeName);
}

function getProp(obj,propName){
    if(obj != null){
        return obj.prop ?  obj.prop(propName) : obj.attr(propName)
    }
}

function setUpLangOptions() {
    var codeLang = $('#codeLang')
    var options = getProp(codeLang,'options');
    codeLang.empty()

    for(var i in scriptConfig){
        var config = scriptConfig[i]
        var opt = new Option(config.langName,config.langCode);
        if(config.mode){
            opt.langMode = config.mode;
        }
        options[options.length] = opt
    }
    codeLang.change(function(){
        var opt = $(this).find(":selected");
        updateWithOption(opt)
    });

    codeLang.find('option:eq(0)').attr('selected','selected')
    updateWithOption($(options[0]))
}

function executeScript(){
    inputEditor.save() //Copy the contents to textarea form field
    sendData(pluginRoot, $("#consoleForm").serialize());
}

function clearOutput(){
    outputEditor.setValue('')
}

$(document).ready(function () {
    $("#executeButton").click(executeScript);
    $('#ajaxSpinner').hide();
    $('#code-output').hide();
    setUpCodeMirror();
    setUpLangOptions();
});
