# advenjure [![Build Status](https://secure.travis-ci.org/facundoolano/advenjure.png)](http://travis-ci.org/facundoolano/advenjure)

![Example game](example.gif)

Advenjure is a text adventure (or interactive fiction) game engine. I wrote it as an excuse to learn Clojure.
Some of its distinctive features are:

  * Target the terminal and the browser with the same codebase.
  * Unix-like prompt with smart tab completion and command history
  * Customizable verbs and plugins.
  * LucasArts inspired dialog trees.

## Example game

You can see the code for a working example in the [advenjure-example](https://github.com/facundoolano/advenjure-example) repository and play it online [here](https://facundoolano.github.io/advenjure).

For a fully fledged game see [House Taken Over](https://github.com/facundoolano/house-taken-over).

## Installation

Add the following to your project map as a dependency:

```clojure
[advenjure "0.9.0"]
```

## Basic Usage

### Creating items

Text adventures consist mainly of moving around rooms and interacting with items
through verb commands such as GO, LOOK, TAKE, etc.

Items are represented by maps in advenjure. the `advenjure.items/make`
function takes a name, a description and a set of key-value pairs to customize behavior.
For example:

```clojure
(require '[advenjure.items :as items])

(def magazine (items/make "magazine"
                          "The cover read 'Sports Almanac 1950-2000'"
                          :take true
                          :read "Oh là là? Oh là là!?"))
```

That will define a magazine item that can be taken (put in the player's inventory)
and can be read (which differs from looking at it).

You can provide a vector of names so the player can refer to it by one of its synonyms;
the first name in the vector will be considered its canonical name:

```clojure
(def magazine (items/make ["magazine" "sports magazine" "newspaper"]
                          "The cover read 'Sports Almanac 1950-2000'"
                          :take true
                          :read "Oh là là? Oh là là!?"))
```

Like `:take` and `:read` there are keywords for the other actions
(`:look-at`, `:open`, `:close`, `:unlock`, etc.).

A special kind of items are those that can contain other items:

```clojure
(def magazine (items/make "bag" :items #{magazine} :closed true))
```

The bag contains the magazine, but since it's `:closed` the player needs to open it
before being able to look inside it and take its contents. Note that marking an
object as `:closed` also implies that OPEN and CLOSE verbs can be applied to it
(i.e. it means `:open true, :close true`).

### Creating rooms

Once you've created a bunch of items, you'll need to put them in a room (if not directly
into the player's inventory). Rooms are also maps and also have an
`advenjure.rooms/make` function to build them:

```clojure
(require '[advenjure.rooms :as rooms])

(def bedroom (rooms/make "Bedroom"
                         "A smelling bedroom."
                         :initial-description "I woke up in a smelling little bedroom, without windows."))
```

Note that rooms can have only one name. `:initial-description` is an optional attribute
to define how the player will describe a room the first time he visits it,
usually with a more verbose description. If `:initial-description` is not defined,
and whenever the LOOK AROUND command is entered, the regular description will be used.

To add items to a room use `advenjure.rooms/add-item`:

```clojure
(def bedroom (-> (rooms/make "Bedroom"
                             "A smelling bedroom."
                             :initial-description "I woke up in a smelling little bedroom, without windows.")
                 (rooms/add-item (items/make "bed" "It was the bed I slept in."))
                 (rooms/add-item magazine "On the floor was a sports magazine.")))
```

The second parameter is an optional room-specific description of the item. It will be used
to mention the item while describing the room (as opposed of the default `a <item> is here.`).

### Building a room map

Once you have some rooms, you need to connect them to build a room map, which is
nothing but a plain clojure hash map. First map the room to some id keyword,
then connect the rooms using the `advenjure.rooms/connect` function:

```clojure
(def room-map (-> {:bedroom bedroom
                   :living living
                   :outside outside}
                  (rooms/connect :bedroom :north :living)
                  (rooms/connect :living :east :outside)))
```

An alternative function, `advenjure.rooms/one-way-connect`, allows connecting the
rooms just in one direction.

### Building and running a game

The next building block is the game map itself, which contains the room map,
the player's inventory and a pointer to the current room. `advenjure.game/make`
helps to build it:

```clojure
(require '[advenjure.game :as game])

(game/make room-map :bedroom)
```

The room keyword defines what room the player will be in when the game starts.
If you want to start off the game with some items in the player's inventory,
just pass them in a set as the third argument.

Lastly, the `advenjure.game/run` takes a game state map, a boolean function
to tell if the game has finished and an optional string to print before it starts.
Putting it all together in a `-main` function:

```clojure
(defn -main
  "Build and run the game."
  [& args]
  (let [game-state (game/make room-map :bedroom)
        finished? #(= (:current-room %) :outside)]
    (game/run game-state finished? :start-message "Welcome to the advenjure!")))
```

The game flows by taking the initial game state map, prompting the user for a command,
applying the command to produce a new game state and repeat the process until the
`finished?` condition is met, which, in the example above means entering the
`:outside` room.

## Advanced Usage

There is a number of advanced features available in the engine:

  * Overriding messages: use custom messages for a given action on a room or item.
  * Pre conditions: function hook to define whether an action can be performed.
  * Post conditions: function hook to customize how the game state is modified after an action is performed.
  * Dialogs: interactive dialogs with 'character' items, in the style of the LucasArts graphic adventures.
  * Text customization and internationalization.
  * Custom verbs/commands.
  * Plugin hooks to customize behavior without modifying the library.

I'm waiting for the APIs to stabilize (and get a lot of free time) before fully documenting all those features,
but I'd be happy to write something up if you need help with something specific, just file an issue!

## Run on the browser

The codebase is prepared to run both in the terminal with Clojure and the browser with ClojureScript.
An example configuration, using lein-cljsbuild would be:

```clojure
:cljsbuild
    {:builds
     {:main {:source-paths ["src"]
             :compiler {:output-to "main.js"
                        :main example.core
                        :optimizations :simple
                        :pretty-print false
                        :optimize-constants true
                        :static-fns true}}}
```

Then the command `lein cljsbuild once` will output a `main.js` file that can be included in any web page to run the game.
The HTML should have a `#terminal` div and include the [jQuery Terminal CSS](https://github.com/facundoolano/advenjure-example/blob/master/resources/jquery.terminal-0.11.7.css) to properly render the terminal.

The current limitations of the ClojureScript version of the library are:
* No internationalization support (since clojure-gettext does not support ClojureScript).
* Can use up to `:simple` optimizations (not `:advanced`), since [ClojureScript self-hosting](https://github.com/clojure/clojurescript/wiki/Optional-Self-hosting) is required for some of the advanced features.

See the [advenjure-example](https://github.com/facundoolano/advenjure-example) for a game that targets both the terminal and the browser.

## Roadmap

[Check the milestones](https://github.com/facundoolano/advenjure/milestones).
