@smoke @e2e
Feature: SauceDemo End-to-End Purchase Flow
  As a registered user
  I want to purchase a product
  So that I receive an order confirmation

  Background:
    Given I am on the SauceDemo login page

  @happy_path
  Scenario: Successful purchase of a single product
    Given I login as a "standard_user"
    When I add "Sauce Labs Backpack" to the cart
    Then the cart badge should display "1"
    When I proceed to checkout
    And I enter checkout details for "valid_customer"
    And I complete the order
    Then I should see the order confirmation page
    And the confirmation message should be "Thank you for your order!"
