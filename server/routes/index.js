var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Bridge Status' });
});

router.route('/partials/:name')
  .get(function(req, res) {
    res.render('partials/'.concat(req.params.name));
  });

module.exports = router;
