# gameday

[![Clojars Project](https://img.shields.io/clojars/v/com.bradlucas/gameday.svg)](https://clojars.org/com.bradlucas/gameday)

gameday, a Clojure cmdline application

## Back story

A few years back, in 2011, I worked on an application for a startup. We were building a team-based sports site based on sports questions about games. The questions were setup to be mini-predictions of upcoming games and teams competed to see who was better at selecting what would happen. To start we focused on baseball and we built a prototype using the MLB gameday data as a source.

To get an idea of the data source look at the following links:

- http://gd2.mlb.com/components/game/mlb/
- http://gd2.mlb.com/components/game/mlb/year_2016/month_09/day_24/scoreboard.xml

This repo contains some of the code I deleved for that project. It was nice to see that in 2016 the MLB data source still has the same structure and the old code here 'worked'. That said I've only verified the features to get a schedule for a day and a specific game for a day. There is more that can be teased out. The events.clj file is included but untouched. Also, the data source is huge and I'm sure there are many other ideas that can be developed.

## Usage

- Get the schedule for 9/24/2016
- java -jar target/gameday-1.0.0-standalone.jar -s 9 24 2016


- Get the third game in the shedule for 9/24/2016
- java -jar target/gameday-1.0.0-standalone.jar -g 9 24 2016 3

## Versions

### 1.0.0

- Initial release. Get schedule and get game only

## License

Copyright © 2016 Brad Lucas

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
