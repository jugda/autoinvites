var ical = require('ical');
var moment = require('moment');
var config = require('./config');
var mail = require('./mail');
var twitter = require('./tweet');

exports.handler = function(event, context) {
  var transporter = nodemailer.createTransport(config.smtp);
  var mailOptions = config.mailOptions;

  moment.locale('de');
  var today = moment().startOf('day');

  ical.fromURL(config.ical_url, {}, function(err, data) {
    if (err) {
      console.error(err);
    } else {
      for (var k in data){
        if (data.hasOwnProperty(k)) {
          var ev = data[k];
          if (ev.start) {
            var m = moment(ev.start).startOf('day');
            var diff = m.diff(today, 'days');
            
            switch (diff) {
              case 0:

                break;
              case 1:

                break;
              case 2:
                mail(ev);
                break;
              case 3:

                break;
              case 4:

                break;
              case 5:

                break;
              case 6:

                break;
              case 7:
                mail(ev);
                break;
              default:
                if (config.now) {
                  mail(ev);
                }
                break;
            }
          }
        }
      }
    }
  });
}
