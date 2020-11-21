const fs = require('fs');
const AWS = require('aws-sdk');
const moment = require('moment');
const handlebars = require('handlebars');
const strip = require('strip');

const ses = new AWS.SES({apiVersion: '2010-12-01'});

const days = [2, 7, 28];

const getUtf8Data = data => {
  return {
    Charset: 'UTF-8',
    Data: data
  }
};

const sendMail = (ev, day) => {
  if (!ev.externalEvent && days.indexOf(day) > -1) {
    if (day !== 28 || !ev.hideRegistration) {
      console.log('prepare mail to send for event uid ' + ev.uid);

      const to = process.env.MAIL_TO;
      const from = process.env.MAIL_FROM;
      const subject = (day === 28 ? 'Ankündigung für ' : '') + moment(ev.start).format('DD.MM.YYYY') + ': ' + ev.summary;

      ev.date = moment(ev.start).format('dddd, DD. MMMM YYYY');
      ev.time = moment(ev.start).format('HH:mm');

      const templateName = day === 28 ? 'announcement' : 'invitation';
      const source = fs.readFileSync(`templates/mail_${templateName}.hbs`, 'utf-8');
      const template = handlebars.compile(source);
      const htmlBody = template(ev);
      const textBody = strip(htmlBody);

      const email = {
        Destination: { ToAddresses: [to] },
        Message: {
          Body: {
            Html: getUtf8Data(htmlBody),
            Text: getUtf8Data(textBody)
          },
          Subject: getUtf8Data(subject)
        },
        Source: from
      };

      ses.sendEmail(email).promise()
        .then(data => {
          console.log('Message sent', data);
        })
        .catch(error => {
          throw error;
        });
    }
  }
};

module.exports = sendMail;
