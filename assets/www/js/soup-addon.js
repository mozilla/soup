(function _moz_soup_init() {
	
	"use strict";
	
	// console.log('plugins.mozId ' + ((window.plugins && plugins.mozId) || 'No mozId'));
	
	// Fix post-load injected phonegap dependence on onDOMContentLoaded
	if (document.readyState == 'complete' && window.PhoneGap && !PhoneGap.onDOMContentLoaded.fired) {
		PhoneGap.onDOMContentLoaded.fire();
	}

	if (window._moz_soup) return;

	// if (location == 'about:blank') {
		// setTimeout(_moz_soup_init, 10);
		// return;
	// }
	
	window._moz_soup = true;
	
	var plugins = (window.plugins = window.plugins || {}), empty = function() {};
	
	// TODO: Implement promise to have all calls pending until phonegap plugins are ready
	
	/**
	 * .id bridge
	 */

	var id = (navigator.id = navigator.id || {});
	
	(function bridgeId() {

		var audience, origin, assertion, popup, timer;

		id.getVerifiedEmail = function(callback) {
			console.log("getVerifiedEmail");
			
			plugins.mozId.preVerify(function(evt) {
				audience = evt.audience;
				origin = evt.origin;
				email = evt.email || null;
				
				console.log("getVerifiedEmail for " + audience);
				
				if (evt.assertion) {
					setTimeout(function() {
						callback(evt.assertion);
					}, 10);
					return;
				}
				
				var data = JSON.stringify({
					origin: location.protocol + '//' + location.host,
					audience: audience,
					email: email
				});
				
				console.log("getVerifiedEmail starts with " + data);
				
				function oncomplete(assertion) {
					callback(assertion);
				};
				function onmessage(evt) {
					evt.stopPropagation();
					
					if (timer) {
						console.log("getVerifiedEmail cleaned postMessage setInterval");
						clearInterval(timer);
						timer = null;
					}
		
					if (evt.origin != origin || !popup)
						return;
					
					window.removeEventListener('message', onmessage, false);
					
					if (popup) {
						if (popup.close) popup.close();
						popup = null;
					}
		
					plugins.mozId.postVerify(audience, evt.data || null, oncomplete, oncomplete);
				};
				
				window.addEventListener('message', onmessage, false);
				
				popup = window.open(evt.url, '_moz_verify');
				
				timer = setInterval(function() {
					if (!popup || !popup.postMessage) {
						console.log("getVerifiedEmail killed postMessage setInterval");
						clearInterval(timer);
						timer = null;
					} else {
						popup.postMessage(data, origin);
					}
				}, 50);
			});
		};

		console.log('soup-addon.js bridged *id* on ' + (location.host || location));
	})();


	/**
	 * .id.channel bridge
	 * 
	 * Targets browser-id pop ups.
	 */

	(function bridgeIdChannel() {
		
		// TODO: Filter to pop up!

		var channel = (id.channel = id.channel || {});

		channel.registerController = function(controller) {
			
			console.log("getVerifiedEmail channel.registerController started");
			
			var origin, fired;

			window.addEventListener('message', function(evt) {
				evt.stopPropagation();
				
				if (origin) return;
				
				var data = JSON.parse(evt.data);
				origin = data.origin;
				
				console.log("getVerifiedEmail received first postMessage with " + evt.data);
				
				var cb = function(assertion) {
					if (!fired) opener.postMessage(assertion, origin);
					fired = true;
				};
				
				if (data.email && 'BrowserID' in window && 'User' in BrowserID) {
					console.log("getVerifiedEmail using BrowserID.User for " + data.email);
					
					BrowserID.User.setOrigin(data.audience);
					BrowserID.User.getAssertion(data.email, cb, cb);
				} else {
					console.log("getVerifiedEmail using controller.getVerifiedEmail " + data.audience);
					
					controller.getVerifiedEmail(data.audience, cb, cb);
				}
				
			}, false);

		};
		
		var controller = '$' in window && $('body').controller && $('body').controller('dialog');
		// Script might got injected after load
		if (controller) {
			console.log("getVerifiedEmail channel.registerController restarted");
			channel.registerController(controller);
		}

		console.log('soup-addon.js bridged *id.channel* on ' + (location.host || location));
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
			console.log('apps.mgmt.list');
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
		
		console.log('soup-addon.js bridged *apps* on ' + (location.host || location));

	})();

	// END bridge
	
	console.log('soup-addon.js bridged on ' + (location.host || location));
	
})();
