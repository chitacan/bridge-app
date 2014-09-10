var client   = require('./client')
  , inquirer = require('inquirer')
  , rest     = require('restler')
  , u        = require('url');

var option;

var defaultChoices = [
  new inquirer.Separator(),
  {
    name  : 'Connect without daemon',
    value : Number.MAX_VALUE
  }
];

function request(cb) {
  var url  = u.resolve(option.url, 'api/bridge/daemon');
  rest.get(url)
  .on('complete', function(data) {
    if (data)
      cb(data.concat(defaultChoices));
  })
  .on('fail', function(err, res) {
    cb(defaultChoices);
  })
  .on('error', function(err, res) {
    cb(defaultChoices);
  })
  .on('timeout', function() {
    cb(defaultChoices);
  });
}

function handleResponse(choices) {
  inquirer.prompt([
                  {
    type    : 'list',
    name    : 'daemonId',
    message : 'Select connected daemon ID',
    choices : choices
  }
  ], function(answer) {
    if (answer.daemonId !== Number.MAX_VALUE)
      option.daemonId = answer.daemonId;

    client(option);
  });
}

module.exports = function(opt) {
  option = opt;
  request(handleResponse);
}

