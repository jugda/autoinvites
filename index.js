const https = require('https');
const moment = require('moment');
const mail = require('./mail');
const tweet = require('./tweet');

exports.handler = (event, context, callback) => {
  console.log('Received event:', JSON.stringify(event, null, 2));
  moment.locale('de');
  const today = moment().startOf('day');

  https.get(process.env.EVENTS_URL, (res) => {
    let body = '';
    res.on('data', (chunk) => {body += chunk});
    res.on('end', () => {
      const data = JSON.parse(body);
      data.forEach(event => {
        if (event.start) {
          const m = moment(event.start).startOf('day');
          const diff = m.diff(today, 'days');
          console.log('processing event id ' + event.uid + " / days: " + diff);
          mail(event, diff);
          tweet(event, diff);
        }
      });
    });
  }).on('error', (e) => {
    console.error("ERROR", e);
    callback(e)
  });

};
