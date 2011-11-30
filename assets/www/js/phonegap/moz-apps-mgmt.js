(function(context) {

if (window.plugins.mozAppsMgmt) return;
	
function MozAppsMgmt() {
};

MozAppsMgmt.prototype.list = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsMgmtPlugin',
    'list',
    []
  );
};

MozAppsMgmt.prototype.launch = function(origin, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozAppsMgmtPlugin',
    'launch',
    [origin]
  );
};

MozAppsMgmt.prototype.watchUpdates = function(onSuccess) {
  return PhoneGap.exec(
    onSuccess,
    null,
    'MozAppsMgmtPlugin',
    'watchUpdates',
    []
  );
};

MozAppsMgmt.prototype.clearWatch = function(id) {
  return PhoneGap.exec(
    null,
    null,
    'MozAppsMgmtPlugin',
    'clearWatch',
    [id]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozAppsMgmt', new MozAppsMgmt());
});

})(this);
