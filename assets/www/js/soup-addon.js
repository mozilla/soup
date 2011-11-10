(function(context) {

	if(context.$soup)
		return;
	context.$soup = true;

	// Fix injected phonegap	
	if (document.readyState == 'complete') {
		PhoneGap.onDOMContentLoaded.fire();
	}
	
	document.addEventListener("deviceready", function() {
		navigator.notification.beep(1);
	}, false);


	var readyInterval = context.setInterval(function() {
		if (Device.available) {
			context.clearInterval(readyInterval);
		}
	}, 1000);


	// "use strict";

	// TODO: .apps bridge
	// navigator.mozApps = navigator.mozApps || {};

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

	console.log('navigator.id bridged ' + (location.host || 'file://'));

})(this);
