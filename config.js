config = {};

config.smtp = {
  host: 'localhost',
  port: 465,
  secure: true,
  auth: {
      user: '',
      pass: ''
  }
};

config.mailOptions = {
  from: 'JUG DA <info@jug-da.de>',
  to: 'jug-da-info@groups.google.com'
};

config.ical_url = 'http://www.jug-da.de/events.ics';

module.exports = config;
