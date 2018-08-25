const fs = require('fs');
const moment = require('moment');
const handlebars = require('handlebars');
const Twitter = require('twitter');

const client = new Twitter({
  consumer_key: process.env.TWITTER_CONSUMER_KEY,
  consumer_secret: process.env.TWITTER_CONSUMER_SECRET,
  access_token_key: process.env.TWITTER_ACCESS_TOKEN_KEY,
  access_token_secret: process.env.TWITTER_ACCESS_TOKEN_SECRET
});

const days = [0, 2, 7, 28];

const tweet = (ev, day) => {
  if (days.indexOf(day) > -1) {
    console.log('prepare tweet for event uid ' + ev.uid);

    const source = fs.readFileSync(`templates/twitter_invitation.hbs`, 'utf-8');
    const template = handlebars.compile(source);
    ev.day = day === 0 ? '!!!HEUTE!!! ' : moment(ev.start).format('DD.MM.');
    const params = {
      status: template(ev)
    };

    client.post('statuses/update', params, (error, tweet) => {
      if (error) {
        throw error;
      }
      console.log('Tweeted: ' + JSON.stringify(tweet));
    });
  }
};

module.exports = tweet;
