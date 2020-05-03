var exec = require('cordova/exec');

exports.checkTemperature = function(success, error) {
    exec(success, error, 'HprtPrinter', 'checkTemperature');
};

exports.isDeviceCompatible = function(success, error) {
    exec(success, error, 'HprtPrinter', 'isDeviceCompatible');
};