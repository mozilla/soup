(function(context) {

	// "use strict";

	if (navigator.mozSoup) {
		console.log('soup-addon.js SKIPPED');
		return;
	}
		
	var plugins = (context.plugins = context.plugins || {}), empty = function() {
		console.log('soup-addon.js empty()');
	};
	// TODO: Implement promise to have all calls pending until phonegap plugins are ready

	
	/**
	 * .id bridge
	 */

	var id = (navigator.id = navigator.id || {});
	
	(function bridgeId() {

		var audience, origin, assertion, popup, timer;

		id.getVerifiedEmail = function(callback) {
			plugins.mozId.preVerify(function(evt) {
				audience = evt.audience;
				origin = evt.origin;
				
				if (evt.assertion) {
					callback(evt.assertion);
					return;
				}
				
				popup = window.open(evt.url);
				
				var data = JSON.stringify({
					origin: location.protocol + '//' + location.host,
					audience: audience
				});
				
				timer = setInterval(function() {
					if (popup) popup.postMessage(data, origin);
				}, 50);
				
				function onmessage(evt) {
					if (timer) {
						clearInterval(timer);
						timer = null;
					}
		
					if (evt.origin != origin || !popup)
						return;
					
					window.removeEventListener('message', onmessage, false);
					if (popup) {
						popup.close();
						popup = null;
					}
		
					plugins.mozId.postVerify(audience, evt.data || null, oncomplete, oncomplete);
				};
				
				window.addEventListener('message', onmessage, false);
				
				function oncomplete(assertion) {
					callback(assertion);
				};
				
			});
		};

		console.log('soup-addon.js bridged id on ' + (location.host || 'file://'));
	})();


	/**
	 * .id.channel bridge
	 */

	(function bridgeIdChannel() {

		var channel = (id.channel = id.channel || {});

		channel.registerController = function(controller) {
			var origin, fired;

			window.addEventListener('message', function(evt) {
				if (origin) return;
				
				var data = JSON.parse(evt.data);
				origin = data.origin;
				
				controller.getVerifiedEmail(data.audience, function(assertion) {
					if (!fired) opener.postMessage(assertion, origin);
					fired = true;
				}, function() {
					if (!fired) opener.postMessage(null, origin);
					fired = true;
				});
				
			}, false);

		};

		console.log('soup-addon.js bridged id.channel on ' + (location.host || 'file://'));
	})();

	
	/**
	 * .apps bridge && .apps.mgmt
	 */

	var apps = (navigator.mozApps = navigator.mozApps || {});
	
	(function bridgeApps() {

		apps.install = function(url, install_data, onsuccess, onerror) {
			if (!plugins.mozApps)
				(onerror || empty)();
			else
				plugins.mozApps.install(url, install_data, onsuccess, onerror);
		};

		apps.amInstalled = function(onsuccess, onerror) {
			if (!plugins.mozApps)
				(onerror || empty)();
			else
				plugins.mozApps.amInstalled(onsuccess, onerror);
		};

		apps.enumerate = apps.getInstalledBy = function(onsuccess, onerror) {
			if (!plugins.mozAppsMgmt)
				(onsuccess || empty)([]);
			else
				plugins.mozAppsMgmt.list(onsuccess, onerror);
		};

		apps.mgmt = apps.mgmt || {};

		apps.mgmt.list = function(onsuccess, onerror) {
			if (!plugins.mozAppsMgmt)
				(onsuccess || empty)([]);
			else
				plugins.mozAppsMgmt.list(onsuccess, onerror);
		};

		apps.mgmt.launch = function(origin, onsuccess, onerror) {
			if (!plugins.mozAppsMgmt)
				(onerror || empty)();
			else
				plugins.mozAppsMgmt.launch(origin, onsuccess, onerror);
		};
		
		navigator.mozApps = apps;

		console.log('soup-addon.js bridged apps on ' + (location.host || 'file://'));

	})();

	// END bridge

	// Fix post-load injected phonegap dependence on onDOMContentLoaded
	if (document.readyState == 'complete' && !PhoneGap.onDOMContentLoaded.fired) {
		PhoneGap.onDOMContentLoaded.fire();
	}
})(this);
