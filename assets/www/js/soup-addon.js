(function(context) {
	
	context.$soup = true;

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
	
	console.log('navigator.id bridged ' + location.host);
	
})(this);