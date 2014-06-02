
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

var https = require('https');
var http = require('http');
var fs = require('fs');
var url = require('url');

keys = {
    '10000000100010001000100000000001': new Buffer("3A2A1B68DD2BD9B2EEB25E84C4776668", 'hex'),
    '10000000100010001000100000000002': new Buffer("07E4D653CFB45C66158D93FFCE422907", 'hex'),
    'A1B1C1D1A2B2A3B3A4B4A5B5C5D5E5F5': new Buffer("2539FA84B987416009A7FBBA11B239AB", 'hex')
};

var options = {
    key: fs.readFileSync('security/cl_clearkey-key.pem'),
    cert: fs.readFileSync('security/cl_clearkey-cert.pem')
};

var addCORSHeaders = function(res, length) {
    res.writeHeader(200, {
        "Content-Length": length,
        "Content-Type": 'application/json',
        "Access-Control-Allow-Origin": '*',
        "Access-Control-Allow-Methods": 'GET, PUT, POST, DELETE, OPTIONS',
        "Access-Control-Allow-Headers": 'Content-Type, Authorization, Content-Length, X-Requested-Width'});
};

https.createServer(options, function(req, res) {
    addCORSHeaders(res);
    res.end("hello world\n");

}).listen(8585);

http.createServer(function(req, res) {
    var parsed_url = url.parse(req.url, true);
    var query = parsed_url.query;

    // Validate query string
    if (query === undefined || query.keyid === undefined) {
        res.writeHeader(400, "Illegal query string");
        res.end();
    }

    var keyIDs = [];
    if (query.keyid instanceof Array) {
        keyIDs = query.keyid;
    } else {
        keyIDs.push(query.keyid);
    }

    var jwk_array = [];
    for (var i = 0; i < keyIDs.length; i++) {
        var jwk = {
            kty: "oct",
            alg: "A128GCM",
            kid: new Buffer(keyIDs[i], 'hex').toString('base64').replace(/=/g, ""),
            k: keys[keyIDs[i]].toString('base64').replace(/=/g, "")
        };
        jwk_array.push(jwk);
    }
    var response = {
        keys: jwk_array
    };
    var json_str_response = JSON.stringify(response);
    addCORSHeaders(res, json_str_response.length);
    res.write(json_str_response);
    res.end();

}).listen(8584);

