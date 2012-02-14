(function _moz_soup_init() {
	
	"use strict";
	
	var plugins = (window.plugins = window.plugins || {}), empty = function() {};
	

	function promise(cb) {
		// Fix post-load injected phonegap dependence on onDOMContentLoaded
		if (window.PhoneGap) {
			if (PhoneGap.onPhoneGapInit.fired) {
				cb();
			} else {
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

		id.get = id.getVerifiedEmail = function(callback, options) {
			
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
							console.log("getVerifiedEmail received callback from popup");
							clearInterval(timer);
							timer = null;
						}
			
						if (evt.origin != origin || evt.data === false || !popup) {
							console.log("getVerifiedEmail dropped callback");
							return;
						}
						
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

	})();


	/**
	 * .id.channel bridge
	 * 
	 * Targets browser-id pop ups.
	 */

	(function bridgeIdChannel() {
		
		// TODO: Filter to pop up!

		function ready() {
			if (!('BrowserID' in window) || !('internal' in BrowserID)) {
				console.log('bridgeIdChannel FAILED for ' + location);
				return;
			}
			
			if (!ready) return;
			ready = null;
			
			console.log("getVerifiedEmail ready");
			
			var origin, fired;
			
			window.addEventListener('message', function(evt) {
				evt.stopPropagation();
				
				if (origin) return;
				
				var data = JSON.parse(evt.data);
					
				origin = data.origin
				
				console.log("getVerifiedEmail received first postMessage with " + evt.data);
				opener.postMessage(false, origin); // this stops further awake calls from the window
				
				var cb = function(assertion) {
					if (fired) return;
					fired = true;
					
					console.log("getVerifiedEmail posting assertion (" + (assertion !== null) + ") back to " + origin);
					
					opener.postMessage(assertion || null, origin);
				};
				
				
				if (data.email) {
					
					// User has an email, so we just get a new assertion
					BrowserID.internal.setPersistent(origin, function() {
						BrowserID.internal.get(data.audience, cb, {requiredEmail: data.email, silent: true});
					});
					
				} else {
					
					// User has no email provided yet, the dialog needs to do the
					// full flow
					BrowserID.internal.setPersistent(origin, function() {
						BrowserID.internal.get(data.audience, cb, {silent: false});
					});

				}
				
			}, false);

		};
		
		if (!('$' in window)) return;
		
		if (document.readyState == 'complete') {
			console.log("getVerifiedEmail INSTANT on " + location);
			setTimeout(ready, 10);
		} else {
			console.log("getVerifiedEmail DOMContentLoaded on " + location);
			window.addEventListener('load', function() {
				console.log("getVerifiedEmail DOMContentLoaded FIRED on " + location);
				setTimeout(ready, 10);
			}, false);
		}

	})();

	
	/**
	 * .apps bridge && .apps.mgmt
	 */

	var apps = (navigator.mozApps = navigator.mozApps || {});
	
	(function bridgeApps() {
		
		// Application
		
		function Application(app) {
			
			console.log('new Application: ' + JSON.stringify(app));

			this.manifest = app.manifest;
			this.manifestURL = app.manifestURL || app.manifest_url;
			
			this.origin = app.origin;
			this.installOrigin = app.installOrigin || app.install_origin;
			this.installTime = app.installTime || app.install_time;
			
			var data = app.installData || app.install_data;
			
			if (data) {
				if (data.receipts) {
					this.receipts = data.receipts;
				} else if (data.receipts) {
					this.receipts = [data.receipt];
				}
			}
		};
		
		Application.prototype.launch = function(startPoint) {
			
			var stub = new RequestStub();
			var origin = this.origin;
			
			promise(function() {
				plugins.mozAppsMgmt.launch(origin, stub.success, stub.error);
			});
			
			return stub.request;
			
		};
		
		Application.prototype.uninstall = function() {
			throw new Error('NOT_IMPLEMENTED');
		};
		
		Application.toApp = function(app) {
			return new Application(app);
		};
		
		// Request
		
		function Request() {};
		
		Request.prototype.result = null;
		Request.prototype.error = null;
		Request.prototype.onerror = null;
		Request.prototype.onsuccess = null;
		
		function RequestStub() {
			
			var request = new Request();
			
			return {
				success: function(result) {
					console.log('RequestStub SUCCESS ' + JSON.stringify(result));
					
					if (result) {
						if (Array.isArray(result)) {
							result = result.map(Application.toApp);
						} else {
							result = Application.toApp(result);
						}
					}
					
					request.result = result;
					if (request.onsuccess) {
						request.onsuccess();
					}
				},
				error: function(error) {
					console.log('RequestStub ERROR ' + error);
					
					request.error = error;
					
					if (request.onerror) {
						request.onerror();
					}
				},
				request: request
			};
			
		};
		
		
		apps.install = function(manifestUrl, parameters) {
			
			var stub = new RequestStub();
			
			promise(function() {
				plugins.mozApps.install(manifestUrl, parameters, stub.success, stub.error);
			});
			
			return stub.request;
		};
		
		apps.getSelf = function() {
			
			var stub = new RequestStub();
			
			promise(function() {
				plugins.mozApps.getSelf(location.protocol + '//' + location.host, stub.success, stub.error);
			});
			
			return stub.request;
		};

		apps.getInstalled = function() {
			var stub = new RequestStub();
			
			promise(function() {
				plugins.mozApps.getInstalled(location.protocol + '//' + location.host, stub.success, stub.error);
			});
			
			return stub.request;
		};

		apps.mgmt = apps.mgmt || {};
		
		var polling = false;

		apps.mgmt.getAll = function() {
			
			var stub = new RequestStub();
			
			promise(function() {
				plugins.mozAppsMgmt.getAll(stub.success, stub.error);
				
				if (!polling) {
					setInterval(function() {
						plugins.mozAppsMgmt.sync();
					}, 1000 * 60);
					
					plugins.mozAppsMgmt.watchUpdates(function(lists) {
						var installed = lists[0] || [];
						var uninstalled = lists[1] || [];
						
						if (Array.isArray(installed) && installed.length) {
							installed.forEach(function(app) {
								fireEventListener('install', Application.toApp(app));
							});
						}
						
						if (Array.isArray(uninstalled) && uninstalled.length) {
							uninstalled.forEach(function(app) {
								fireEventListener('uninstall', Application.toApp(app));
							});
						}
					});
				}
			});
			
			return stub.request;
		};
		
		var mgmtListeners = {};
		
		apps.mgmt.addEventListener = function(name, callback) {
			mgmtListeners[name] = mgmtListeners[name] || [];
			mgmtListeners[name].push(callback);
		};

		apps.mgmt.removeEventListener = function(name, callback) {
			mgmtListeners[name] = (mgmtListeners[name] || []).filter(function(fn) {
				return fn != callback;
			});
		};
		
		function fireEventListener(name, app) {
			var callbacks = (mgmtListeners[name] || []).splice(0);
			
			if (apps.mgmt['on' + name]) {
				callbacks.push(apps.mgmt['on' + name]);
			}
			
			callbacks.forEach(function(callback) {
				setTimeout(function() {
					callback.call(apps.mgmt, {application: app});
				}, 1);
			});
		};
		
	})();

	// END bridge
	
	
	console.log('soup-addon.js bridged on ' + (location.host || location));
	
})();
