(function _moz_soup_init() {
	
	"use strict";
	
	var plugins = (window.plugins = window.plugins || {}), empty = function() {};
	
	function logPhonegapChannel(name) {
		if (PhoneGap[name].fired) {
			console.log(name + ' FIRED');
		} else {
			console.log(name + ' pending');
			PhoneGap[name].subscribeOnce(function() {
				console.log(name + ': FIRED delayed');
			})
		}
	};
	
	function promise(cb) {
		// Fix post-load injected phonegap dependence on onDOMContentLoaded
		if (window.PhoneGap) {
			if (PhoneGap.onPhoneGapInit.fired) {
				console.log('promise instant FIRE');
				return cb();
			} else {
				console.log('promise delayed');
				
				PhoneGap.onPhoneGapInit.subscribeOnce(cb);
				
				if (!PhoneGap.onNativeReady.fired) PhoneGap.onNativeReady.fire();
				if (!PhoneGap.onDOMContentLoaded.fired) PhoneGap.onDOMContentLoaded.fire();
			}
			
		}
	};
	
	
	/**
	 * .id bridge
	 */

	var id = (navigator.id = navigator.id || {});
	
	(function bridgeId() {

		id.getVerifiedEmail = function(callback) {
			
			promise(function() {
				
				plugins.mozId.preVerify(function(evt) {
					var audience = evt.audience;
					var origin = evt.origin;
					var email = evt.email || null;
					
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
					
					var popup = window.open(evt.url, '_moz_verify');
					
					var timer = setInterval(function() {
						if (!popup || !popup.postMessage) {
							console.log("getVerifiedEmail killed postMessage setInterval");
							clearInterval(timer);
							timer = null;
						} else {
							popup.postMessage(data, origin);
						}
					}, 50);
				});
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
					if (fired) return;
					fired = true;
					
					console.log("getVerifiedEmail received callback with assertion: " + (assertion != null));
					
					opener.postMessage(assertion, origin);
				};
				
				if (data.email && 'BrowserID' in window && 'User' in BrowserID) {
					console.log("getVerifiedEmail using BrowserID.User for " + data.email);
					
					BrowserID.User.setOrigin(data.audience);
					
					/*
					BrowserID.User.syncEmailKeypair(data.email, function() {
						BrowserID.User.getAssertion(data.email, cb, cb);
					}, function() {
						cb();
					});
					*/
					
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
			
			// added via stomlinson
			try {
				controller.stateMachine();
				console.log("getVerifiedEmail stateMachine success");
			} catch (e) {
				console.log("getVerifiedEmail stateMachine failed " + e);
			}
			
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
			promise(function() {
				plugins.mozApps.install(url, install_data, onsuccess, onerror);
			});
		};

		apps.amInstalled = function(onsuccess, onerror) {
			promise(function() {
				plugins.mozApps.amInstalled(onsuccess, onerror);
			});
		};

		apps.enumerate = apps.getInstalledBy = function(onsuccess, onerror) {
			promise(function() {
				plugins.mozAppsMgmt.list(onsuccess, onerror);
			});
		};

		apps.mgmt = apps.mgmt || {};

		apps.mgmt.list = function(onsuccess, onerror) {
			promise(function() {
				plugins.mozAppsMgmt.list(onsuccess, onerror);
			});
		};

		apps.mgmt.launch = function(origin, onsuccess, onerror) {
			promise(function() {
				plugins.mozAppsMgmt.launch(origin, onsuccess, onerror);
			});
		};

		apps.mgmt.watchUpdates = function(onsuccess) {
			return promise(function() {	
				return plugins.mozAppsMgmt.watchUpdates(onsuccess);
			});
		};
		
		apps.mgmt.clearWatch = function(id) {
			return promise(function() {
				return plugins.mozAppsMgmt.clearWatch(id);
			});
		};
		
		console.log('soup-addon.js bridged *apps* on ' + (location.host || location));

	})();

	// END bridge
	
	console.log('soup-addon.js bridged on ' + (location.host || location));
	
})();
