var page = require("webpage").create();
var system = require("system");
var fs = require("fs");

var file_type = system.args[1];
var delay = system.args[2];
var pixel_ratio = system.args[3];

var snapshot =
    "    function(){" +
    "        var ele = document.querySelector('div[_echarts_instance_]');" +
    "        var mychart = echarts.getInstanceByDom(ele);" +
    "        return mychart.getDataURL({type:'" + file_type + "', pixelRatio: " + pixel_ratio + ", excludeComponents: ['toolbox']});" +
    "    }";

var snapshot_svg =
    "    function () {" +
    "       var element = document.querySelector('div[_echarts_instance_] div');" +
    "       return element.innerHTML;" +
    "    }";
var file_content = system.stdin.read();
page.setContent(file_content, "");

window.setTimeout(function () {
    if (file_type === 'svg') {
        var content = page.evaluateJavaScript(snapshot_svg);
    } else {
        var content = page.evaluateJavaScript(snapshot);
    }
    var filename = new Date().getTime() + ".txt"
    fs.write("phantomjs/output/" + filename, content)
    console.log(filename)
    phantom.exit();
}, delay);