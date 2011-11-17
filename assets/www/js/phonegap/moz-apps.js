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

MozApps.prototype.amInstalled = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'amInstalled',
    []
  );
};

MozApps.prototype.amInstalledBy = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsPlugin',
    'amInstalledBy',
    []
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozApps', new MozApps());
});

})();
