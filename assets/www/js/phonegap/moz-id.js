(function(context) {

if (window.plugins.mozId) return;

function MozId() {
};

MozId.prototype.preVerify = function(onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'preVerify',
    []
  );
};


MozId.prototype.duringVerify = function(audience, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'duringVerify',
    [audience]
  );
};


MozId.prototype.postVerify = function(audience, assertion, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'MozIdPlugin',
    'postVerify',
    [audience, assertion]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin('mozId', new MozId());
});

})(this);
