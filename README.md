# Coal Bag Tracker

Coal Bag Tracker shows the number of coal pieces in your inventory coal bag. Enable it and use **Check** on the bag once. A known empty bag shows `0`. A `?` means the plugin needs a fresh count. Turn off the original Coal Bag overlay if you see two numbers.

## What changes the number

- **Mining.** Coal mined into an open bag increases the count. The tracker includes Celestial ring and Varrock platebody bonus messages from the same mining tick and accounts for overflow into your inventory. The usual capacity is 27, or 36 with a Smithing cape or regular max cape.
- **Superheat Item and furnace smelting.** The tracker matches the ore consumed to the bar produced and uses the change in loose coal to calculate the bag's share. One mithril bar costs four coal. If you have one loose coal, the bag count drops by three. A smithing catalyst halves the cost of the bar it helps produce.
- **Blast Furnace.** The coal you put on the belt belongs to the furnace. Emptying the bag updates the counter from the game's reported amount. Collecting bars from the dispenser spends no further bag coal.
- **Checking, filling and emptying.** The game's reported count takes priority, including partial empties when your inventory is nearly full.

Use **Check** again after a world hop, reconnect, plugin restart, or taking the bag back out of the bank. Coal picked up from the ground can go directly into an open bag without a reliable contents update. Use **Check** after a ground pickup to refresh the number.

## Run and verify

From this directory on Windows, run `.\gradlew.bat test` to run the tests or `.\gradlew.bat run` to launch the development client. On macOS or Linux, use `./gradlew test` or `./gradlew run`. Sign in using the [RuneLite Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Compare the overlay **before** using **Check** with the amount the game then reports.

| In-game check | Expected overlay |
| --- | --- |
| Start with 10 in the bag and 1 loose coal. Superheat one mithril ore. | 7 |
| Repeat at a regular furnace with the same starting amounts. | 7 |
| Repeat with a smithing catalyst as well. | 9 |
| With 4 loose coal and 10 in the bag, superheat one mithril ore. | 10 |
| With no loose coal, superheat a gold ore or attempt a cast that fails. | The previous count |
| Mine one coal into an open bag holding 26 without a Smithing or max cape. Mine another. | 27 after each rock, with the excess coal in inventory |
| With 10 in the bag and one free inventory slot, use Empty outside the bank. | 9 |
| Empty the bag for a Blast Furnace belt deposit, then collect bars. | 0 throughout collection |

## Credits

Based on [WolffTech's Coal Bag plugin](https://github.com/WolffTech/coal-bag-plugin). The overlay, bag message handling and colour settings derive from that project. The listing icon depicts a coal golem carrying a full bag. See [LICENSE](LICENSE) for the code license.
