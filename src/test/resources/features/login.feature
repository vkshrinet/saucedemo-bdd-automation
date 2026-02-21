@smoke @negative
Feature: SauceDemo Login Validation
  As the system
  I want to reject invalid login attempts
  So that unauthorized users cannot access the application

  Background:
    Given I am on the SauceDemo login page

  @invalid_login
  Scenario Outline: Invalid login shows authentication error
    Given I attempt login with username "<username>" and password "<password>"
    Then I should see the error message "<error_message>"
    And I should remain on the login page

    Examples:
      | username        | password     | error_message                                                             |
      | invalid_user    | wrong_pass   | Epic sadface: Username and password do not match any user in this service |
      | standard_user   | wrong_pass   | Epic sadface: Username and password do not match any user in this service |
      |                 | secret_sauce | Epic sadface: Username is required                                        |
      | standard_user   |              | Epic sadface: Password is required                                        |

  @locked_user
  Scenario: Locked-out user sees appropriate error
    Given I attempt login with username "locked_out_user" and password "secret_sauce"
    Then I should see the error message "Epic sadface: Sorry, this user has been locked out."
    And I should remain on the login page
