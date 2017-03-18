const Twitter = require('twitter');
const config = require('./config');

const client = new Twitter(config.twitter);

const buildStatus = (ev) => {
  return moment(ev.start).format('DD.MM.') + ': ' + ev.summary.substr(0, (140-8-25)) + " " + ev.url;
};

const tweet = (ev, day) => {
  if (config.days.twitter.indexOf(day) > -1) {
    const params = {
      status: buildStatus(ev)
    };

    client.post('statuses/update', params, (error, tweet, response) => {
      if (error) {
        throw error;
      }
      console.log(tweet);
    });
  }
};

module.exports = tweet;
