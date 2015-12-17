var Twitter = require('twitter');
var config = require('./config');

var client = new Twitter(config.twitter);

var buildStatus = function(ev) {
  return moment(ev.start).format('DD.MM.') + ': ' + ev.summary.substr(0, (140-8-25)) + " " + ev.url;
};

var tweet = function(ev) {
  var params = {
    status: buildStatus(ev);
  };

  client.post('statuses/update', params, function(error, tweet, response) {
    if (!error) {
      console.log(tweet);
    } else {
      console.log(error);
    }
  });
};

module.exports = tweet;
