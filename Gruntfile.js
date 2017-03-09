module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    clean: {
      release: {
        src: ['dist/*']
      }
    },
    copy: {
      release: {
        files: [{
          expand: true,
          cwd: 'src',
          src: '**/*.*',
          dest: 'dist'
        }]
      }
    },
    watch: {
      options: {
        spawn: false
      },
      test: {
        files: ['src/**/*.json', 'src/**/*.js', 'spec/**/*.js', 'sync.js']
      }
    },
    nodemon: {
      dev: {
        script: 'src',
        options: {
          nodeArgs: ['--harmony']
        }
      }
    }
  });
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-nodemon');
  grunt.registerTask('release', ['clean', 'copy:release']);
};