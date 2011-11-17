(function(context) {

	// "use strict";

	// There can be only one
	if(navigator.mozSoup)
		return;

	var options = {};
	navigator.mozSoup = function(merge) {
		for(var key in merge) {
			if(merge.hasOwnProperty(key))
				options[key] = merge[key];
		}
	};
	var plugins = (context.plugins = context.plugins || {}), empty = function() {
		console.log('soup-addon.js empty()')
	};
	// TODO: Implement promise to have all calls pending until phonegap plugins are ready

	// .id bridge

	var id = (navigator.id = navigator.id || {});

	function bridgeId() {
		id._getVerifiedEmailOrigin = id.getVerifiedEmail;

		id.getVerifiedEmail = function(callback) {
			console.log('bridged navigator.id.getVerifiedEmail');

			id._getVerifiedEmailOrigin(function(assertion) {
				plugins.id.getVerifiedEmail(assertion, onsuccess);
			});
		};

		console.log('soup-addon.js bridged id on ' + (location.host || 'file://'));
	};

	// wait for navigator.id

	if(navigator.id) {
		bridgeId();
	} else {
		bridgeId.timer = setInterval(function() {
			if(!navigator.id)
				return;
			clearTimeout(bridgeId.timer);
			bridgeId();
		}, 50);
	}

	// .id.channel bridge

	// (function bridgeIdChannel() {
// 
		// id.channel = id.channel || {};
// 
		// var idController, fired = false;
// 
		// id.channel.registerController = function(controller) {
			// idController = controller;
// 
			// idController.getVerifiedEmail(navigator.mozSoup.id_host, function(assertion) {
				// if(!fired)
					// plugins.id.postVerify(assertion, oncomplete);
				// fired = true;
			// }, function() {
				// if(!fired)
					// plugins.id.postVerify(null, oncomplete);
				// fired = true;
			// });
		// };
// 		
		// function oncomplete() {
			// window.close();
		// };
// 
// 
		// console.log('soup-addon.js bridged id.channel on ' + (location.host || 'file://'));
	// })();

	// .apps bridge && .apps.mgmt

	var apps = (navigator.mozApps = navigator.mozApps || {}); (function bridgeApps() {

		apps.install = function(url, install_data, onsuccess, onerror) {
			if(!plugins.mozApps)
				(onerror || empty)();
			else
				plugins.mozApps.install(url, install_data, onsuccess, onerror);
		};

		apps.amInstalled = function(onsuccess, onerror) {
			if(!plugins.mozApps)
				(onerror || empty)();
			else
				plugins.mozApps.amInstalled(onsuccess, onerror);
		};

		apps.enumerate = apps.getInstalledBy = function(onsuccess, onerror) {
			if(!plugins.mozAppsMgmt)
				(onsuccess || empty)([]);
			else
				plugins.mozAppsMgmt.list(onsuccess, onerror);
		};

		apps.mgmt = apps.mgmt || {};

		apps.mgmt.list = function(onsuccess, onerror) {
			if(!plugins.mozAppsMgmt)
				(onsuccess || empty)([]);
			else
				plugins.mozAppsMgmt.list(onsuccess, onerror);
		};

		apps.mgmt.launch = function(origin, onsuccess, onerror) {
			if(!plugins.mozAppsMgmt)
				(onerror || empty)();
			else
				plugins.mozAppsMgmt.launch(origin, onsuccess, onerror);
		};

		console.log('soup-addon.js bridged apps on ' + (location.host || 'file://'));

	})();

	// END bridge

	// Fix post-load injected phonegap dependence on onDOMContentLoaded
	if(document.readyState == 'complete' && !PhoneGap.onDOMContentLoaded.fired) {
		PhoneGap.onDOMContentLoaded.fire();
	}

})(this);
