// Confidential material under the terms of the Limited Distribution Non-disclosure
// Agreement between CableLabs and Comcast

/**
 * To include your own Widevine server credentials,
 * create a new node.js module file and pass the full path
 * of that module file (as would be necessary for passing
 * to the 'require' API) on the command line when running.
 * For example,
 *
 * node proxy.js /var/data/myserver
 *
 * In /var/data, there would exist a file called 'myserver.js'.  In that
 * file, you must define the following module, replacing your own
 * data values for 'url', 'key', 'iv', and 'provider'.
 *
 * var wvServer = {};
 *
 * // Server URL
 * wvServer.url = "https://license.widevine.com/cenc/your_url";
 *
 * // 32-byte request signing key in base64 format
 * wvServer.key = "HtQS+5CSGFBt0NrjTTaXS9+tTwYWl12l2rsTi/+GQp4=";
 *
 * // 16-byte initialization vector used in signing the requests
 * wvServer.iv = "MD1GNrtCwMd1M/eoSwKb8Q==";
 *
 * // String provider name
 * wvServer.provider = "my_provider";
 *
 * module.exports = wvServer;
 */

// Default server values (not signed,
var signed = false;
var wvServer = {};
wvServer.url = "https://license.uat.widevine.com";
wvServer.provider = "widevine_test";

// Other required modules
var https = require('https');
var http = require('http');
var crypto = require('crypto');
var url = require('url');

// Look for server definition module on the command line
var args = process.argv.slice(2);
if (args.length > 0) {
    wvServer = require(args[0]);
    signed = true;
}

// Validate server URL
var urlParsed = url.parse(wvServer.url);
var client;
if (urlParsed.protocol === "https")
    client = https;
else if (urlParsed.protocol === "http")
    client = http;
else {
    console.log("Illegal server URL: " + wvServer.url);
    process.exit(1);
}
urlParsed.method = 'POST';

var addCORSHeaders = function(res, length) {
    res.writeHeader(200, {
        "Content-Length": length,
        "Content-Type": 'application/json',
        "Access-Control-Allow-Origin": '*',
        "Access-Control-Allow-Methods": 'GET, PUT, POST, DELETE, OPTIONS',
        "Access-Control-Allow-Headers": 'Content-Type, Authorization, Content-Length, X-Requested-Width'});
};

var sendLicenseRequest = function(data) {

    var requestMessage = {};
    requestMessage.payload = (new Buffer(data)).toString('base64');
    requestMessage.provider = wvServer.provider;
    requestMessage.allowed_track_types = "SD_HD";
    var requestMessageJSON = JSON.stringify(requestMessage);

    var request = {};
    request.request = (new Buffer(requestMessageJSON, "utf8")).toString("base64");

    if (signed) {

        var sha1Hash = crypto.createHash("sha1");
        sha1Hash.update(requestMessageJSON, "utf8");
        var sha1 = sha1Hash.digest();

        var aesCipher = crypto.createCipheriv("aes-256-cbc",
                (new Buffer(wvServer.key, "base64")), (new Buffer(wvServer.iv, "base64")));
        aesCipher.update(sha1);

        request.signature = aesCipher.final("base64");
        request.signer = wvServer.provider;
    }

    var client;

};

var cipherFound = false, hashFound = false;

// Test for required hash and cipher
crypto.getCiphers().forEach(function (val) {
    if (val === "aes-256-cbc") {
        cipherFound = true;
    }
});
if (!cipherFound) {
    console.log("Could not find support for 'aes-256-cbc' cipher!");
    process.exit(1);
}
crypto.getHashes().forEach(function (val) {
    if (val === "sha1") {
        hashFound = true;
    }
});
if (!hashFound) {
    console.log("Could not find support for 'sha1' hash!");
    process.exit(1);
}

http.createServer(function(req, res) {

    var payload = "";
    req.on('data', function(data) {
        payload += data;
    });
    req.on('end', function() {
        var licenseResponse = sendLicenseRequest(payload);

        addCORSHeaders(res, json_str_response.length);
        res.write(json_str_response);
        res.end();
    });

}).listen(8025);

