const http = require('http');
const moment = require('moment');
const config = require('./config');
const mail = require('./mail');

exports.handler = (event, context, callback) => {
  console.log('starting function with event:', JSON.stringify(event, null, 2));
  moment.locale('de');
  const today = moment().startOf('day');

  http.get(config.events_url, (res) => {
    let body = '';
    res.on('data', (chunk) => {body += chunk});
    res.on('end', () => {
      const data = JSON.parse(body);
      const event = data[0];
      if (event.start) {
        const m = moment(event.start).startOf('day');
        const diff = m.diff(today, 'days');
        console.log('processing event id ' + event.uid + " / days: " + diff);
        mail(event, diff);
      }
    });
  }).on('error', (e) => {
    console.error("ERROR", e);
    callback(e)
  });

};
