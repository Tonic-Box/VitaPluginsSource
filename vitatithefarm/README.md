# Vita Tithe Farm
Plays the Tithe Farm minigame for Farming XP and reward points.
- takes seeds from the table
- plants, waters, harvests and replants a round of patches
- clears dead plants
- deposits fruit in the sack
- refills cans with Humidify, or at the water barrels
- drinks stamina and energy potions
- buys rewards if you want it to

### How to use
1. Bring watering cans or Gricoller's can, a seed dibber (unless you have unlocked barehanded planting), a spade, and optionally staminas.
2. Enable the plugin and switch on **Run**.
   - If you are not at Tithe Farm it walks you there.
   - On the Lunar spellbook, with the Magic level and runes for Humidify, the cans are refilled with Humidify. Otherwise they are filled at the water barrels.
   - It will not start below 34 Farming, or without a watering can or Gricoller's can, a spade, and a seed dibber (unless barehanded planting is learned). The reason is shown in chat and the client log.

### How many cans to bring
Each plant needs 3 waterings, and a watering can holds 8.

| Refilled by | Cans to bring |
|---|---|
| Water barrel (no Humidify) | Plants per run × 3 ÷ 8, rounded up: 8 for 20 plants, 9 for 23, 10 for 25. With fewer, fewer plants are planted. |
| Humidify | 8. Humidify only fills empty cans, so with only a few the plugin keeps stopping to cast. |
| Gricoller's can | Just the one. It holds 1,000 waterings, about 333 plants, and frees inventory space. |

### Settings
- **Seed**: Automatic plants the best seed for your level. You can also pick a lower tier.
- **Plants per run** (4 to 25, default 20): how many patches each run plants, in the order of the wiki's 25x4 route; fewer plants drop column 3 and the lone patch first, and 12 or fewer use a small loop in the north half. A run goes: deposit and refill, plant and water each patch in order, water each plant in order as it dries, twice, then harvest in order. If planting takes longer than 140 ticks, that run plants fewer, so no plant can dry out. Without Humidify, fewer are planted when the cans cannot water them all.
- **Drink below energy**: when to drink a potion. Without potions it walks once run energy runs out.
- **Rewards**: buy Gricoller's can, the farmer's outfit (and wear it), the seed box or the herb sack, never going below **Keep at least points**.
- **Max runtime**, **Points target**, **Farming level target**, **Stop after sacks**: when to switch Run off. It finishes the sack it is filling first.
