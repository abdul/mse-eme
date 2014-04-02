
var https = require('https');
var http = require('http');
var fs = require('fs');

keys = {
    '10000000100010001000100000000001': new Buffer("3A2A1B68DD2BD9B2EEB25E84C4776668", 'hex'),
    '10000000100010001000100000000002': new Buffer("04714BD8D7E1F3815FC47D0A834F0E18", 'hex')
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
    var requested_kid = "10000000100010001000100000000001";
    var key_b64 = keys[requested_kid].toString('base64').replace(/=/g, "");
    var requested_kid_b64 =
        new Buffer("10000000100010001000100000000001", 'hex').toString('base64').replace(/=/g, "");
    var jwk = {
        kty: "oct",
        alg: "A128GCM",
        kid: requested_kid_b64,
        k: key_b64
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

