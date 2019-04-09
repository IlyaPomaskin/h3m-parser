# h3m-parser

Heroes of Might and Magic III maps parser.
Parse only visual elements. A lot of gameplay elements are omit or unknown.

## Usage

    $ lein run ./resources/invasion.h3m
```
{:description
 "This map is taken from the catalogue www.heroesportal.net\n",
 :teams-count 2,
 :heroes [],
 :difficulty 3,
 :map-version 28,
 :has-players? true,
 ... }
```
