var moment = require('moment');
var nodemailer = require('nodemailer');
var strip = require('strip');
var config = require('./config');

var buildMailText = function(ev) {
  var date = moment(ev.start).format('dddd, DD. MMMM YYYY');
  var time = moment(ev.start).format('HH:mm');
  return '<p>Am kommenden <b>' + date + '</b> findet ab <b>' + time + ' Uhr</b> unser nächster Talk statt:</p>'
      + '<p><h3>&nbsp;&nbsp;&nbsp;&nbsp;' + ev.summary + '</h3></p>'
      + '<p>Ort: ' + ev.location + '</p>'
      + '<p>Alle Infos zum Talk, Anfahrtsbeschreibung und Anmeldung wie immer auf unserer Website:<br/>'
      + '<a href="' + ev.url + '">' + ev.url + '</a></p>'
      + '<p>Wir freuen uns auf Euer zahlreiches Kommen!<br/><br/>Viele Grüße,<br/>Euer JUG DA Orga-Team</p>';
};

var sendMail = function(ev, day) {
  if (config.days.mail.indexOf(day) > -1 || config.now) {
    var transporter = nodemailer.createTransport(config.smtp);
    var mailOptions = config.mailOptions;

    mailOptions.html = buildMailText(ev);
    mailOptions.text = strip(mailOptions.html);
    mailOptions.subject = moment(ev.start).format('DD.MM.YYYY') + ': ' + ev.summary;

    transporter.sendMail(mailOptions, function (error, info) {
      if (error) {
        return console.log(error);
      }
      console.log('Message sent: ' + info.response);
    });
  }
};

module.exports = sendMail;
