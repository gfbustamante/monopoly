# Monopoly integration data

This implemented ruleset combines the standard US board and modern fixed taxes ($200 Income Tax, $100 Luxury Tax) with older classic US card effects. Card descriptions below are paraphrases. IDs are local to each deck; each deck contains exactly one of every ID from 0 through 15. The game uses `new Deck(random, 10)` for Chance and `new Deck(random, 2)` for Community Chest. These tables match `MonopolyGame`.

## Board and Space conventions

Board indices run clockwise from GO at 0 to Boardwalk at 39. `Board.get(index)` requires 0–39 and does not wrap. `Space` takes `(String name, int type, int price, int group, int buildingCost, int[] rents)`; supply six rent entries, which are copied. Initial owner is -1, level is 0, and mortgage status is false. `setLevel` accepts 0–5 and ignores other values. Owners are player IDs; -1 means unowned.

| Group ID | Color | Street indices |
| --- | --- | --- |
| 0 | Brown | 1, 3 |
| 1 | Light blue | 6, 8, 9 |
| 2 | Pink | 11, 13, 14 |
| 3 | Orange | 16, 18, 19 |
| 4 | Red | 21, 23, 24 |
| 5 | Yellow | 26, 27, 29 |
| 6 | Green | 31, 32, 34 |
| 7 | Dark blue | 37, 39 |
| -1 | Every nonstreet space | All remaining indices |

Street rent indices 0–5 mean unimproved, one house, two houses, three houses, four houses, hotel. Building cost is the price of one house or the hotel upgrade after four houses. The game doubles unimproved street rent for a complete color group, subject to its mortgage rules; the stored rent remains the printed base amount. Mortgaged properties collect no rent; the game handles that check.

Railroad rent indices 0–3 mean one through four railroads owned: $25, $50, $100, $200. Utility rent indices 0–1 are dice multipliers for one or two utilities owned: 4 and 10. Unused railroad and utility entries are zero. Do not use street building level to select railroad or utility rent; count the owner's properties instead. Nonstreet building cost is zero. Other spaces have six zero rents. Tax `price` is the amount due, not a purchase price. Only streets, railroads, and utilities may be purchased or mortgaged. `getMortgage()` always returns integer `price / 2`; the game must check eligibility first.

## Chance

| ID | Effect |
| --- | --- |
| 0 | Advance to GO (0); collect $200. |
| 1 | Advance to Illinois Avenue (24); collect $200 if passing GO; resolve the destination. |
| 2 | Advance to St. Charles Place (11); collect $200 if passing GO; resolve the destination. |
| 3 | Advance to Reading Railroad (5); collect $200 if passing GO; resolve with normal railroad rent. |
| 4 | Advance to Boardwalk (39); resolve the destination. |
| 5 | Advance to the nearest railroad clockwise. Collect $200 if passing GO. If unowned, offer purchase or auction. If owned by another player and not mortgaged, pay twice its normal railroad rent. |
| 6 | Same nearest-railroad effect as ID 5; this is a separate card. |
| 7 | Advance to the nearest utility clockwise. Collect $200 if passing GO. If unowned, offer purchase or auction. If owned by another player and not mortgaged, roll two dice and pay 10 times their total. |
| 8 | Move backward three spaces; resolve the destination; do not collect GO salary. |
| 9 | Go directly to Jail (10); do not collect GO salary. |
| 10 | Get Out of Jail Free; hold until used or transferred. |
| 11 | Receive a $50 bank dividend. |
| 12 | Receive $150 when a building loan matures. |
| 13 | Pay the bank a $15 poor tax. |
| 14 | Pay the bank $25 per house and $100 per hotel you own. |
| 15 | Pay each other player $50 as chairman. |

## Community Chest

| ID | Effect |
| --- | --- |
| 0 | Advance to GO (0); collect $200. |
| 1 | Go directly to Jail (10); do not collect GO salary. |
| 2 | Get Out of Jail Free; hold until used or transferred. |
| 3 | Receive $200 for a bank error. |
| 4 | Pay the bank a $50 doctor's fee. |
| 5 | Receive $50 from a stock sale. |
| 6 | Receive $100 from a maturing holiday fund. |
| 7 | Receive a $20 income tax refund. |
| 8 | Collect $50 from each other player for grand opera opening night. |
| 9 | Receive $100 from a maturing life insurance policy. |
| 10 | Pay the bank $100 in hospital fees. |
| 11 | Pay the bank $150 in school fees. |
| 12 | Receive $25 for services. |
| 13 | Pay the bank $40 per house and $115 per hotel you own for street repairs. |
| 14 | Receive $10 for second place in a beauty contest. |
| 15 | Receive a $100 inheritance. |

## Deck and effect handling

`Deck(Random random, int jailCardId)` requires a nonnull Random and a jail card ID from 0–15. It shuffles once at construction. `draw()` removes the top card; `size()` counts only cards currently in the deck. Empty draws return -1. After handling an ordinary card, the game calls `returnCard(id)` to append it to the bottom. Keep a jail card outside its source deck while held, including during transfers between players; return it to that deck when used. Returning an invalid ID or a card already in the deck does nothing. No draw automatically returns any card.

The game handles cash transfers, ownership, auctions, mortgages, jail state, movement, and card effects. Award GO salary once per qualifying move, including advance-to-GO cards. Moving backward three spaces from Chance at 36 lands on Chest at 33: handle that draw too. Hotels count as hotels only, not as four additional houses, for repair cards. Free Parking pays nothing. A nearest-utility card requires a fresh rent roll; that roll does not trigger movement or doubles rules. No rent is due on the player's own property.

Historical card editions vary. This is a deliberately specified teaching ruleset rather than a claim that all editions match it. Rule reference: [Hasbro classic instructions](https://www.hasbro.com/common/instruct/monins.pdf). No official artwork, card scans, or rulebook is distributed with this project.
