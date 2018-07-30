var express = require("express");

var app = express();
app.use('/', express.static(__dirname + '/'));

app.listen(8080, function(){
    console.log('Running app on port 8080');
});
