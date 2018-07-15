/**
 * A page object for the example app home page.
 * 
 * Note that browser, element and by are globals defined by Potractor.
 * @module page-objects/home-page
 */
/* global element, by */ 
'use strict';


var _ = require('underscore');
var basePageObject = require('./page-base');


/**
 * Elements on the page.
 * 
 * Unlike a WebElement the ElementFinder object understands the Angular digest loop.
 * http://angular.github.io/protractor/#/api?view=ElementFinder
 */
var newTodoEl = element(by.model('newTodo'));
var firstTodo = element(by.repeater('todo in todos').row(0));

// Multiple elements are found with the 'all' method which
// like most (all?) methods on element objects returns a
// promise.
var toDoEls   = element.all(by.repeater('todo in todos'));


var homePage = {
  url: browser.baseUrl,


  /**
   * Create a new todo item.
   * @param  {string} todoText
   * @return {undefined}
   */
  createTodo: function createTodo(todoText) {
    newTodoEl.sendKeys(todoText);
    newTodoEl.sendKeys('\n');
  },


  /**
   * Get a promise for the text content of the first todo item.
   * @return {promise}
   */
  getFirstTodoText: function getFirstTodoText() {
    return firstTodo.element(by.tagName('label')).getText();
  },


  /**
   * Get a promise for the text content of all todo items.
   * @return {promise}
   */
  getAllTodoText: function getAllTodoText() {

    // Map over an array of promises for ElementFinders
    // Map    https://github.com/angular/protractor/issues/392#issuecomment-33237672
    // ElementFinder object    https://github.com/angular/protractor/blob/master/docs/locators.md#finding-multiple-elements
    return toDoEls.map(function (todo) {
      return todo.element(by.tagName('label')).getText();
    });
  },


  /**
   * Get a promise for the number of todo elements on the page.
   * @return {promise}
   */
  getNumberOfTodos: function getNumberOfTodos() {
    return toDoEls.count();
  }
};


// Mix in default page object properties and methods.
// Properties on the base page object will be copied
// over if they are undefined on the current page object.
_.defaults(homePage, basePageObject);


// Export the home page object.
module.exports = homePage;
