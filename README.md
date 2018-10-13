# Blitz Bot (ML version)

I was seriously addicted to [Bejeweled Blitz](https://apps.facebook.com/bejeweledblitz), so I thought that if I wrote a bot that could play it well enough such that getting to the top of the leaderboard was no longer a challenge, I would be able to quit it. It worked... but this is not that bot.

For [the previous bot](https://github.com/donaq/blitzbot), I basically hardcoded the colours of the gems, took screenshots, modeled the grid and then attempted to execute all possible current moves simultaneously.

Rinse and repeat the above.

That worked because the computer can click way faster than a human can, but it's just brute forcing the problem. I took Andrew Ng's ML course in Coursera some time in 2015 but have had neither time nor opportunity to use anything I learned. As with most knowledge, you use it or lose it, and I have definitely done the latter. This bot seems like a fun way to do some revision. For added challenge, I decided to use Clojure, a language which I was halfway through learning but had to put aside because work kept me busy... doing... *management* (the horror!)

## Installation

Install [leiningen](https://leiningen.org/). Clone the project.

```bash
$ git clone https://bitbucket.org/donaq/blitzcheat-ml/
```

I am not really a serious clojure dev, and this project isn't about learning clojure, so don't expect a high degree of polish.

## Dependencies

1. [(Arch) Linux](https://www.archlinux.org/) (this was developed on Arch Linux. it will probably work on any other linux. it may work on other operating systems because it was developed using a JVM language, but that would be a happy accident)

2. [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine)

3. [Clojure](https://clojure.org/)

4. [DL4J](https://deeplearning4j.org/)

5. [Chromium](https://www.chromium.org/Home) should also therefore work on Google Chrome, but no guarantees.

6. [pepper flash](https://www.archlinux.org/packages/?name=pepper-flash) That's the flashplayer plugin I currently use, but whatever you can play bejeweled blitz on should be fine.

## Usage

There are a few stages to using this bot.

1. obtain training data

2. annotate training data

3. train classifier

4. train score reader (unimplemented. if possible, I will actually skip this and use an OCR package that exists)

5. train the bot (RL unimplemented. no idea how to do this yet)


### Obtain training data

First, run the program in `gather` mode. In the root directory of the project, run the following [command](https://bitbucket.org/donaq/blitzcheat-ml/src/4b1f52be668308e44177d756a9d858bcf3140ead/project.clj#lines-18):

```bash
$ lein gather
```

[Install the extension](https://developer.chrome.com/extensions/getstarted) in chrome

How this currently works is there is a [chrome extension](https://bitbucket.org/donaq/blitzcheat-ml/src/master/extension/) that opens a websocket to the server started by the previous command. When you go to bejeweled blitz, the extension sends the coordinates and size of the HTML element in which the game resides. In response, the server takes a screenshot of the game element and stores it in `raw/`. So play a couple of games. :)

### Annotate training data

Once again, run the program in `gather` mode.

Now navigate your browser to `http://localhost:9999`, where the annotator interface should be served. The first row of controls are for navigating between images. `First` jumps to the first unannotated picture. The arrows are self-explanatory.

The second row of controls allow one to classify the image. The dropdown list contains existing classes, while the text input box allows one to enter a new class. Clicking `Classify` saves the current picture's classification. Navigating away from the current picture also saves the current class.

The point of classifying images is so that the bot will be able to recognise when there is no actual game happening and be able to automatically click through the screens we are not interested in to start a new game. Hence, for non-game classes, you should designate one click point such that clicking on that coordinate navigates toward a game state.

#### Designating a class click point

Click on the point in the image. Click `Set class click point` to save that point for this class. Note that you only need to do this once per class. Subsequent occurrences of the same class inherit the click point after classification.

#### Game class

The only class that is absolutely necessary is the `game` class. For this class of images, it is necessary to designate two areas of interest:

1. `score` area

2. `board` area

Upon detecting that a game is happening, the bot will train itself to play the game based on pixels in the game area and the current score.

#### Designating a class area

Click on the top left and bottom right of an area, then enter the name of the area and click `Add class area`.

#### What happens if I mess up?

If you misclassify an image, just reclassify it.

If you mess up a class click point, just setting a new one will overwrite it. This is because any non-game class is supposed to only have one, so this behaviour is hardcoded. Unfortun

If you mess up an area of interest for the `game` class, you will have to edit `public/annotations.json` manually.

Mine currently looks like this:

```json
{
    "pics":{
        "1532355620385.jpg":{"class":8},
        ...
    },

    "classes":[
        {"name":"offer1","click":[779,160]},
        {"name":"wait"},
        {"name":"spin1","click":[376,386]},
        {"name":"spin2","click":[443,389]},
        {"name":"spin3","click":[376,380]},
        {"name":"leaderboard1","click":[474,501]},
        {"name":"harvest1","click":[389,496]},
        {"name":"boost1","click":[484,516]},
        {"name":"game","areas":{"score":{"x":378,"y":112,"width":200,"height":34},"board":{"x":308,"y":150,"width":322,"height":321}}},
        {"name":"bringback1","click":[780,66]},
        {"name":"playagain1","click":[468,600]},
        {"name":"encore1","click":[473,393]}]
}
```

### Train classifier

TBD

### Train score reader

Hopefully we don't need to do this.

### Train bot

TBD (probably `lein play`)


## License

Copyright © 2016

This program is licensed under the "I do not give a fuck what you do with it" license. Use, fork, sell it or whatever. Just don't ask me for maintenance or upgrades. If for some crazy reason you wish to reward me, please consider making a donation to the EFF or the FSF.

## Warranty

None whatsoever. If this bot causes Armageddon, let this be a lesson to you that cheaters never win. :p
