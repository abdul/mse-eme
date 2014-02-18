
var https = require('https');
var fs = require('fs');

var keys = {
    10000000100010001000000000000001: 0x3,
    10000000100010001000000000000002: 0x4
};

var options = {
    key: fs.readFileSync('security/cl_clearkey-key.pem'),
    cert: fs.readFileSync('security/cl_clearkey-cert.pem')
};

https.createServer(options, function(req, res) {
    res.writeHead(200);
    res.end("hello world\n");

}).listen(8585);

