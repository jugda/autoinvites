const moment = require('moment');
const Twitter = require('twitter');

const client = new Twitter({
  consumer_key: process.env.TWITTER_CONSUMER_KEY,
  consumer_secret: process.env.TWITTER_CONSUMER_SECRET,
  access_token_key: process.env.TWITTER_ACCESS_TOKEN_KEY,
  access_token_secret: process.env.TWITTER_ACCESS_TOKEN_SECRET
});

const days = [0, 2, 7];

const buildStatus = (ev, day) => {
  let status = day === 0 ? '!!!HEUTE!!! ' : moment(ev.start).format('DD.MM.') + ": ";
  status = status + ev.title;
  if (ev.speaker) {
    status = status + ' (' + ev.speaker;
    if (ev.twitter) {
      status = status + ' / @' + ev.twitter;
    }
    status = status + ')';
  }
  status = status + ' ' + ev.url;
  return status;
};

const tweet = (ev, day) => {
  if (days.indexOf(day) > -1) {
    console.log('prepare tweet for event uid ' + ev.uid);
    const params = {
      status: buildStatus(ev, day)
    };

    client.post('statuses/update', params, (error, tweet, response) => {
      if (error) {
        throw error;
      }
      console.log('Tweeted: ' + JSON.stringify(tweet));
    });
  }
};

module.exports = tweet;
