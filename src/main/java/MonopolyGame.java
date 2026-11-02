import java.util.Random;

public class MonopolyGame {
    private Board board;
    private Player[] players;
    private Deck chance;
    private Deck chest;
    private Dice dice;
    private ConsoleInput input;
    private int houses = 32;
    private int hotels = 12;
    private int turns;
    private int current;
    private int doubles;
    private boolean stopped;

    public MonopolyGame(String[] names, ConsoleInput input, Dice dice, Random random) {
        board = new Board();
        players = new Player[names.length];
        for (int index = 0; index < names.length; index++) { players[index] = new Player(names[index]); }
        this.input = input;
        this.dice = dice;
        chance = new Deck(random, 10);
        chest = new Deck(random, 2);
    }

    public Board getBoard() { return board; }
    public Player getPlayer(int index) { return players[index]; }
    public int getHouses() { return houses; }
    public int getHotels() { return hotels; }
    public Deck getChance() { return chance; }
    public Deck getChest() { return chest; }
    public int getTurns() { return turns; }

    public int activeCount() {
        int count = 0;
        for (int index = 0; index < players.length; index++) {
            if (!players[index].isBankrupt()) { count++; }
        }
        return count;
    }

    public String result() {
        if (activeCount() == 1) {
            for (int index = 0; index < players.length; index++) {
                if (!players[index].isBankrupt()) {
                    return "Winner: " + players[index].getName() + " after " + turns + " turns.";
                }
            }
        }
        return "Session stopped after " + turns + " turns; " + activeCount() + " players remain. No winner declared.";
    }

    public void play(int maximumTurns) {
        while (activeCount() > 1 && !input.isClosed() && !stopped
                && (maximumTurns == 0 || turns < maximumTurns)) {
            if (!players[current].isBankrupt()) {
                input.say("\nTURN: " + players[current].getName());
                status();
                management(current);
                if (!stopped && !input.isClosed() && !players[current].isBankrupt()) {
                    takeTurn(current);
                    turns++;
                }
            }
            current = (current + 1) % players.length;
        }
    }

    void takeTurn(int player) {
        doubles = 0;
        boolean again;
        do {
            again = false;
            if (players[player].isJailed()) {
                jailTurn(player);
            } else {
                int first = dice.roll();
                int second = dice.roll();
                again = processRoll(player, first, second);
            }
            if (again && !players[player].isBankrupt() && activeCount() > 1) { management(player); }
        } while (again && !players[player].isBankrupt() && activeCount() > 1 && !stopped && !input.isClosed());
    }

    boolean processRoll(int player, int first, int second) {
        input.say(players[player].getName() + " rolled " + first + " + " + second);
        if (first == second) { doubles++; } else { doubles = 0; }
        if (doubles == 3) {
            sendToJail(player);
            return false;
        }
        move(player, first + second);
        land(player, first + second, 0);
        return first == second && !players[player].isJailed() && !players[player].isBankrupt();
    }

    void jailTurn(int player) {
        Player person = players[player];
        int option = input.choose("Jail: 0 try doubles, 1 pay $50, 2 use held card", 0, 2,
                person.hasChanceCard() || person.hasChestCard() ? 2 : 0);
        if (option == 2 && !person.hasChanceCard() && !person.hasChestCard()) {
            input.say("No held card; trying doubles.");
            option = 0;
        }
        if (option == 1 || option == 2) {
            if (option == 1 && !pay(player, -1, 50)) { return; }
            if (option == 2) { releaseCard(player); }
            person.setJailed(false);
            person.setJailTurns(0);
            boolean again = processRoll(player, dice.roll(), dice.roll());
            while (again && activeCount() > 1 && !stopped && !input.isClosed()) {
                management(player);
                if (!stopped && !input.isClosed()) { again = processRoll(player, dice.roll(), dice.roll()); }
            }
            return;
        }
        int first = dice.roll();
        int second = dice.roll();
        input.say("Jail roll: " + first + " + " + second);
        person.setJailTurns(person.getJailTurns() + 1);
        if (first == second || person.getJailTurns() == 3) {
            if (first != second && !pay(player, -1, 50)) { return; }
            person.setJailed(false);
            person.setJailTurns(0);
            move(player, first + second);
            land(player, first + second, 0);
        }
    }

    void sendToJail(int player) {
        players[player].setPosition(10);
        players[player].setJailed(true);
        players[player].setJailTurns(0);
        input.say(players[player].getName() + " goes directly to jail. No GO salary.");
    }

    void move(int player, int steps) {
        int destination = players[player].getPosition() + steps;
        if (steps > 0 && destination >= 40) { players[player].changeCash(200); }
        players[player].setPosition((destination % 40 + 40) % 40);
    }

    void advance(int player, int destination) {
        int steps = (destination - players[player].getPosition() + 40) % 40;
        if (steps == 0 && destination == 0) { steps = 40; }
        move(player, steps);
    }

    void land(int player, int roll, int special) {
        if (players[player].isBankrupt()) { return; }
        int position = players[player].getPosition();
        Space space = board.get(position);
        input.say("Landed on " + position + ": " + space.getName());
        if (space.getType() == Space.GO_TO_JAIL) { sendToJail(player); }
        else if (space.getType() == Space.TAX) { pay(player, -1, space.getPrice()); }
        else if (space.getType() == Space.CHANCE || space.getType() == Space.CHEST) {
            boolean isChance = space.getType() == Space.CHANCE;
            Deck deck = isChance ? chance : chest;
            int card = deck.draw();
            applyCard(player, isChance, card);
            if (card != (isChance ? 10 : 2)) { deck.returnCard(card); }
        } else if (space.getType() == Space.STREET || space.getType() == Space.RAILROAD
                || space.getType() == Space.UTILITY) {
            if (space.getOwner() == -1) {
                int buy = input.choose("Buy " + space.getName() + " for $" + space.getPrice() + "? 1 yes, 0 auction",
                        0, 1, players[player].getCash() >= space.getPrice() + 100 ? 1 : 0);
                if (buy == 1 && players[player].getCash() >= space.getPrice()) {
                    players[player].changeCash(-space.getPrice());
                    space.setOwner(player);
                } else { auction(position); }
            } else if (space.getOwner() != player && !space.isMortgaged()) {
                if (special == 2 && space.getType() == Space.UTILITY) {
                    roll = dice.roll() + dice.roll();
                    input.say("Utility card fresh roll: " + roll);
                }
                int amount = rent(position, roll);
                if (special == 1 && space.getType() == Space.RAILROAD) { amount *= 2; }
                if (special == 2 && space.getType() == Space.UTILITY) { amount = roll * 10; }
                input.say("Rent: $" + amount);
                pay(player, space.getOwner(), amount);
            }
        }
    }

    boolean ownsGroup(int owner, int group) {
        if (group < 0) { return false; }
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getGroup() == group && space.getOwner() != owner) { return false; }
        }
        return true;
    }

    int ownedOfType(int owner, int type) {
        int count = 0;
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getOwner() == owner && space.getType() == type) { count++; }
        }
        return count;
    }

    int rent(int position, int roll) {
        Space space = board.get(position);
        if (space.getOwner() < 0 || space.isMortgaged()) { return 0; }
        if (space.getType() == Space.RAILROAD) {
            int amount = 25;
            for (int count = 1; count < ownedOfType(space.getOwner(), Space.RAILROAD); count++) { amount *= 2; }
            return amount;
        }
        if (space.getType() == Space.UTILITY) {
            return roll * (ownedOfType(space.getOwner(), Space.UTILITY) == 2 ? 10 : 4);
        }
        if (space.getType() == Space.STREET) {
            int amount = space.getRent(space.getLevel());
            if (space.getLevel() == 0 && ownsGroup(space.getOwner(), space.getGroup())) { amount *= 2; }
            return amount;
        }
        return 0;
    }

    boolean groupClear(int group) {
        if (group < 0) { return true; }
        for (int index = 0; index < 40; index++) {
            if (board.get(index).getGroup() == group && board.get(index).getLevel() > 0) { return false; }
        }
        return true;
    }

    boolean canBuild(int player, int position) {
        Space space = board.get(position);
        if (space.getType() != Space.STREET || space.getOwner() != player || space.getLevel() >= 5
                || !ownsGroup(player, space.getGroup()) || players[player].getCash() < space.getBuildingCost()) { return false; }
        for (int index = 0; index < 40; index++) {
            Space other = board.get(index);
            if (other.getGroup() == space.getGroup()
                    && (other.isMortgaged() || other.getLevel() < space.getLevel())) { return false; }
        }
        return space.getLevel() == 4 ? hotels > 0 : houses > 0;
    }

    boolean build(int player, int position) {
        if (!canBuild(player, position)) { return false; }
        Space space = board.get(position);
        if (space.getLevel() == 4) { hotels--; houses += 4; } else { houses--; }
        space.setLevel(space.getLevel() + 1);
        players[player].changeCash(-space.getBuildingCost());
        return true;
    }

    boolean sellBuilding(int player, int position) {
        Space space = board.get(position);
        if (space.getOwner() != player || space.getLevel() == 0) { return false; }
        for (int index = 0; index < 40; index++) {
            Space other = board.get(index);
            if (other.getGroup() == space.getGroup() && other.getLevel() > space.getLevel()) { return false; }
        }
        if (space.getLevel() == 5) {
            if (houses < 4) { return false; }
            hotels++; houses -= 4;
        } else { houses++; }
        space.setLevel(space.getLevel() - 1);
        players[player].changeCash(space.getBuildingCost() / 2);
        return true;
    }

    boolean sellGroup(int player, int position) {
        Space chosen = board.get(position);
        if (chosen.getGroup() < 0 || !ownsGroup(player, chosen.getGroup()) || groupClear(chosen.getGroup())) { return false; }
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getGroup() == chosen.getGroup()) { removeBuildings(player, space); }
        }
        return true;
    }

    private void removeBuildings(int player, Space space) {
        if (space.getLevel() == 5) { hotels++; } else { houses += space.getLevel(); }
        players[player].changeCash(space.getLevel() * space.getBuildingCost() / 2);
        space.setLevel(0);
    }

    boolean mortgage(int player, int position) {
        Space space = board.get(position);
        if (space.getOwner() != player || space.isMortgaged() || !groupClear(space.getGroup())) { return false; }
        space.setMortgaged(true);
        players[player].changeCash(space.getMortgage());
        return true;
    }

    int interest(Space space) { return (space.getMortgage() + 9) / 10; }

    boolean unmortgage(int player, int position) {
        Space space = board.get(position);
        int cost = space.getMortgage() + interest(space);
        if (space.getOwner() != player || !space.isMortgaged() || players[player].getCash() < cost) { return false; }
        players[player].changeCash(-cost);
        space.setMortgaged(false);
        return true;
    }

    void auction(int position) {
        Space space = board.get(position);
        input.say("Auction: " + space.getName() + ". $1 minimum; 0 permanently passes.");
        boolean[] passed = new boolean[players.length];
        int high = 0;
        int winner = -1;
        boolean bidding = true;
        while (bidding && !input.isClosed()) {
            bidding = false;
            for (int bidder = 0; bidder < players.length; bidder++) {
                if (!players[bidder].isBankrupt() && !passed[bidder] && bidder != winner) {
                    bidding = true;
                    int maximum = players[bidder].getCash();
                    int automatic = high + 10;
                    if (automatic > maximum - 50 || automatic > space.getPrice() * 3 / 2) { automatic = 0; }
                    int bid = input.choose(players[bidder].getName() + ": current $" + high + "; bid or 0 pass",
                            0, maximum, automatic);
                    while (bid > 0 && bid <= high && !input.isClosed()) {
                        bid = input.choose("Bid must exceed $" + high + ", or 0 pass", 0, maximum, 0);
                    }
                    if (bid == 0) { passed[bidder] = true; }
                    else { high = bid; winner = bidder; }
                }
            }
        }
        if (winner >= 0) {
            space.setOwner(winner);
            players[winner].changeCash(-high);
            input.say(players[winner].getName() + " wins auction for $" + high);
        }
    }

    int liquidationValue(int player) {
        int value = players[player].getCash();
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getOwner() == player) {
                if (!space.isMortgaged()) { value += space.getMortgage(); }
                value += space.getLevel() * space.getBuildingCost() / 2;
            }
        }
        return value;
    }

    boolean pay(int payer, int creditor, int amount) {
        if (players[payer].isBankrupt()) { return false; }
        while (players[payer].getCash() < amount && !input.isClosed()) {
            input.say(players[payer].getName() + " owes $" + amount + "; cash $" + players[payer].getCash());
            if (input.isAutomated()) {
                if (!raiseOne(payer)) { break; }
            } else {
                int option = input.choose("Debt: 1 sell building, 2 sell entire color's buildings, 3 mortgage, 4 trade, 5 assets, 6 sell jail card, 0 bankruptcy",
                        0, 6, 0);
                if (option == 0) {
                    if (liquidationValue(payer) < amount) { break; }
                    input.say("Your assets can cover this debt. Sell buildings or mortgage first.");
                } else if (option == 4) { tradeMenu(payer); }
                else if (option == 5) { showAssets(payer); }
                else if (option == 6) { cardSaleMenu(payer); }
                else {
                    int position = input.choose("Board position", 0, 39, 0);
                    boolean done = option == 1 ? sellBuilding(payer, position)
                            : option == 2 ? sellGroup(payer, position) : mortgage(payer, position);
                    if (!done) { input.say("That transaction is not legal."); }
                }
            }
        }
        if (input.isClosed()) { return false; }
        if (players[payer].getCash() < amount) {
            bankrupt(payer, creditor);
            return false;
        }
        players[payer].changeCash(-amount);
        if (creditor >= 0) { players[creditor].changeCash(amount); }
        return true;
    }

    boolean raiseOne(int player) {
        for (int index = 0; index < 40; index++) { if (sellBuilding(player, index)) { return true; } }
        for (int index = 0; index < 40; index++) { if (sellGroup(player, index)) { return true; } }
        for (int index = 0; index < 40; index++) { if (mortgage(player, index)) { return true; } }
        return false;
    }

    void bankrupt(int player, int creditor) {
        Player debtor = players[player];
        debtor.setBankrupt(true);
        input.say(debtor.getName() + " is bankrupt.");
        int fees = 0;
        int[] returned = new int[40];
        int returnedCount = 0;
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getOwner() == player) {
                removeBuildings(player, space);
                space.setOwner(creditor);
                if (creditor < 0) {
                    space.setMortgaged(false);
                    returned[returnedCount++] = index;
                } else if (space.isMortgaged()) { fees += interest(space); }
            }
        }
        if (creditor >= 0) {
            players[creditor].changeCash(debtor.getCash());
            if (debtor.hasChanceCard()) { players[creditor].setChanceCard(true); debtor.setChanceCard(false); }
            if (debtor.hasChestCard()) { players[creditor].setChestCard(true); debtor.setChestCard(false); }
        } else {
            if (debtor.hasChanceCard()) { chance.returnCard(10); debtor.setChanceCard(false); }
            if (debtor.hasChestCard()) { chest.returnCard(2); debtor.setChestCard(false); }
        }
        debtor.changeCash(-debtor.getCash());
        if (creditor >= 0 && fees > 0 && activeCount() > 1) {
            input.say("Inherited mortgage interest due: $" + fees + ". Mortgages remain in place.");
            pay(creditor, -1, fees);
        }
        for (int index = 0; index < returnedCount; index++) { auction(returned[index]); }
    }

    boolean canTradeDeed(int player, int position) {
        return position == 40 || (board.get(position).getOwner() == player && groupClear(board.get(position).getGroup()));
    }

    boolean trade(int first, int second, int firstDeed, int secondDeed, int firstCash, int secondCash) {
        if (first == second || players[first].isBankrupt() || players[second].isBankrupt()
                || firstDeed < 0 || firstDeed > 40 || secondDeed < 0 || secondDeed > 40
                || firstCash < 0 || secondCash < 0 || !canTradeDeed(first, firstDeed) || !canTradeDeed(second, secondDeed)
                || firstCash > players[first].getCash() || secondCash > players[second].getCash()) { return false; }
        int firstFee = secondDeed < 40 && board.get(secondDeed).isMortgaged() ? interest(board.get(secondDeed)) : 0;
        int secondFee = firstDeed < 40 && board.get(firstDeed).isMortgaged() ? interest(board.get(firstDeed)) : 0;
        if (players[first].getCash() - firstCash + secondCash < firstFee
                || players[second].getCash() - secondCash + firstCash < secondFee) { return false; }
        players[first].changeCash(secondCash - firstCash - firstFee);
        players[second].changeCash(firstCash - secondCash - secondFee);
        if (firstDeed < 40) { board.get(firstDeed).setOwner(second); }
        if (secondDeed < 40) { board.get(secondDeed).setOwner(first); }
        return true;
    }

    private void tradeMenu(int player) {
        status();
        int other = input.choose("Trade partner number (shown starting at 1)", 1, players.length, 1) - 1;
        if (other == player || players[other].isBankrupt()) { input.say("Choose another active player."); return; }
        showAssets(player);
        showAssets(other);
        int offered = input.choose("Your offered deed position; 40 none", 0, 40, 40);
        int requested = input.choose("Their offered deed position; 40 none", 0, 40, 40);
        int offeredCash = input.choose("Your offered cash", 0, players[player].getCash(), 0);
        int requestedCash = input.choose("Their offered cash", 0, players[other].getCash(), 0);
        input.say("Offer: " + players[player].getName() + " gives deed " + offered + " and $" + offeredCash
                + "; " + players[other].getName() + " gives deed " + requested + " and $" + requestedCash
                + ". Each recipient pays immediate 10% mortgage interest; principal stays mortgaged.");
        int accepted = input.choose(players[other].getName() + ", accept? 1 yes, 0 no", 0, 1, 0);
        if (accepted == 1) {
            input.say(trade(player, other, offered, requested, offeredCash, requestedCash)
                    ? "Trade completed." : "Illegal trade or insufficient cash for transfer interest.");
        }
    }

    private void management(int player) {
        if (input.isAutomated()) { autoManage(player); return; }
        int turnPlayer = player;
        boolean done = false;
        while (!done && !input.isClosed() && !stopped) {
            input.say("Managing " + players[player].getName() + "; next roll belongs to " + players[turnPlayer].getName());
            int option = input.choose("Before rolling: 0 finish management / roll, 1 board, 2 assets, 3 build, 4 sell building, 5 mortgage, 6 unmortgage, 7 trade, 8 manage another player, 9 sell color's buildings, 10 stop session, 11 sell jail card",
                    0, 11, 0);
            if (option == 0) { done = true; }
            else if (option == 1) { showBoard(); }
            else if (option == 2) { showAssets(player); }
            else if (option == 7) { tradeMenu(player); }
            else if (option == 11) { cardSaleMenu(player); }
            else if (option == 8) {
                int other = input.choose("Player to manage", 1, players.length, 1) - 1;
                if (!players[other].isBankrupt()) { player = other; }
            } else if (option == 10) { stopped = true; }
            else {
                int position = input.choose("Board position", 0, 39, 0);
                boolean success = false;
                if (option == 3) { success = build(player, position); }
                if (option == 4) { success = sellBuilding(player, position); }
                if (option == 5) { success = mortgage(player, position); }
                if (option == 6) { success = unmortgage(player, position); }
                if (option == 9) { success = sellGroup(player, position); }
                input.say(success ? "Completed." : "Not legal: check ownership, cash, even levels, mortgages, and building supply.");
            }
        }
    }

    boolean sellJailCard(int seller, int buyer, boolean isChance, int price) {
        if (seller == buyer || players[seller].isBankrupt() || players[buyer].isBankrupt()
                || price < 0 || players[buyer].getCash() < price) { return false; }
        if (isChance) {
            if (!players[seller].hasChanceCard() || players[buyer].hasChanceCard()) { return false; }
            players[seller].setChanceCard(false);
            players[buyer].setChanceCard(true);
        } else {
            if (!players[seller].hasChestCard() || players[buyer].hasChestCard()) { return false; }
            players[seller].setChestCard(false);
            players[buyer].setChestCard(true);
        }
        players[seller].changeCash(price);
        players[buyer].changeCash(-price);
        return true;
    }

    private void cardSaleMenu(int seller) {
        status();
        int buyer = input.choose("Buyer player number", 1, players.length, 1) - 1;
        boolean isChance = input.choose("Card: 0 Community Chest, 1 Chance", 0, 1, 1) == 1;
        int price = input.choose("Agreed card price", 0, players[buyer].getCash(), 0);
        int accepted = input.choose(players[buyer].getName() + ", buy this card for $" + price + "? 1 yes, 0 no", 0, 1, 0);
        if (accepted == 1) { input.say(sellJailCard(seller, buyer, isChance, price) ? "Card sold." : "Invalid card sale."); }
    }

    private void autoManage(int player) {
        autoTrade(player);
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getOwner() == player && space.isMortgaged()
                    && players[player].getCash() > 500 + space.getMortgage()) { unmortgage(player, index); }
        }
        for (int round = 0; round < 5; round++) {
            for (int index = 0; index < 40; index++) {
                if (players[player].getCash() > 250 + board.get(index).getBuildingCost()) { build(player, index); }
            }
        }
    }

    private void autoTrade(int player) {
        for (int group = 0; group < 8; group++) {
            int missing = -1;
            int missingCount = 0;
            int owned = 0;
            for (int position = 0; position < 40; position++) {
                Space space = board.get(position);
                if (space.getGroup() == group) {
                    if (space.getOwner() == player) { owned++; }
                    else { missing = position; missingCount++; }
                }
            }
            if (owned > 0 && missingCount == 1) {
                Space space = board.get(missing);
                int seller = space.getOwner();
                int price = space.getPrice() * 2;
                if (seller >= 0 && players[player].getCash() >= price + 250) {
                    trade(player, seller, 40, missing, price, 0);
                }
            }
        }
    }

    void releaseCard(int player) {
        if (players[player].hasChanceCard()) { players[player].setChanceCard(false); chance.returnCard(10); }
        else if (players[player].hasChestCard()) { players[player].setChestCard(false); chest.returnCard(2); }
    }

    int repairs(int player, int perHouse, int perHotel) {
        int amount = 0;
        for (int index = 0; index < 40; index++) {
            Space space = board.get(index);
            if (space.getOwner() == player) {
                amount += space.getLevel() == 5 ? perHotel : space.getLevel() * perHouse;
            }
        }
        return amount;
    }

    void applyCard(int player, boolean isChance, int card) {
        input.say((isChance ? "Chance" : "Community Chest") + " card " + card);
        if (isChance) { chanceCard(player, card); } else { chestCard(player, card); }
    }

    private void chanceCard(int player, int card) {
        if (card <= 4) {
            int[] destinations = {0, 24, 11, 5, 39};
            input.say("Advance to " + board.get(destinations[card]).getName());
            advance(player, destinations[card]);
            land(player, 0, 0);
        } else if (card == 5 || card == 6 || card == 7) {
            int wanted = card == 7 ? Space.UTILITY : Space.RAILROAD;
            int destination = (players[player].getPosition() + 1) % 40;
            while (board.get(destination).getType() != wanted) { destination = (destination + 1) % 40; }
            input.say("Advance to nearest " + (wanted == Space.UTILITY ? "utility" : "railroad"));
            advance(player, destination);
            land(player, 0, wanted == Space.UTILITY ? 2 : 1);
        } else if (card == 8) {
            input.say("Go back three spaces."); move(player, -3); land(player, 0, 0);
        } else if (card == 9) { sendToJail(player); }
        else if (card == 10) { input.say("Keep Get Out of Jail Free."); players[player].setChanceCard(true); }
        else if (card == 11) { input.say("Bank dividend: collect $50."); players[player].changeCash(50); }
        else if (card == 12) { input.say("Building loan matures: collect $150."); players[player].changeCash(150); }
        else if (card == 13) { input.say("Poor tax: pay $15."); pay(player, -1, 15); }
        else if (card == 14) { input.say("Repairs: $25 per house, $100 per hotel."); pay(player, -1, repairs(player, 25, 100)); }
        else if (card == 15) {
            input.say("Chairman: pay each other player $50, in player order.");
            for (int other = 0; other < players.length && !players[player].isBankrupt(); other++) {
                if (other != player && !players[other].isBankrupt()) { pay(player, other, 50); }
            }
        }
    }

    private void chestCard(int player, int card) {
        if (card == 0) { input.say("Advance to GO."); advance(player, 0); }
        else if (card == 1) { sendToJail(player); }
        else if (card == 2) { input.say("Keep Get Out of Jail Free."); players[player].setChestCard(true); }
        else if (card == 3) { input.say("Bank error: collect $200."); players[player].changeCash(200); }
        else if (card == 4) { input.say("Doctor: pay $50."); pay(player, -1, 50); }
        else if (card == 5) { input.say("Stock sale: collect $50."); players[player].changeCash(50); }
        else if (card == 6) { input.say("Holiday fund: collect $100."); players[player].changeCash(100); }
        else if (card == 7) { input.say("Tax refund: collect $20."); players[player].changeCash(20); }
        else if (card == 8) {
            input.say("Opera opening: collect $50 from each other player, in player order.");
            for (int other = 0; other < players.length && !players[player].isBankrupt(); other++) {
                if (other != player && !players[other].isBankrupt()) { pay(other, player, 50); }
            }
        } else if (card == 9) { input.say("Life insurance: collect $100."); players[player].changeCash(100); }
        else if (card == 10) { input.say("Hospital: pay $100."); pay(player, -1, 100); }
        else if (card == 11) { input.say("School: pay $150."); pay(player, -1, 150); }
        else if (card == 12) { input.say("Consultancy: collect $25."); players[player].changeCash(25); }
        else if (card == 13) { input.say("Street repairs: $40 per house, $115 per hotel."); pay(player, -1, repairs(player, 40, 115)); }
        else if (card == 14) { input.say("Beauty contest: collect $10."); players[player].changeCash(10); }
        else if (card == 15) { input.say("Inheritance: collect $100."); players[player].changeCash(100); }
    }

    public void status() {
        for (int index = 0; index < players.length; index++) {
            Player player = players[index];
            input.say((index + 1) + ". " + player.getName() + ": $" + player.getCash() + ", "
                    + board.get(player.getPosition()).getName() + (player.isJailed() ? " [JAILED]" : "")
                    + (player.isBankrupt() ? " [BANKRUPT]" : ""));
        }
    }

    public void showAssets(int player) {
        input.say(players[player].getName() + ": $" + players[player].getCash() + "; jail cards: Chance="
                + players[player].hasChanceCard() + ", Chest=" + players[player].hasChestCard());
        for (int index = 0; index < 40; index++) {
            if (board.get(index).getOwner() == player) { describe(index); }
        }
    }

    public void showBoard() {
        for (int index = 0; index < 40; index++) { describe(index); }
        input.say("Bank buildings: " + houses + " houses, " + hotels + " hotels.");
    }

    private void describe(int index) {
        Space space = board.get(index);
        if (space.getType() != Space.STREET && space.getType() != Space.RAILROAD && space.getType() != Space.UTILITY) {
            input.say(index + ": " + space.getName() + (space.getType() == Space.TAX ? " | pay $" + space.getPrice() : ""));
            return;
        }
        input.say(index + ": " + space.getName() + " | price $" + space.getPrice() + " | group " + space.getGroup()
                + " | owner " + (space.getOwner() < 0 ? "Bank" : players[space.getOwner()].getName())
                + " | level " + space.getLevel() + (space.isMortgaged() ? " MORTGAGED" : "")
                + " | mortgage $" + space.getMortgage() + " | building $" + space.getBuildingCost());
        if (space.getType() == Space.STREET) {
            input.say("  Printed rents [bare, 1 house, 2, 3, 4, hotel]: $" + space.getRent(0) + ", $"
                    + space.getRent(1) + ", $" + space.getRent(2) + ", $" + space.getRent(3) + ", $"
                    + space.getRent(4) + ", $" + space.getRent(5));
        } else if (space.getType() == Space.RAILROAD) {
            input.say("  Rents with 1/2/3/4 railroads: $25/$50/$100/$200.");
        } else { input.say("  Rent: dice total times 4 (one utility) or 10 (both)."); }
    }
}
