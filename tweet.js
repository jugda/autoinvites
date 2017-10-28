const Twitter = require('twitter');

const client = new Twitter({
  consumer_key: process.env.TWITTER_CONSUMER_KEY,
  consumer_secret: process.env.TWITTER_CONSUMER_SECRET,
  access_token_key: process.env.TWITTER_ACCESS_TOKEN_KEY,
  access_token_secret: process.env.TWITTER_ACCESS_TOKEN_SECRET
});

const days = [2, 7];

const buildStatus = (ev) => {
  return moment(ev.start).format('DD.MM.') + ': ' + ev.summary.substr(0, (140-8-25)) + " " + ev.url;
};

const tweet = (ev, day) => {
  if (days.indexOf(day) > -1) {
    console.log('prepare tweet for event uid ' + ev.uid);
    const params = {
      status: buildStatus(ev)
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
