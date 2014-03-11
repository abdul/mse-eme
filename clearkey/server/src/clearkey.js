
var https = require('https');
var http = require('http');
var fs = require('fs');

var keys = {
    10000000100010001000000000000001: '04714BD8D7E1F3815FC47D0A834F0E17',
    10000000100010001000000000000002: '04714BD8D7E1F3815FC47D0A834F0E17'
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
    var requested_kid = "10000000100010001000000000000001";
    var jwk = {
        kty: "oct",
        alg: "A128GCM",
        kid: requested_kid,
        k: keys[requested_kid]
    };
    var jwk_array = [];
    jwk_array.push(jwk);
    var response = {
        keys: jwk_array
    };
    var json_str_response = JSON.stringify(response);
    addCORSHeaders(res, json_str_response.length);
    res.write(json_str_response);
    res.end();

}).listen(8584);

