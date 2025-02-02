# Region command

## Description

Manage regions for an arena. More information is available [on dedicated page](../regions.md).

## Usage

| Command                                                             | Definition                                                                                              |
|---------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| /pa \<arena\> region                                                | start setting up a region for an arena (type again to leave region editor)                              |
| /pa \<arena\> region \<regionName\> (shape)                         | save a region for the arena. Cuboid is the default shape                                                |
| /pa \<arena\> region \<regionName\> remove                          | remove an arena region                                                                                  |
| /pa \<arena\> region \<regionName\> border                          | show limits of a region                                                                                 |
| /pa \<arena\> region \<regionName\> shift \<amount\> (direction)    | move a region of the amount of blocks toward the requested direction (or looking direction otherwise)   |
| /pa \<arena\> region \<regionName\> expand \<amount\> (direction)   | expand a region of the amount of blocks toward the requested direction (or looking direction otherwise) |
| /pa \<arena\> region \<regionName\> contract \<amount\> (direction) | reduce a region by the amount of blocks to the requested direction (or looking direction otherwise)     |

Example:
- `/pa free !r ball` - save the cuboid region "ball" to the arena "free"
- `/pa ctf !r X remove` - remove the region "X" from the arena "ctf"
- `/pa 4vs4 !r battlefield expand 10 UP` - extend the region "battlefield" (of the arena "4vs4") 10 blocks upward

## Details
If we have to sum up, setting up an arena needs 3 things:

- `/pa <arenaname> region`
- select two points that define the region
- `/pa <arenaname> region <regionshape>`

Check out [our tutorial section](../regions.md) for more information.

<br> 

Valid regionshapes include:
 
- CUBOID (default)
- SPHERIC
- CYLINDRIC