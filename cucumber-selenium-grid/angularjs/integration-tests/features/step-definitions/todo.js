/**
 * Step definitions.
 */
'use strict';

// Require and configure the assertion libraries.
var chai = require('chai');
var chaiAsPromised = require('chai-as-promised'); // https://github.com/domenic/chai-as-promised/
chai.use(chaiAsPromised);

// Convenience.
var expect = chai.expect;
var _ = require('underscore');
const { setWorldConstructor } = require('cucumber')

var CustomWorld = function({attach, parameters}) {
	  this.attach = attach
	  this.parameters = parameters
	};
	
	CustomWorld.prototype.clearLocalStorage = function () {
        browser.executeScript('localStorage.clear();');
      };

     CustomWorld.prototype.flattenTable = function (table) {
        // Array of arrays e.g.
        // [ [ 'First todo' ], [ 'Second todo' ] ]
        table = table.raw();

        // Flatten array
        // [ 'First todo', 'Second todo' ]
        return _.flatten(table);
      };	

setWorldConstructor(CustomWorld);

const { Given, When, Then, After, Before, Status } = require('cucumber');

//Require page objects.
var homePage = require('../../page-objects/home-page');

Before('@home', function () {
    var world = this;

    // Load the homepage.
    homePage.get();
    //browser.get('');

    // Clear localStorage.
    world.clearLocalStorage();

    // Reload the page with empty localStorage.
    homePage.get();
    //browser.get('');

  });

After(function (testCase) {
	  var world = this;
	  if (testCase.result.status === Status.FAILED) {
	    return browser.takeScreenshot().then(function(screenShot) {
	      // screenShot is a base-64 encoded PNG
	      world.attach(screenShot, 'image/png');
	    });
	  }
	});
	
  Given(/^I am on the app home page\.?$/, function (done) {
    var expectedTitle = 'AngularJS • TodoMVC';

// The page loading is async so we need an async expectation
// and an async 'done'.
    
    expect(homePage.getPageTitle())
      .to.eventually.equal(expectedTitle)
      .notify(done);
  });


  When(/^I add a todo called "([^"]*)"\.?$/, addTodoText);


  When(/^I add the todos\.?$/, addTodoText);


  // Use a data table. API here
  // https://github.com/cucumber/cucumber-js/blob/master/lib/cucumber/ast/data_table.js
  When(/^I add multiple todos:$/, function (table, done) {
    var world = this;

    table = world.flattenTable(table);

    table.forEach(function (todoText) {
      homePage.createTodo(todoText);
    });

    world.expectedNumberOfTodos = table.length;

    done(); 
  });


  // Deliberately pending step.
 // When(/^Something is done\.$/, function (done) {
// Write code here that turns the phrase above into concrete actions
//    done.pending();
 // });


  Then(/^I should see it added to the todo list\.?$/, function (done) {
    checkFirstTodoText(this.expectedTodoText, done);
  });


  Then(/^I should see a todo called "([^"]*)"\.?$/, function (expectedTodoText, done) {
    checkFirstTodoText(expectedTodoText, done);
  });



  Then(/^I should see them added to the todo list\.$/, function (done) {
var world = this;

// Split the expected text string on new line to
// allow comparison to the array of todos taken
// from the UI.
var expectedTodoTextArray = world.expectedTodoText.split(/\r?\n/);

// Use Chai deep equal to compare arrays.
    homePage.getAllTodoText()
      .then(function(todoTextArray) {
        expect(todoTextArray).to.deep.equal(expectedTodoTextArray);
        done();
      });
  });


 Then(/^there should be that number of todos in the list\.?$/, function (done) {
    var world = this;
    checkNumberOfTodos(world.expectedNumberOfTodos, done);
  });


  Then(/^it should (.*) in the list\.$/, function (appearOrNot, done) {
var expectedNumberOfTodos = (appearOrNot === 'appear') ? 1 : 0;
    checkNumberOfTodos(expectedNumberOfTodos, done);
  });


 // Then(/^there should be a measurable result\.$/, function (done) {
// Write code here that turns the phrase above into concrete actions
//    done.pending();
  //});
	



/* Helper functions */


function checkFirstTodoText(expectedTodoText, done) {
  // The underlying getText method and and all Protractor DOM
  // action methods (i.e. actions on 'elelement' objects) are
  // asynchronous, because they wait for the Angular digest
  // loop to settle down, and return promises.
  // Here the promise is handled explicitly in the check.
  homePage.getFirstTodoText()
    .then(function(todoText) {
      try {
        expect(todoText).to.equal(expectedTodoText);
        done();
      }
      catch(err){
        done(err);
      }
      
    });
}

function checkNumberOfTodos(expectedNumberOfTodos, done) {
  // If using Chai-as-promised then promises for values can also
  // be checked against using the 'eventually' property. Note
  // the call to 'notify' with the Cucumber callback as an
  // argument in order to signal step completion.
  expect(homePage.getNumberOfTodos())
    .to.eventually
    .equal(expectedNumberOfTodos)
    .notify(done);
}

function addTodoText(todoText, done) {
  // Share state on the world object. Could also have done this with a closure.
  /*jshint validthis:true */
  var world = this;
  world.expectedTodoText = todoText;

  homePage.createTodo(todoText);
  done();
}



