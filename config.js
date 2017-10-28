const config = {
  days: {
    mail: [2,7],
    twitter: [2,7]
  }
};

config.smtp = {
  host: process.env.SMTP_HOST,
  port: 465,
  secure: true,
  debug: false,
  auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASSWORD
  }
};

config.mailOptions = {
  from: 'JUG DA <info@jug-da.de>',
  to: 'jug-da@googlegroups.com'
};

config.events_url = process.env.EVENTS_URL;

module.exports = config;
