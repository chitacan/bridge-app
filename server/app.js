var express      = require('express')
  , http         = require('http')
  , path         = require('path')
  , logger       = require('morgan')
  , cookieParser = require('cookie-parser')
  , bodyParser   = require('body-parser')
  , debug        = require('debug')('server:app')


module.exports = create;

function create(dep) {
  var app    = express()
    , server = http.Server(app)
    , io     = require('socket.io').listen(server)
    , bridge = dep.bridge || require('./bridge');

  var routes = require('./routes/index')
  var api    = require('./routes/api')(bridge(io))

  // view engine setup
  app.set('views', path.join(__dirname, 'views'));
  app.set('view engine', 'jade');

  if (debug.enabled)
    app.use(logger('dev'));
  app.use(bodyParser.json());
  app.use(bodyParser.urlencoded({ extended: true }));
  app.use(cookieParser());
  app.use('/static', express.static(path.join(__dirname, 'public')));

  app.use('/'    , routes);
  app.use('/api' , api);

  /// catch 404 and forward to error handler
  app.use(function(req, res, next) {
    var err = new Error('Not Found');
    err.status = 404;
    next(err);
  });

  /// error handlers

  // development error handler
  // will print stacktrace
  if (app.get('env') === 'development') {
    require('longjohn');
    app.use(function(err, req, res, next) {
      res.status(err.status || 500);
      res.render('error', {
        message: err.message,
        error: err
      });
    });
  }

  // production error handler
  // no stacktraces leaked to user
  app.use(function(err, req, res, next) {
    res.status(err.status || 500);
    res.render('error', {
      message: err.message,
      error: {}
    });
  });

  return {
    app : app,
    server: server
  }
}
