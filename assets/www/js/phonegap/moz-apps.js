(function(context) {

if (window.plugins.mozApps) return;
	
function MozApps() {
};

MozApps.prototype.install = function(url, parameters, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'install',
    [url, parameters]
  );
};

MozApps.prototype.getSelf = function(origin, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'getSelf',
    [origin]
  );
};

MozApps.prototype.getInstalled = function(origin, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'getInstalled',
    [origin]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozApps', new MozApps());
});

})(this);
