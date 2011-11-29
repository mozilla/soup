
(function(context) {

Function.prototype.bind = Function.prototype.bind || function(obj) {
  var fn = this, headArgs = Array.prototype.slice.call(arguments, 1);
  var bound = function() {
    var args = Array.prototype.concat.apply(headArgs, arguments);
    return fn.apply(obj, args);
  };
  return bound;
};


var soup = context.soup = {};

soup.App = (function() {
	
	function App(data) {
		
		this.data = data;
		this.manifest = data.manifest;
		
	};
	
	App.prototype.render = function(container) {
		
		var el = document.createElement('div');
		el.className = 'app';
		
		var img = document.createElement('img');
		if (this.manifest.icons && this.manifest.icons['128']) {
			img.src = this.data.origin + this.manifest.icons['128'];
		} else {
			img.src = 'http://proness.kix.in/misc/etherpal96.png';
		}
		
		var title = document.createElement('div');
		title.textContent = this.manifest.name;
		
		el.appendChild(img);
		el.appendChild(title);
		
		el.onclick = this.launch.bind(this);
		
		container.appendChild(el);
		
	};
	
	App.prototype.launch = function() {
		
		navigator.mozApps.mgmt.launch(this.data.origin);
		
	};
	
	return App;
	
})();

document.addEventListener('deviceready', function() {

	console.log("navigator.id.getVerifiedEmail");
	
	navigator.id.getVerifiedEmail(function(assertion) {
		
		navigator.mozApps.mgmt.list(function(list) {
			var container = document.getElementById('myapps');
			container.innerHTML = '';
			
			if (!list || !list.length) {
				container.innerHTML = 'No apps installed!';
				return;
			}
			
			list.forEach(function(data) {
				var app = new soup.App(data);
				app.render(container);
			});
			
			// login button
			
			var login = document.getElementById('btn-login');
			
			if (login) {
				login.addEventListener('click', function(evt) {
					evt.preventDefault();
					
					navigator.id.getVerifiedEmail(function(assertion) {
						navigator.notification.alert('Thanks for logging in!');
						
						login.style.display = 'none';
					});
				}, false);
			}
		});
	});
	

	// var apps = document.getElementsByClassName("app");
	// for (var i = 0; i < apps.length; i++) {
	    // var app = apps[i];
	    // var manifest = app.getAttribute("data-manifest");

	    // function makeInstallFunc(appSpan, manifest)
	    // {
	        // return function() {
	            // navigator.mozApps.install(
	                // manifest,
	                // {},
	                // function(done) {
	                    // appSpan.style.display = "none";
	                // },
	                // function(err) {
	                    // alert("Oh no, there was an error " + err);
	                // }
	            // );
	        // }
	    // };
	    // app.onclick = makeInstallFunc(app, manifest);
	// }

}, false);

})(this);
