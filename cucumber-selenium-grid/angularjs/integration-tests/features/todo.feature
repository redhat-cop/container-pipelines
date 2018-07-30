# Example feature, the description is using feature injection syntax.
Feature: Todo list
  As a user of the todo list
  I want to be able to add and read todos
  So that I can remember what I am supposed to do later


  @home @smoke
  Scenario: Adding a todo
    Given I am on the app home page.
    When I add a todo called "hello world".
    Then I should see it added to the todo list.


  # NB if you are comparing blocks of text beware of the file encoding
  # UTF-8 and Unix line-endings are recommended.
  @home @block-string
  Scenario: Adding multiple todos in a block string
    Given I am on the app home page.
    When I add the todos
      """
      First todo
      Second todo
      Third todo
      """
    Then I should see them added to the todo list.


  # Data table.
  # Data tables: https://www.relishapp.com/cucumber/cucumber-js/docs/cucumber-tck/data-tables
  @home @regression @data-table
  Scenario: Adding multiple todos in a data table
    Given I am on the app home page.
    When I add multiple todos:
      | First todo  |
      | Second todo |
    Then there should be that number of todos in the list.



  # Scenario outline.
  # Scenario outlines: supported https://github.com/cucumber/cucumber-js/commit/c2a9916810a224d77c6b7e94260c39bb867aee5b
  @home @outline
  Scenario Outline: Edge case todos
    Given I am on the app home page.
    When I add a todo called "<todo text>".
    Then it should <appear or not> in the list.

    Examples:
      | todo text | appear or not |
      | howdy     | appear        |
      |           | not appear    |


  # Senario with a pending step.
  @home @somePendingTest
  Scenario: Anther great scenario
    Given I am on the app home page.
    When Something is done.
    Then there should be a measurable result.

  # Senario with a failing step.
  @home @someFailingTest
  Scenario: Yet anther great scenario
    Given I am on the app home page.
    When I add a todo called "expected to fail".
    Then I should see a todo called "something totally different".