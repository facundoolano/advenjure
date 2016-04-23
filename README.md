# advenjure

![Example game](example.gif)

A text adventure engine I wrote to learn Clojure (and because it's fun!).
Some of its distinctive features are:

  * Unix-like prompt with smart tab completion and command history (powered by [JLine](https://github.com/jline/jline2)).
  * Customizable and localizable texts and commands (powered by [clojure-gettext](https://github.com/facundoolano/clojure-gettext))
  * A domain specific language for dialog trees.

## Installation

Add the following to your project map as a dependency:

```clojure
[advenjure "0.1.0"]
```

## Basic Usage

### Creating items

Text adventures consist mainly of moving around rooms and interacting with items
through verb commands such as GO, LOOK, TAKE, etc.

Items are represented by Clojure records in advenjure. the `advenjure.item/make`
function takes a name, a description and a set key value pairs to customize behavior.
For example:

```clojure
```

That will define a magazine item that can be taken (put in the player's inventory)
and can be read (which differs from looking at it).

You can provide a vector of names so the player can refer to it by one of it's synonyms;
the first name in the vector will be considered it's canonical name:

```clojure
```

Like `:take` and `:read` there are keywords for the other actions
(`:look-at`, `:open`, `:close`, `:unlock`, etc.).

A special kind of items are those that can contain other items:

```clojure
```

The bag contains the magazine, but since it's `:closed` the player needs to open it
before being able to look inside it and take it's contents. Note that marking an
object as `:closed` also implies that OPEN and CLOSE actions are available on it
(i.e. `:open true, :close true`).

### Creating rooms

Once you create a bunch of items, you'll need to put them in a room (if not directly
into the player's inventory). Rooms are also records and also have an
`advenjure.rooms/make` function to build them:

```clojure
```

Note rooms can have only one name. `:initial-description` is an optional attribute
to define how the player will describe a room the first time he visits it,
usually with a more verbose description. If not `:initial-description` is not defined,
and whenever the LOOK AROUND command is entered, the regular description will be used.

To add items to a room use `advenjure.rooms/add-item`:

```clojure
```

### Building a room map

Once you have some rooms, you need to connect them to build a room map, which is
nothing but a plain clojure hash map. First map the room record to some id keyword,
then connect the rooms using the `advenjure.rooms/connect` function:

```clojure
```

An alternative function, `advenjure.rooms/one-way-connect`, allows connecting the
rooms just in one direction.

### Building and running a game

The next building block is the game map itself, which contains the room map,
the player's inventory and a pointer to the current room. `advenjure.game/make`
helps to build it:

```clojure
```

The room keyword defines what room the player will be in when the game starts.
If you want to start off the game with some items in the player's inventory,
just pass them in a set as the third argument:

```clojure
```

Lastly, the `advenjure.game/room` takes a game state map, a boolean function
to tell if a game has finished and an optional string to print before it starts:

```clojure
```

The game flows by taking the initial game state map, prompting the user for a command,
applying the command to produce a new game state and repeat the process until the
`finished` condition is met, which, in the example above means entering the
`:outside` room.

## Example game

You can see a working example in the [advenjure-example](https://github.com/facundoolano/clojure-gettext) repository.

## Advanced Usage

There are a number of advanced features available in the engine:

  * Overriding messages: use custom messages for a given action on a room or item.
  * Pre conditions: function hook to defines whether an action can be performed.
  * Post conditions: function hook to define how the game state is modified after an action.
  * Dialogs: interactive dialogs with 'character' items, in the style of LucasArts graphic adventures.
  * Text customization and internationalization.

But I'm not feeling like documenting those right now, specially since I doubt
anyone other than me will ever use this. But if you *do* want to use it, just open an issue
and I'll fill the rest of the README =)




