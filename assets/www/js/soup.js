
navigator.mozApps = {
	install: function(url, install_data, onsuccess, onerror) {
		alert("TO BE IMPLEMENTED");		
	}
};

var apps = document.getElementsByClassName("app");
for (var i = 0; i < apps.length; i++) {
    var app = apps[i];
    var manifest = app.getAttribute("manifest");

    function makeInstallFunc(appSpan, manifest)
    {
        return function() {
            navigator.mozApps.install(
                manifest,
                {},
                function(done) {
                    appSpan.style.display = "none";
                },
                function(err) {
                    alert("Oh no, there was an error " + err);
                }
            );
        }
    };
    app.onclick = makeInstallFunc(app, manifest);
}
