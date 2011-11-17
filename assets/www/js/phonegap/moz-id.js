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

MozId.prototype.postVerify = function(assertion, nSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'postVerify',
    [assertion]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozId', new MozId());
});

})();
