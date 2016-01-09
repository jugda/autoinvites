var ical = require('ical');
var moment = require('moment');
var config = require('./config');
var mail = require('./mail');
//var tweet = require('./tweet');

exports.handler = function(event, context) {
  console.log('starting function');
  moment.locale('de');
  var today = moment().startOf('day');

  ical.fromURL(config.ical_url, {}, function(err, data) {
    if (err) {
      console.error(err);
    } else {
      for (var k in data) {
        if (data.hasOwnProperty(k)) {
          var ev = data[k];
          if (ev.start) {
            var m = moment(ev.start).startOf('day');
            var diff = m.diff(today, 'days');
            console.log('processing event id ' + ev.uid + " / days: " + diff);
            mail(ev, diff);
          }
        }
      }

      setTimeout(function() {
        context.done();
      }, 3000);
    }
  });
};
