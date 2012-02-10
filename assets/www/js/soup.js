
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
		
		console.log('new App: ' + JSON.stringify(data));
		
		this.data = data;
		this.manifest = data.manifest;
		
		this.origin = this.manifest.origin;
		
		App.list[this.origin] = this;
		
	};
	
	App.prototype.render = function(container, animate) {
		
		var el = this.element = document.createElement('div');
		el.className = 'app';
		
		var img = document.createElement('img'),
			 sizeFound = 0;
			 
		if (this.manifest.icons) {
			
			for (var size in this.manifest.icons) {
				
				if (size > sizeFound) {
					sizeFound = size;
				}
			}
			
			if (sizeFound) {
				var found = this.manifest.icons[sizeFound];
				
				if (found.substr(0, 4) != 'data') {
					found = this.data.origin + found;
				}
				
				img.src = found;
			}
			
		}
		if (!sizeFound) {
			img.src = 'images/ic_app_default.png';
		}
		
		var title = document.createElement('div');
		title.textContent = this.manifest.name;
		
		el.appendChild(img);
		el.appendChild(title);
		
		el.onclick = this.launch.bind(this);
		
		container.appendChild(el);
		
	};
	
	App.prototype.remove = function() {

		this.element.parentNode.removeChild(this.element);
		
		App.list[this.origin] = null;
		this.element = this.data = null;
		
	};
	
	App.prototype.launch = function() {
		moveByy
		this.data.launch();
		
	};
	
	App.list = {};
	
	App.renderList = function(list, append) {
		
		if (!Array.isArray(list)) {
			list = [list];
		}
		
		console.log('Apps.renderList ' + JSON.stringify(list));

		var container = document.getElementById('myapps');
		
		if (!append || !container.getElementsByTagName('div').length) {
			container.innerHTML = '';
			
			if (!list || !list.length) {
				container.textContent = 'No installed apps, watching for sync updates …';
				return;
			}
		}
		
		list.forEach(function(data) {
			var app = new soup.App(data, append);
			app.render(container);
		});
		
	};
	
	App.removeFromList = function(app) {
		app = App.list[app.origin] || null;
		if (app) {
			app.remove();
		}
	};
	
	return App;
	
})();

document.addEventListener('deviceready', function() {
	
	function verify() {
		
		var container = document.getElementById('myapps');
		var log = container.getElementsByTagName('em')[0];
		
		console.log('navigator.id.getVerifiedEmail START');
		
		navigator.id.getVerifiedEmail(function(assertion) {
		
			console.log('navigator.id.getVerifiedEmail DONE');
		
			if (assertion) {
				var pending = navigator.mozApps.mgmt.getAll();
				pending.onsuccess = function() {
					soup.App.renderList(pending.result);
				};
				pending.onerror = function() {
					log.textContent = error;
				};

				navigator.mozApps.mgmt.oninstall = function(evt) {
					console.log('navigator.mozApps.mgmt.oninstall')
					soup.App.renderList(evt.application, true);
				};
				navigator.mozApps.mgmt.onuninstall = function(evt) {
					console.log('navigator.mozApps.mgmt.onuninstall')
					soup.App.removeFromList(evt.application);
				};
			} else {
				log.textContent = 'Try sign in again!';
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
