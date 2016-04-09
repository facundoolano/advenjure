# advenjure

A text adventure engine I wrote to learn Clojure (and because it's fun!).

## Usage
1. Clone the repository.
2. [Install leiningen](http://leiningen.org/#install).
3. Run the example game with `lein run` on the root directory of the project.


```
Welcome to the example game! type 'help' if you don't know what to do.

I woke up in a smelling little bedroom, without windows. By the bed I was
laying in was a small table and to the north a glass door. On the floor was a
sports magazine.
There's a small table here. The small table contains:
  A wallet
  A reading lamp
  A bottle. The bottle contains:
    An amount of water

                              ?

                         +--- N ---+
                         |         |
                         W         E
                         |         |
                         +--- S ---+



@Bedroom [0:0] > look at wallet
It's made of cheap imitation leather.

@Bedroom [1:0] > take wallet
Taken.

@Bedroom [2:0] > look in
in        inside

@Bedroom [2:0] > look in wallet
I don't have a dime.

@Bedroom [3:0] > talk to
amount of water   bed               bottle            door              glass door        lamp              magazine          reading lamp      small table       sports magazine   table
wallet

@Bedroom [3:0] > talk to wallet
ME —Hi, wallet.
WALLET —Tsup?
ME —Any cash I can use?
WALLET —Sorry.
```
