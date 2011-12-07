
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
	
	App.renderList = function(list) {
		console.log('Apps.renderList ' + list.length);

		var container = document.getElementById('myapps');
		container.innerHTML = '';
		
		if (!list || !list.length) {
			container.innerHTML = 'No local apps, watching for sync updates …';
			return;
		}
		
		list.forEach(function(data) {
			var app = new soup.App(data);
			app.render(container);
		});
		
	}
	
	return App;
	
})();

document.addEventListener('deviceready', function() {

	function verify() {
		
		console.log('navigator.id.getVerifiedEmail START');
		
		navigator.id.getVerifiedEmail(function(assertion) {
		
			console.log('navigator.id.getVerifiedEmail DONE');
			console.log(assertion);
		
			if (assertion && typeof assertion == 'string') {
				navigator.mozApps.mgmt.list(soup.App.renderList);
				navigator.mozApps.mgmt.watchUpdates(soup.App.renderList);
			} else {
				var container = document.getElementById('myapps');
				container.getElementsByTagName('em')[0].innerHTML = 'Try sign in again!';
			}
			
		});
	}
	
	var login = document.getElementById('btn-login');
		
	if (login) {
		login.addEventListener('click', function(evt) {
			evt.preventDefault();
			
			verify();
		}, false);
	}
	
	verify();
	
}, false);

})(this);
