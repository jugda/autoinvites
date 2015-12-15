var gulp = require('gulp');
var del = require('del');
var zip = require('gulp-zip');

var zipName = 'autoinvites.zip';

gulp.task('default', ['zip']);

gulp.task('zip', function() {
  return gulp.src(['./**'])
		.pipe(zip(zipName))
		.pipe(gulp.dest('.'));
});

gulp.task('delete', function() {
  return del(zipName);
});
