# h3m-parser

Heroes of Might and Magic III: The Shadow of Death maps parser.
Parse only visual elements. A lot of gameplay elements are omit or unknown.

Reference: https://github.com/vcmi/vcmi/blob/develop/lib/mapping/MapFormatH3M.cpp

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
