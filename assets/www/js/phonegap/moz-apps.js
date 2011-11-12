(function() {
	
function MozApps() {
};

MozApps.prototype.install = function(url, install_data, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'install',
    [url, install_data]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozApps', new MozApps());
});

})();
