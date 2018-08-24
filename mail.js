const moment = require('moment');
const AWS = require('aws-sdk');
const strip = require('strip');

AWS.config.update({region: 'eu-west-1'});
const ses = new AWS.SES({apiVersion: '2010-12-01'});

const days = [2, 7, 28];

const buildMailText = (ev) => {
  const date = moment(ev.start).format('dddd, DD. MMMM YYYY');
  const time = moment(ev.start).format('HH:mm');
  return '<p>Am kommenden <b>' + date + '</b> findet ab <b>' + time + ' Uhr</b> unser nächster Talk statt:</p>'
      + '<p><h3>&nbsp;&nbsp;&nbsp;&nbsp;' + ev.summary + '</h3></p>'
      + '<p>Ort: ' + ev.location + '</p>'
      + '<p>Alle Infos zum Talk, Anfahrtsbeschreibung und Anmeldung wie immer auf unserer Website:<br/>'
      + '<a href="' + ev.url + '">' + ev.url + '</a></p>'
      + '<p>Wir freuen uns auf Euer zahlreiches Kommen!<br/><br/>Viele Grüße,<br/>Euer JUG DA Orga-Team</p>'
      + '<p><small><i>Eine Veranstaltung des iJUG e.V., organisiert durch die JUG Darmstadt.</i></small></p>';
};

const buildAnnouncingMailText = (ev) => {
  const date = moment(ev.start).format('dddd, DD. MMMM YYYY [ab] HH:mm');
  return 'Liebe JUG DA Freunde,'
    + '<p>wir möchten Euch heute folgende Veranstaltung ankündigen:</p>'
    + '<p><h3>&nbsp;&nbsp;&nbsp;&nbsp;' + ev.summary + '</h3></p>'
    + '<p>Wo: ' + date + '<br/>'
    + 'Wann: ' + ev.location + '</p>'
    + '<p><em style="color: red">Die Anmeldung ist jetzt ebenfalls geöffnet!</em></p>'
    + '<p>Alle weiteren Infos zum Talk, wie Anfahrtsbeschreibung und Anmeldung, gibt\'s wie immer auf unserer Website:<br/>'
    + '<a href="' + ev.url + '">' + ev.url + '</a></p>'
    + '<p>Wir freuen uns auf Euer zahlreiches Kommen!<br/><br/>Viele Grüße,<br/>Euer JUG DA Orga-Team</p>'
    + '<p><small><i>Eine Veranstaltung des iJUG e.V., organisiert durch die JUG Darmstadt.</i></small></p>';
};

const getUtf8Data = data => {
  return {
    Charset: 'UTF-8',
    Data: data
  }
};

const sendMail = (ev, day) => {
  if (days.indexOf(day) > -1) {
    console.log('prepare mail to send for event uid ' + ev.uid);

    const to = process.env.MAIL_TO;
    const from = process.env.MAIL_FROM;
    const htmlBody = day === 28 ? buildAnnouncingMailText(ev) : buildMailText(ev);
    const textBody = strip(htmlBody);
    const subject = (day === 28 ? 'Ankündigung für ' : '') + moment(ev.start).format('DD.MM.YYYY') + ': ' + ev.summary;

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
};

module.exports = sendMail;
