var argv = require('minimist')(process.argv.slice(2));

config = {};

config.smtp = {
  host: argv.smtp_host || 'localhost',
  port: argv.smtp_port || 465,
  secure: argv.smtp_secure || true,
  auth: {
      user: argv.smtp_user || '',
      pass: argv.smtp_pass || ''
  }
};

config.mailOptions = {
  from: argv.from || 'JUG DA <info@jug-da.de>',
  to: argv.to || 'jug-da-info@groups.google.com'
};

config.ical_url = argv.ical_url || 'http://www.jug-da.de/events.ics';

if (argv.print_config) console.log(config);

module.exports = config;
