'use strict';
/* global browser */ 

module.exports = {
  url: '',
  get: get,
  getPageTitle: getPageTitle,
  clearLocalStorage: clearLocalStorage,
  goToHomePage: goToHomePage
};


/**
 * Navigate to the home page.
 * @return {undefined}
 */
function get() {
  /*jshint validthis:true */
  var url = this.url;
  if (!url) {
    throw new TypeError('A page object must have a URL defined in order to call \'get\'');
  }
  browser.get(url);
}



/**
 * Clear Local Storage
 * @return {string} page title.
 */
function clearLocalStorage(done) {
  setTimeout(() => {
    browser.executeScript('window.localStorage.clear();');
    //browser.get('javascript://localStorage.clear();')
    done();
  }, 1000)
  

  
}



/**
 * Navigate to home.
 * @return {string} page title.
 */
function goToHomePage(done) {
	browser.get(browser.baseUrl);
  setTimeout(() => {
    
   //browser.get('javascript://localStorage.clear();')
   done();
  }, 1000)
}

/**
 * Get the page title.
 * @return {string} page title.
 */
function getPageTitle() {
  return browser.getTitle();
  
}