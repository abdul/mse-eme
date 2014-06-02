
/* Copyright (c) 2014, CableLabs, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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

// Default server values (not signed, widevine test account)
var signed = false;
var wvServer = {};
wvServer.url = "https://license.uat.widevine.com";
wvServer.provider = "widevine_test";

// Other required modules
var https = require('https');
var http = require('http');
var crypto = require('crypto');
var url = require('url');
var Q = require('q');

// Look for server definition module on the command line
var args = process.argv.slice(2);
if (args.length > 0) {
    wvServer = require(args[0]);
    signed = true;
}

// Validate server URL
var urlParsed = url.parse(wvServer.url);
var client;
if (urlParsed.protocol === "https:")
    client = https;
else if (urlParsed.protocol === "http:")
    client = http;
else {
    console.log("Illegal server URL: " + wvServer.url);
    process.exit(1);
}
urlParsed.method = 'POST';

console.log("Starting proxy server.  DRM Server Info:");
console.log(wvServer);

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

    console.log("License request message:");
    console.log(JSON.stringify(requestMessage, undefined, 2));

    var requestMessageJSON = JSON.stringify(requestMessage);

    var request = {};
    request.request = (new Buffer(requestMessageJSON, "utf8")).toString("base64");

    if (signed) {

        console.log("Signing request (signer = " + wvServer.provider + ")...");

        var sha1Hash = crypto.createHash("sha1");
        sha1Hash.update(requestMessageJSON, "utf8");
        var sha1 = sha1Hash.digest();

        var aesCipher = crypto.createCipheriv("aes-256-cbc",
                (new Buffer(wvServer.key, "base64")), (new Buffer(wvServer.iv, "base64")));

        request.signature = Buffer.concat( [
            aesCipher.update(sha1, "binary"),
            aesCipher.final()
        ]).toString("base64");
        request.signer = wvServer.provider;
    }

    console.log("Sending request to license server:");
    console.log(JSON.stringify(request, undefined, 2));

    var deferred = Q.defer();
    var httpReq = client.request(urlParsed, function(res) {

        if (res.statusCode !== 200) {
            deferred.reject("Received error status code from license server: " + res.statusCode);
        }
        else {
            var resp = "";
            res.on('data', function (data) {
                resp += data;
            });
            res.on('end', function() {
                deferred.resolve(resp);
            });
        }
    });
    httpReq.on('error', function(e) {
        deferred.reject("Error sending request to license server: " + e.message);
    });
    httpReq.write(JSON.stringify(request));
    httpReq.end();
    return deferred.promise;
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

    var sendResponse = function(resp) {
        var respJSON = JSON.stringify(resp);
        addCORSHeaders(res, respJSON.length);
        res.write(respJSON);
        res.end();
    };

    var payload = "";
    req.on('data', function(data) {
        payload += data;
    });
    req.on('end', function() {

        console.log("Request received! Data length = " + payload.length);

        var proxyResp = {};
        sendLicenseRequest(payload).then(
                function (response_data) {
                    var respJSON = JSON.parse(response_data);


                    if (respJSON.status === "OK") {
                        proxyResp.status = "OK";
                        proxyResp.message = "";
                        proxyResp.license = respJSON.license;
                    }
                    else {
                        proxyResp.status = "ERROR";
                        proxyResp.message = respJSON.status;
                    }
                    sendResponse(proxyResp);
                },
                function (error) {
                    var message = "Error in request to license server: " + error;
                    console.log(message);
                    proxyResp.status = "ERROR";
                    proxyResp.message = message;
                    sendResponse(proxyResp);
                }
        );
    });

}).listen(8025);

