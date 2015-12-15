var gulp = require('gulp');
var zip = require('gulp-zip');
var rimraf = require('gulp-rimraf');

var zipName = 'autoinvites.zip';

gulp.task('default', ['zip']);

gulp.task('zip', ['clean:zip'], function() {
  return gulp.src(
      ['./*.js', 'node_modules/**/*'],
      {base: '.'})
		.pipe(zip(zipName))
		.pipe(gulp.dest('.'));
});

gulp.task('clean:zip', function() {
  return gulp.src(zipName)
    .pipe(rimraf());
});
