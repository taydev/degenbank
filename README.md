This is mostly just on here because I needed an easy way to move files from my PC to my RPi (+ version control). But, here we are.

# degenBank - A bank by the brainless, for the brainless.

### What is degenBank?
degenBank is a small Discord bot I decided to work on to fill free time as an event in the DrAnime's Degen Wasteland Discord server (it's private, don't ask). Essentially, this bot acts as a "centralised" system for currency on-server, allowing for people to perform trades with the virtual coin for whatever they please and create "NFTs" that hold some sort of value. This will mostly be used as an events system to provide rewards for on-server events hosted by the staff team (primarily myself), but can also be used for general server activity.

### What can you use the bot for?
As mentioned prior, degenBank's main system is the DGN "currency". In its current state (26/11/2021), degenBank has the ability to mint an initial volume of degenCoin (DGN), then distribute it to the community via the `-mint` and `-pull` commands. DGN can then be transferred between users via `-send`. 

"NFTs" can also be created using the bot's `-createnft` command, where you can attach an image and register it with a unique identifier and an initial worth. The NFTs created with the bot can then be sent to other users in a similar manner to regular DGN (the only difference is that you use `-sendnft` and not `-send`), and can also be sold and bought at prices relevant to the current market.

### Why does this exist?
I'm bored.