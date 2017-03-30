const moment = require('moment');
const nodemailer = require('nodemailer');
const strip = require('strip');
const config = require('./config');

const buildMailText = (ev) => {
  const date = moment(ev.start).format('dddd, DD. MMMM YYYY');
  const time = moment(ev.start).format('HH:mm');
  return '<p>Am kommenden <b>' + date + '</b> findet ab <b>' + time + ' Uhr</b> unser nächster Talk statt:</p>'
      + '<p><h3>&nbsp;&nbsp;&nbsp;&nbsp;' + ev.summary + '</h3></p>'
      + '<p>Ort: ' + ev.location + '</p>'
      + '<p>Alle Infos zum Talk, Anfahrtsbeschreibung und Anmeldung wie immer auf unserer Website:<br/>'
      + '<a href="' + ev.url + '">' + ev.url + '</a></p>'
      + '<p>Wir freuen uns auf Euer zahlreiches Kommen!<br/><br/>Viele Grüße,<br/>Euer JUG DA Orga-Team</p>'
      + '<p><small><i>Eine Veranstaltung des iJUG e.V. organisiert durch die JUG Darmstadt.</i></small></p>';
};

const sendMail = (ev, day) => {
  if (config.days.mail.indexOf(day) > -1) {
    console.log('prepare mail to send for event uid ' + ev.uid);
    const transporter = nodemailer.createTransport(config.smtp);
    const mailOptions = config.mailOptions;

    mailOptions.html = buildMailText(ev);
    mailOptions.text = strip(mailOptions.html);
    mailOptions.subject = moment(ev.start).format('DD.MM.YYYY') + ': ' + ev.summary;

    transporter.sendMail(mailOptions, (error, info) => {
      if (error) {
        throw error;
      }
      console.log('Message sent: ' + info.response);
    });
  }
};

module.exports = sendMail;
