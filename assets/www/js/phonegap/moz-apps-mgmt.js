(function() {
	
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

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozAppsMgmt', new MozAppsMgmt());
});

})();
