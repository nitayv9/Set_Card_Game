# Set Card Game

[![j.png](https://i.postimg.cc/2SGvFyTC/j.png)](https://postimg.cc/Yv4htpJP)

##  Description
The Set card game implementation is a project developed as part of a university assignment to practice Object-Oriented Programming (OOP) and concurrency principles. The objective of this project is to simulate the Set card game, where players interact with a table and a dealer. The game involves identifying sets of three cards that satisfy specific criteria.

You can find the games rules here - https://en.wikipedia.org/wiki/Set_(card_game).

## Features

Simulates the Set card game.
Supports multiple players participating simultaneously.
Implements gameplay mechanics, including card selection, matching, and scoring.
Utilizes Object-Oriented Programming concepts to model the game entities such as Table, Dealer, and Player.
Implements concurrency principles to handle multiple players interacting with the game simultaneously.

## Technologies Used
The project leverages the following technologies and concepts:

* Programming language: Java
* Object-Oriented Programming (OOP) principles
* Concurrency and multi-threading

## Installation and Setup
Just  the project repository to your local machine and runthe Main.java file. (src/main/java/bguspl/set/Main.java)

During gameplay, human players will input their moves using the keyboard. The left-side player will use the following keys for placing and removing tokens: Q, W, E, R, A, S, D, F, Z, X, C, V. The right-side player will use the following keys: U, I, O, P, J, K, L, ';', M, ',', '.'.
Each key puts and removes a token.


## Configuration
The game's configuration can be customized using the config.properties file, located in the resources folder. This file allows you to specify various settings for the game, such as the number of bots, the minimum delay between player actions, and any other relevant configurations. Open the config.properties file in a text editor and modify the values according to your requirements.

## Testing Concurrency

To thoroughly test the concurrency principles implemented in the Set card game, a specific configuration can be used. By setting all players as bots and introducing a minimum delay between their actions, the concurrency mechanisms can be evaluated in a demanding scenario. In this configuration, the bots play at a rapid pace, frequently choosing the same cards, and the table refreshes very quickly. This setup aims to stress-test the concurrency handling and ensure that concurrent access to the game state is managed correctly. It provides an ideal environment to observe and address any potential concurrency issues that may arise during gameplay.

[![Untitled-Project-V1.gif](https://i.postimg.cc/SKG9dsry/Untitled-Project-V1.gif)](https://postimg.cc/QBHVxjwP)
