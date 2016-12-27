var http = require('http');
var moment = require('moment');
var config = require('./config');
var mail = require('./mail');

exports.handler = function(event, context) {
  console.log('starting function');
  moment.locale('de');
  var today = moment().startOf('day');

  http.get(config.events_url, function(res) {
    var body = '';
    res.on('data', function(chunk) {body += chunk});
    res.on('end', function() {
      var data = JSON.parse(body);
      var event = data[0];
      if (event.start) {
        var m = moment(event.start).startOf('day');
        var diff = m.diff(today, 'days');
        console.log('processing event id ' + event.uid + " / days: " + diff);
        mail(event, diff);
      }
    });
  }).on('error', function(e) {
    console.error(e);
  });

  setTimeout(function() {
    context.done();
  }, 3000);

};
