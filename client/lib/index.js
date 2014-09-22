var client   = require('./client')
  , inquirer = require('inquirer')
  , rest     = require('restler')
  , u        = require('url')
  , rl       = require('readline')
  , debug    = require('debug')
  , debug_e  = debug('client:error');

debug_e.log = console.error.bind(console);

var option;
var p;

var defaultChoices = [
  new inquirer.Separator(),
  {
    name  : 'Connect without daemon',
    value : 0
  },
  {
    name  : 'Refresh',
    value : -1
  },
  {
    name  : 'Get me out of here!',
    value : -2
  }
];

function request(cb) {
  // validate url
  var url  = u.resolve(option.url, 'api/bridge/daemon');
  rest.get(url)
  .on('2XX', function(result, res) {
    if (!(result instanceof Error)) {
      cb(result.concat(defaultChoices));
    }
  })
  .on('fail'   , exit)
  .on('error'  , exit)
  .on('timeout', exit);
}

function onResponse(choices) {
  p = inquirer.prompt([
                  {
    type    : 'list',
    name    : 'daemonId',
    message : 'Select connected daemon ID',
    choices : choices
  }
  ], onSelect);
}

function onSelect(answer) {
  switch(answer.daemonId) {
    case -1:
      clearUpperLine();
      request(onResponse);
      return;
    case -2:
      exit();
      return;
    case 0:
    default:
      if (typeof answer.daemonId == 'string')
        option.daemonId = answer.daemonId;
      client(option);
      break;
  }
}

function clearUpperLine() {
  rl.moveCursor(process.stdout, 0, -1);
  rl.clearLine (process.stdout, 0);
}

function exit() {
  if (arguments.length > 0)
    debug_e('Sever not responding...');
  process.exit(0);
}

module.exports = function(opt) {
  option = opt;
  request(onResponse);
}

