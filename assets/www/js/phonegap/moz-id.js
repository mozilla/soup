(function() {
	
function MozId() {
};

MozId.prototype.getSession = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'getSession',
    []
  );
};

MozId.prototype.getVerifiedEmail = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'getVerifiedEmail',
    []
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozId', new MozId());
});

})();
