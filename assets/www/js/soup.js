
function getBiggestIcon(minifest) {
    // see if the minifest has any icons, and if so, return the largest one
    if (minifest.icons) {
        var biggest = 0;
        for (z in minifest.icons) {
            var size = parseInt(z, 10);
            if (size > biggest) biggest = size;
        }
        if (biggest !== 0) return minifest.icons[biggest];
    }
    return null;
}

navigator.mozApps = {
	install: function(url, install_data, onsuccess, onerror) {
        if (url.substr(0, 7) == "http://") {
	    // Fetch Manifest
        var req = new XMLHttpRequest();
        req.open("GET", url, true);
        req.onreadystatechange = function(evt) {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var manifest = JSON.parse(req.responseText);
                    var origin = URLParse(url).normalize().originOnly().toString();
                    var launch = origin;
                    var icon = getBiggestIcon(manifest);

                    if (icon.slice(0, 5) != "data:")
                        icon = origin + icon;
                    if ('launch_path' in manifest)
                        launch += manifest.launch_path;

                    window.plugins.homeScreen.add(launch, manifest.name, icon);
                } else {
                    alert("Could not install app!");
                }
            }  
        };
        req.send();
        } else {
            switch (url) {
                case "etherpal":
                    window.plugins.homeScreen.add("http://etherpal.org", "Etherpal", "http://proness.kix.in/misc/etherpal48.png");
                    break;
                case "grantland":
                    window.plugins.homeScreen.add("http://grantland.com/", "Grantland", "http://proness.kix.in/misc/grantland48.png");
                    break;
                case "aprilzero":
                    window.plugins.homeScreen.add("http://aprilzero.com/", "April Zero", "http://proness.kix.in/misc/aprilzero96.png");
                    break;
                case "halma":
                    window.plugins.homeScreen.add("http://diveintohtml5.info/examples/offline/halma.html", "Halma", "http://proness.kix.in/misc/halma96.png");
                    break;
            }
        }
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
