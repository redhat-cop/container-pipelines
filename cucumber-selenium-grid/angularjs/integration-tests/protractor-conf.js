exports.config = {
  framework: 'custom',
  frameworkPath: require.resolve('protractor-cucumber-framework'),
  rootElement: 'body', // location of ng-app directive
//  capabilities: {
//	    'browserName': 'chrome'
//	  },
 multiCapabilities: [{
	  'browserName': 'firefox'
	}, {
	  'browserName': 'chrome'
	}],
  specs: ['features/*.feature'],
  coloredLogs: false,
  verbose: true,
  // See cucumberOpts in https://github.com/angular/protractor/blob/master/docs/referenceConf.js
  // A list of tags to run can be specified here e.g.
  // tags: '@smoke'
  // tags: ['@smoke', '@otherTag, @thirdTag']
  // The Cucumber require path can be set with the 'require' property.
  cucumberOpts: {
    format: 'json:./cucumber/results.json',
    require: 'features/*.js',
    'no-colors': true,
    verbose: true
  },
  plugins: [{
      package: 'protractor-multiple-cucumber-html-reporter-plugin',
      options:{
          // read the options part
    	  automaticallyGenerateReport: true
      }
  }]
};

