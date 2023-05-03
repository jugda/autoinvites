const fs = require('fs');
const moment = require('moment');
const handlebars = require('handlebars');
const { login } = require('masto');

const masto = login({
  url: process.env.MASTO_URL,
  accessToken: process.env.MASTO_TOKEN,
});

const days = [0, 2, 7, 28];

const toot = (ev, day) => {
  if (days.indexOf(day) > -1) {
    console.log('prepare toot for event uid ' + ev.uid);

    const source = fs.readFileSync(`templates/twitter_invitation.hbs`, 'utf-8');
    const template = handlebars.compile(source);
    ev.day = day === 0 ? '!!!HEUTE!!! ' : moment(ev.start).format('DD.MM.');

    masto.then((client) => {
      client.v1.statuses.create({
        status: template(ev),
        visibility: 'public',
      }).then((status) => {
        console.log(`Tooted: ${status.url}`);
      });
    });
  }
};

module.exports = toot;
