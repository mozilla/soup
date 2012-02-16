
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
		
		this.origin = data.origin;
		
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
		title.className = 'title';
		title.textContent = this.manifest.name;
		
		el.appendChild(img);
		el.appendChild(title);
		
		el.onclick = this.install.bind(this);
		
		container.appendChild(el);
		
	};
	
	App.prototype.install = function() {

		navigator.mozApps.install(this.origin);
		
	};
	
	App.renderList = function(list) {
		
		if (!Array.isArray(list)) {
			list = [list];
		}
		
		console.log('Apps.renderList ' + JSON.stringify(list));

		var container = document.getElementById('myapps');
		
		list.forEach(function(data) {
			var app = new soup.App(data);
			app.render(container);
		});
		
	};
	
	return App;
	
})();

document.addEventListener('deviceready', function() {
	
	var request = new XMLHttpRequest();
	request.open('GET', 'testdb.json', false);   
	request.send(null);  
	
	var list;
	if (!request.responseText || !(list = JSON.parse(request.responseText))) {
		console.error(request.responseText || 'No response');
		return;
	}
	
	// FIXME: Seems to fail
	var cb = navigator.mozAppsMgmt && navigator.mozAppsMgmt.getAll();
	
	if (cb) {
		cb.onsuccess = function() {
			console.log(cb.result);
			console.log(JSON.stringify(cb.result));
			
			soup.App.renderList(list);
		};
	} else {
		soup.App.renderList(list);
	}
	
}, false);

})(this);
