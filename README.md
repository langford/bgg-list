# bgg-list

## Purpose

This app creates a site about a single bgg collection. It shows play times, best with/good with numbers, and descriptions of the games that are there. This allows people to make choices about what to play when you go to someone's house with dozens or hundreds of games you do not know.

It does require that a environment variable be set for the BGGLIST_USERNAME. This app does not require the use of the password protected features of bgg, so no need to expose your password

To run, setup the environment variable to the desired username, and run the app using "lein run". It will download and process the collection over  the course of a few minutes. This is locally cached (in a file in the root directory of the project, cached.db.end) and you will need to delete this file if you change the username, or get new games (or get rid of games). This means re-starting the app only takes a couple seconds.

Copyright 2015 Michael Langford, Available for use under the MIT license

Pull requests: include a copyright assignment and good set of tests 

