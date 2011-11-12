(function(context) {

console.log(document.lastChild.innerHTML.replace(/\s/g, ''));

	// "use strict";
	
	// There can be only one
	if (navigator.mozSoup) return;
	navigator.mozSoup = {};
	

	// .id bridge
	
	navigator.id = navigator.id || {};

	navigator.id._getVerifiedEmailOrigin = navigator.id.getVerifiedEmail;

	navigator.id.getVerifiedEmail = function(callback) {
		console.log('bridged navigator.id.getVerifiedEmail');

		navigator.id._getVerifiedEmailOrigin(function(assertion) {
			console.log(assertion);
			callback(assertion);
		});
	};
	
	
	// .apps bridge
	
	navigator.mozApps = navigator.mozApps || {};
	
	function getBiggestIcon(manifest) {
    // see if the manifest has any icons, and if so, return the largest one
    if (manifest.icons) {
        var biggest = 0;
        for (z in manifest.icons) {
            var size = parseInt(z, 10);
            if (size > biggest) biggest = size;
        }
        if (biggest !== 0) return manifest.icons[biggest];
    }
    return null;
	};
	
	navigator.mozApps.install = function(url, install_data, onsuccess, onerror) {
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
	};
	
	// .apps.mgmt bridge
	
	navigator.mozApps.mgmt = navigator.mozApps.mgmt || {};
	
	navigator.mozApps.mgmt.list = function(onsuccess, onerror) {
		console.log('navigator.mozApps.mgmt.list');
		window.plugins.mozAppsMgmt.list(onsuccess, onerror);
	};
	
	navigator.mozApps.mgmt.launch = function(origin, onsuccess, onerror) {
		console.log('navigator.mozApps.mgmt.launch, ' + origin);
		window.plugins.mozAppsMgmt.launch(origin, onsuccess, onerror);
	};
	
	// END bridge

	console.log('navigator.id bridged ' + (location.host || 'file://') + ': ' + document.readyState);
	
	// Fix post-load injected phonegap dependence on onDOMContentLoaded
	if (document.readyState == 'complete' && !PhoneGap.onDOMContentLoaded.fired) {
		PhoneGap.onDOMContentLoaded.fire();
	}

})(this);
