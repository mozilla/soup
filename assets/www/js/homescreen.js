var HomeScreen = function() {
};

HomeScreen.prototype.add = function(url, title, icon, onSuccess, onError) {
  return PhoneGap.exec(
    onSuccess,
    onError,
    'HomeScreenPlugin',
    'add',
    [url, title, icon]
  );
};

PhoneGap.addConstructor(function() {
  PhoneGap.addPlugin("homeScreen", new HomeScreen());
});
