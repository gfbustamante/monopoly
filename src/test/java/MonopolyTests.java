import java.util.Random;
import java.util.Scanner;

public class MonopolyTests {
    private static int checks;
    private static int failures;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + description);
        }
    }

    private static MonopolyGame game(int[] rolls) {
        return new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                new ConsoleInput(new Scanner(""), true, true), new Dice(rolls), new Random(42));
    }

    private static MonopolyGame interactive(String answers) {
        return new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                new ConsoleInput(new Scanner(answers), false, true), new Dice(new int[0]), new Random(42));
    }

    private static void cash(MonopolyGame game, int player, int amount) {
        game.getPlayer(player).changeCash(amount - game.getPlayer(player).getCash());
    }

    private static void own(MonopolyGame game, int player, int[] positions) {
        for (int position = 0; position < positions.length; position++) {
            game.getBoard().get(positions[position]).setOwner(player);
        }
    }

    private static void hold(MonopolyGame game, int player, boolean isChance) {
        Deck deck = isChance ? game.getChance() : game.getChest();
        int jailCard = isChance ? 10 : 2;
        for (int attempt = 0; attempt < 16; attempt++) {
            int card = deck.draw();
            if (card == jailCard) {
                game.applyCard(player, isChance, card);
                return;
            }
            deck.returnCard(card);
        }
        check(false, "Jail card must be available to draw");
    }

    private static void invariants(MonopolyGame game, String description) {
        int houses = game.getHouses();
        int hotels = game.getHotels();
        int chanceHeld = 0;
        int chestHeld = 0;
        boolean validPlayers = true;
        boolean validDeeds = true;
        for (int player = 0; player < 3; player++) {
            Player person = game.getPlayer(player);
            validPlayers = validPlayers && person.getCash() >= 0
                    && person.getPosition() >= 0 && person.getPosition() < 40;
            if (person.hasChanceCard()) { chanceHeld++; }
            if (person.hasChestCard()) { chestHeld++; }
        }
        for (int position = 0; position < 40; position++) {
            Space space = game.getBoard().get(position);
            int owner = space.getOwner();
            validDeeds = validDeeds && owner >= -1 && owner < 3
                    && space.getLevel() >= 0 && space.getLevel() <= 5;
            if (owner >= 0 && owner < 3) {
                validDeeds = validDeeds && !game.getPlayer(owner).isBankrupt();
            }
            if (space.getLevel() == 5) { hotels++; }
            else { houses += space.getLevel(); }
            if (space.getLevel() > 0) {
                validDeeds = validDeeds && owner >= 0 && space.getType() == Space.STREET
                        && !space.isMortgaged();
            }
        }
        check(validPlayers, description + " player cash and positions");
        check(validDeeds, description + " active owners and legal levels");
        check(houses == 32 && game.getHouses() >= 0, description + " house conservation");
        check(hotels == 12 && game.getHotels() >= 0, description + " hotel conservation");
        check(chanceHeld <= 1 && game.getChance().size() + chanceHeld == 16,
                description + " Chance deck conservation");
        check(chestHeld <= 1 && game.getChest().size() + chestHeld == 16,
                description + " Chest deck conservation");
    }

    private static void boardAndRent() {
        MonopolyGame game = game(new int[0]);
        Board board = game.getBoard();
        check(board.size() == 40, "Board has 40 spaces");
        int[] counts = new int[10];
        for (int position = 0; position < board.size(); position++) {
            Space space = board.get(position);
            counts[space.getType()]++;
            check(space.getOwner() == -1 && space.getLevel() == 0 && !space.isMortgaged(),
                    "Initial deed state at " + position);
        }
        check(counts[Space.STREET] == 22 && counts[Space.RAILROAD] == 4
                && counts[Space.UTILITY] == 2, "Property type counts");
        check(board.get(39).getPrice() == 400 && board.get(39).getRent(5) == 2000,
                "Boardwalk price and hotel rent");
        check(board.get(4).getPrice() == 200 && board.get(38).getPrice() == 100, "Tax amounts");
        check(game.rent(1, 7) == 0, "Unowned deed has no rent");
        own(game, 0, new int[] {1});
        check(game.rent(1, 7) == 2 && !game.ownsGroup(0, 0), "Incomplete color base rent");
        own(game, 0, new int[] {3});
        check(game.ownsGroup(0, 0) && game.rent(1, 7) == 4, "Complete color doubles base rent");
        check(game.mortgage(0, 3), "Mortgage sibling");
        check(game.rent(1, 7) == 4 && game.rent(3, 7) == 0, "Mortgaged sibling preserves doubled rent");
        check(!game.build(0, 1), "Mortgaged sibling blocks building");
        int[] railroads = {5, 15, 25, 35};
        int expectedRent = 25;
        for (int railroad = 0; railroad < railroads.length; railroad++) {
            board.get(railroads[railroad]).setOwner(0);
            check(game.rent(5, 7) == expectedRent, "Railroad count rent " + (railroad + 1));
            expectedRent *= 2;
        }
        game.mortgage(0, 15);
        check(game.rent(5, 7) == 200 && game.rent(15, 7) == 0, "Mortgaged railroad still counts siblings");
        own(game, 0, new int[] {12});
        check(game.rent(12, 7) == 28, "Single utility multiplier");
        own(game, 0, new int[] {28});
        check(game.rent(12, 7) == 70, "Two utilities multiplier");
        game.mortgage(0, 28);
        check(game.rent(12, 7) == 70 && game.rent(28, 7) == 0, "Mortgaged utility sibling");
        invariants(game, "Rent tests");
    }

    private static void buildingsAndMortgages() {
        MonopolyGame game = game(new int[0]);
        cash(game, 0, 100000);
        own(game, 0, new int[] {1, 3});
        check(!game.build(1, 1) && !game.build(0, 5), "Build requires owned street");
        check(game.build(0, 1) && !game.build(0, 1), "Build evenly");
        check(!game.mortgage(0, 3), "Buildings on sibling block mortgage");
        check(game.build(0, 3), "Catch up sibling");
        check(game.sellBuilding(0, 1) && !game.sellBuilding(0, 1), "Sell evenly");
        check(game.build(0, 1), "Restore even level");
        for (int level = 2; level <= 5; level++) {
            check(game.build(0, 1) && game.build(0, 3), "Build brown level " + level);
        }
        check(game.getHouses() == 32 && game.getHotels() == 10, "Hotel returns four houses");
        check(!game.build(0, 1), "Cannot exceed hotel");
        check(game.rent(1, 6) == 250, "Hotel rent");
        check(game.sellBuilding(0, 1), "Hotel downgrade");
        check(game.getHouses() == 28 && game.getHotels() == 11, "Downgrade consumes four houses");
        check(!game.sellBuilding(0, 1), "Cannot sell below sibling hotel");
        int beforeSale = game.getPlayer(0).getCash();
        check(game.sellGroup(0, 1), "Bulk sale mixed hotel and houses");
        check(game.getPlayer(0).getCash() == beforeSale + 225, "Bulk sale half building costs");
        check(game.getHouses() == 32 && game.getHotels() == 12, "Bulk sale restores stock");
        check(!game.sellGroup(0, 1), "Empty group cannot sell again");
        int beforeMortgage = game.getPlayer(0).getCash();
        check(game.mortgage(0, 1), "Mortgage clear group");
        check(game.getPlayer(0).getCash() == beforeMortgage + 30, "Mortgage principal credited");
        check(!game.mortgage(0, 1) && !game.unmortgage(1, 1), "Mortgage ownership and duplicate validation");
        cash(game, 0, 32);
        check(!game.unmortgage(0, 1) && game.getBoard().get(1).isMortgaged(), "Unmortgage requires fee cash");
        cash(game, 0, 33);
        check(game.unmortgage(0, 1) && game.getPlayer(0).getCash() == 0, "Unmortgage principal plus ten percent");
        check(game.interest(game.getBoard().get(37)) == 18, "Interest rounds up");
        check(!game.build(0, 1), "Insufficient building cash");
        invariants(game, "Buildings");

        game = game(new int[0]);
        cash(game, 0, 100000);
        own(game, 0, new int[] {1, 3, 6, 8, 9, 11, 13, 14, 16, 18, 19});
        for (int level = 0; level < 5; level++) {
            game.build(0, 1);
            game.build(0, 3);
        }
        int[] housing = {6, 8, 9, 11, 13, 14, 16, 18};
        for (int level = 0; level < 4; level++) {
            for (int index = 0; index < housing.length; index++) { game.build(0, housing[index]); }
            game.build(0, 19);
        }
        check(game.getHouses() == 0, "All houses can be exhausted");
        check(!game.sellBuilding(0, 1), "Hotel downgrade blocked without four bank houses");
        check(game.sellGroup(0, 1), "Bulk hotel liquidation works with empty house stock");
        check(game.getHouses() == 0 && game.getHotels() == 12, "Bulk hotels do not create houses");
        invariants(game, "House shortage");
        game = game(new int[0]);
        cash(game, 0, 100000);
        int[] streets = {1, 3, 6, 8, 9, 11, 13, 14, 16, 18, 19, 21, 23, 24};
        own(game, 0, streets);
        int[] groupStarts = {0, 2, 5, 8, 11, 14};
        for (int group = 0; group < groupStarts.length - 1; group++) {
            for (int level = 0; level < 5; level++) {
                for (int index = groupStarts[group]; index < groupStarts[group + 1]; index++) {
                    game.build(0, streets[index]);
                }
            }
        }
        check(game.getHotels() == 0, "All twelve hotels can be exhausted");
        check(!game.build(0, 23) && game.getBoard().get(23).getLevel() == 4,
                "Hotel shortage blocks conversion without consuming houses");
        invariants(game, "Hotel shortage");
    }

    private static void tradesAndCards() {
        MonopolyGame game = game(new int[0]);
        own(game, 0, new int[] {1, 3});
        own(game, 1, new int[] {5});
        game.mortgage(1, 5);
        check(game.trade(0, 1, 1, 5, 100, 20), "Deeds and cash trade");
        check(game.getBoard().get(1).getOwner() == 1 && game.getBoard().get(5).getOwner() == 0,
                "Trade transfers both deeds");
        check(game.getPlayer(0).getCash() == 1410 && game.getPlayer(1).getCash() == 1680,
                "Trade charges recipient mortgage interest");
        check(game.getBoard().get(5).isMortgaged(), "Trade preserves mortgage");
        int[] invalidDeeds = {-1, 41, 39};
        for (int index = 0; index < invalidDeeds.length; index++) {
            check(!game.trade(0, 1, invalidDeeds[index], 40, 10, 0), "Invalid offered deed " + invalidDeeds[index]);
        }
        check(!game.trade(0, 0, 40, 40, 0, 0), "Reject self trade");
        check(!game.trade(0, 1, 40, 40, -1, 0), "Reject negative cash");
        check(!game.trade(0, 1, 40, 40, 2000, 0), "Reject unfunded cash");
        check(game.getPlayer(0).getCash() == 1410 && game.getBoard().get(5).getOwner() == 0,
                "Rejected trades are atomic");
        cash(game, 1, 0);
        check(!game.trade(0, 1, 5, 40, 0, 0), "Recipient must fund mortgage fee");
        check(game.getBoard().get(5).getOwner() == 0 && game.getPlayer(1).getCash() == 0,
                "Rejected mortgage transfer leaves both players unchanged");
        check(game.trade(0, 1, 5, 40, 10, 0), "Incoming trade cash can cover interest");
        check(game.getPlayer(1).getCash() == 0, "Incoming cash pays exact fee");
        hold(game, 0, true);
        hold(game, 0, false);
        int sellerCash = game.getPlayer(0).getCash();
        cash(game, 1, 100);
        check(game.sellJailCard(0, 1, true, 25), "Sell Chance jail card");
        check(!game.getPlayer(0).hasChanceCard() && game.getPlayer(1).hasChanceCard(), "Card sale transfers held flag");
        check(game.getPlayer(0).getCash() == sellerCash + 25 && game.getPlayer(1).getCash() == 75,
                "Card sale transfers cash");
        check(game.getChance().size() == 15, "Card sale leaves deck unchanged");
        check(!game.sellJailCard(0, 1, true, 1) && !game.sellJailCard(0, 0, false, 1)
                && !game.sellJailCard(0, 1, false, -1) && !game.sellJailCard(0, 1, false, 76),
                "Reject unavailable self negative and unfunded card sales");
        check(game.sellJailCard(0, 1, false, 0), "Gift Chest jail card");
        game.releaseCard(1);
        check(game.getChance().size() == 16 && game.getPlayer(1).hasChestCard(), "Release Chance first");
        game.releaseCard(1);
        check(game.getChest().size() == 16 && !game.getPlayer(1).hasChestCard(), "Release Chest card");
        invariants(game, "Trades and held cards");
        game = game(new int[0]);
        own(game, 0, new int[] {1, 3});
        game.build(0, 1);
        check(!game.trade(0, 1, 3, 40, 0, 0), "Buildings on sibling block deed trade");
        game.getPlayer(1).setBankrupt(true);
        check(!game.trade(0, 1, 40, 40, 1, 0), "Bankrupt partner cannot trade");
    }

    private static void movementAndJail() {
        MonopolyGame game = game(new int[0]);
        game.getPlayer(0).setPosition(39);
        game.move(0, 1);
        check(game.getPlayer(0).getPosition() == 0 && game.getPlayer(0).getCash() == 1700, "Landing GO salary once");
        game.land(0, 1, 0);
        check(game.getPlayer(0).getCash() == 1700, "GO landing does not duplicate salary");
        game.move(0, -3);
        check(game.getPlayer(0).getPosition() == 37 && game.getPlayer(0).getCash() == 1700, "Backward wrap no salary");
        game.getPlayer(0).setPosition(20);
        game.land(0, 7, 0);
        check(game.getPlayer(0).getCash() == 1700, "Free Parking pays nothing");
        game.getPlayer(0).setPosition(30);
        game.land(0, 7, 0);
        check(game.getPlayer(0).isJailed() && game.getPlayer(0).getPosition() == 10
                && game.getPlayer(0).getCash() == 1700, "Go To Jail no salary");
        game = game(new int[0]);
        own(game, 0, new int[] {2, 6, 12});
        check(game.processRoll(0, 1, 1), "First doubles permits another roll");
        check(game.processRoll(0, 2, 2), "Second doubles permits another roll");
        check(!game.processRoll(0, 3, 3) && game.getPlayer(0).isJailed()
                && game.getPlayer(0).getPosition() == 10, "Third doubles sends directly to jail");
        game = game(new int[] {1, 2, 2, 3, 3, 4});
        own(game, 0, new int[] {17});
        game.sendToJail(0);
        game.jailTurn(0);
        check(game.getPlayer(0).isJailed() && game.getPlayer(0).getJailTurns() == 1, "First jail failure");
        game.jailTurn(0);
        check(game.getPlayer(0).isJailed() && game.getPlayer(0).getJailTurns() == 2, "Second jail failure");
        game.jailTurn(0);
        check(!game.getPlayer(0).isJailed() && game.getPlayer(0).getPosition() == 17
                && game.getPlayer(0).getJailTurns() == 0 && game.getPlayer(0).getCash() == 1450,
                "Third failure pays fifty and moves");
        game = game(new int[] {2, 2, 6, 6});
        own(game, 0, new int[] {14});
        game.sendToJail(0);
        game.jailTurn(0);
        check(!game.getPlayer(0).isJailed() && game.getPlayer(0).getPosition() == 14
                && game.getPlayer(0).getCash() == 1500, "Jail doubles releases without fee or bonus roll");
        game = game(new int[] {1, 2});
        own(game, 0, new int[] {13});
        hold(game, 0, true);
        game.sendToJail(0);
        game.jailTurn(0);
        check(!game.getPlayer(0).isJailed() && !game.getPlayer(0).hasChanceCard()
                && game.getChance().size() == 16 && game.getPlayer(0).getPosition() == 13,
                "Held card releases and returns to deck");
        Dice scripted = new Dice(new int[] {6, 1});
        Dice fallback = new Dice(new int[0]);
        check(scripted.roll() == 6 && scripted.roll() == 1, "Scripted dice order");
        for (int roll = 0; roll < 12; roll++) {
            check(scripted.roll() == fallback.roll(), "Seeded fallback die " + roll);
        }
    }

    private static void cardEffects() {
        int[] destinations = {0, 24, 11, 5, 39, 15, 15, 12, 4, 10};
        for (int card = 0; card < 10; card++) {
            MonopolyGame game = game(new int[] {3, 4});
            own(game, 0, new int[] {24, 11, 5, 39});
            own(game, 1, new int[] {15, 12});
            game.getPlayer(0).setPosition(7);
            game.applyCard(0, true, card);
            check(game.getPlayer(0).getPosition() == destinations[card], "Chance movement card " + card);
            int expectedCash = card == 0 || card == 3 ? 1700
                    : card == 5 || card == 6 ? 1450 : card == 7 ? 1430 : card == 8 ? 1300 : 1500;
            check(game.getPlayer(0).getCash() == expectedCash, "Chance movement cash " + card);
            check(game.getPlayer(0).isJailed() == (card == 9), "Chance jail state " + card);
        }
        int[] chanceAmounts = {50, 150, -15, -250, -100};
        for (int card = 11; card < 16; card++) {
            MonopolyGame game = repairGame();
            game.applyCard(0, true, card);
            check(game.getPlayer(0).getCash() == 10000 + chanceAmounts[card - 11], "Chance cash card " + card);
            if (card == 15) {
                check(game.getPlayer(1).getCash() == 1550 && game.getPlayer(2).getCash() == 1550,
                        "Chairman pays each active opponent");
            }
        }
        int[] chestAmounts = {200, 0, 0, 200, -50, 50, 100, 20, 100, 100, -100, -150, 25, -310, 10, 100};
        for (int card = 0; card < 16; card++) {
            MonopolyGame game = repairGame();
            game.getPlayer(0).setPosition(33);
            if (card == 2) { hold(game, 0, false); }
            else { game.applyCard(0, false, card); }
            check(game.getPlayer(0).getCash() == 10000 + chestAmounts[card], "Chest cash card " + card);
            check(game.getPlayer(0).getPosition() == (card == 0 ? 0 : card == 1 ? 10 : 33),
                    "Chest position card " + card);
            check(game.getPlayer(0).isJailed() == (card == 1), "Chest jail state " + card);
            if (card == 8) {
                check(game.getPlayer(1).getCash() == 1450 && game.getPlayer(2).getCash() == 1450,
                        "Opera collects from each opponent");
            }
        }
        MonopolyGame game = game(new int[0]);
        hold(game, 0, true);
        check(game.getPlayer(0).hasChanceCard() && game.getChance().size() == 15, "Chance ten held outside deck");
        game.releaseCard(0);
        game.getChance().returnCard(10);
        check(game.getChance().size() == 16, "Duplicate card return ignored");
    }

    private static MonopolyGame repairGame() {
        MonopolyGame game = game(new int[0]);
        cash(game, 0, 20000);
        own(game, 0, new int[] {1, 3, 6, 8, 9});
        for (int level = 0; level < 5; level++) {
            game.build(0, 1);
            game.build(0, 3);
        }
        game.build(0, 6);
        game.build(0, 8);
        cash(game, 0, 10000);
        return game;
    }

    private static void bankruptcyAndAuctions() {
        MonopolyGame game = game(new int[0]);
        cash(game, 0, 10);
        check(game.pay(0, 1, 7) && game.getPlayer(0).getCash() == 3
                && game.getPlayer(1).getCash() == 1507, "Payment credits player");
        check(game.pay(0, -1, 3) && game.getPlayer(0).getCash() == 0, "Payment to bank");
        check(!game.pay(0, 1, 1) && game.getPlayer(0).isBankrupt(), "Player creditor bankruptcy");
        check(!game.pay(0, -1, 0), "Bankrupt player cannot pay");
        game = game(new int[0]);
        own(game, 0, new int[] {1, 3, 5});
        game.build(0, 1);
        game.build(0, 3);
        game.mortgage(0, 5);
        hold(game, 0, true);
        cash(game, 0, 20);
        check(!game.pay(0, 1, 10000), "Debt liquidates buildings before player bankruptcy");
        check(game.getBoard().get(1).getOwner() == 1 && game.getBoard().get(3).getOwner() == 1
                && game.getBoard().get(5).getOwner() == 1, "Creditor inherits every deed");
        check(game.getPlayer(1).getCash() == 1614, "Creditor receives liquidation cash and pays inherited fees");
        check(game.getPlayer(1).hasChanceCard() && !game.getPlayer(0).hasChanceCard(),
                "Player creditor inherits held jail card");
        invariants(game, "Player creditor assets");
        game = game(new int[0]);
        own(game, 0, new int[] {1});
        game.mortgage(0, 1);
        hold(game, 0, true);
        cash(game, 0, 0);
        cash(game, 1, 0);
        check(!game.pay(0, 1, 1000), "Unpayable inherited mortgage debt");
        check(game.getPlayer(0).isBankrupt() && game.getPlayer(1).isBankrupt(),
                "Inherited interest can bankrupt creditor while third player remains");
        check(game.getChance().size() == 16 && !game.getPlayer(1).hasChanceCard(), "Chained bankruptcy returns held card");
        invariants(game, "Inherited fee bankruptcy");
        game = interactive("0\n0\n0\n");
        own(game, 0, new int[] {1});
        game.mortgage(0, 1);
        hold(game, 0, false);
        cash(game, 0, 0);
        check(!game.pay(0, -1, 1000), "Bank creditor bankruptcy");
        check(game.getBoard().get(1).getOwner() == -1 && !game.getBoard().get(1).isMortgaged(),
                "Bank clears mortgage and auctions deed with all passing");
        check(game.getChest().size() == 16 && game.getPlayer(0).getCash() == 0, "Bank bankruptcy returns card and clears cash");
        game = interactive("10\n20\n0\n0\n");
        game.auction(1);
        check(game.getBoard().get(1).getOwner() == 1 && game.getPlayer(1).getCash() == 1480,
                "Auction high bidder pays winning bid");
        check(game.getPlayer(0).getCash() == 1500, "Losing auction bid costs nothing");
        game = interactive("10\n5\n11\n0\n0\n");
        game.auction(3);
        check(game.getBoard().get(3).getOwner() == 1 && game.getPlayer(1).getCash() == 1489,
                "Auction rejects bid below current price and accepts correction");
        game = interactive("0\n0\n0\n");
        game.auction(5);
        check(game.getBoard().get(5).getOwner() == -1, "All pass leaves deed in bank");
        game = interactive("");
        cash(game, 0, 0);
        check(!game.pay(0, -1, 100) && !game.getPlayer(0).isBankrupt(), "EOF stops debt handling without bankruptcy");
    }

    private static void smokeGames() {
        int[] seeds = {1, 9, 42, 99, 2026};
        for (int index = 0; index < seeds.length; index++) {
            MonopolyGame game = new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                    new ConsoleInput(new Scanner(""), true, true),
                    new Dice(new Random(seeds[index])), new Random(seeds[index]));
            for (int cap = 25; cap <= 1000 && game.activeCount() > 1; cap += 25) {
                game.play(cap);
                check(game.getTurns() <= cap, "Seed " + seeds[index] + " respects turn cap");
                invariants(game, "Seed " + seeds[index] + " through " + game.getTurns());
            }
            check(game.getTurns() > 0, "Seed " + seeds[index] + " plays turns");
            check(game.activeCount() == 1 || game.getTurns() == 1000, "Seed completes or reaches cap " + seeds[index]);
            check(game.result().contains(game.activeCount() == 1 ? "Winner:" : "No winner declared"),
                    "Result matches ending " + seeds[index]);
            System.out.println("Seed " + seeds[index] + ": " + game.result());
        }
        int[] fullGameSeeds = {1, 2, 3, 7, 9, 17, 42, 99, 123, 456, 1000, 2026};
        for (int index = 0; index < fullGameSeeds.length; index++) {
            int seed = fullGameSeeds[index];
            MonopolyGame game = new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                    new ConsoleInput(new Scanner(""), true, true), new Dice(new Random(seed)), new Random(seed));
            game.play(20000);
            check(game.getTurns() > 0 && game.getTurns() <= 20000, "Full game respects cap seed " + seed);
            check(game.activeCount() == 1, "Uninterrupted full game winner seed " + seed);
            invariants(game, "Full game seed " + seed);
            System.out.println("Full seed " + seed + ": " + game.result());
        }
    }

    private static MonopolyGame seededGame(int seed) {
        return new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                new ConsoleInput(new Scanner(""), true, true),
                new Dice(new Random(seed)), new Random(seed));
    }

    private static void regressionChecks() {
        MonopolyGame game = game(new int[0]);
        Deck chest = game.getChest();
        int[] chestOrder = new int[16];
        for (int index = 0; index < chestOrder.length; index++) { chestOrder[index] = chest.draw(); }
        chest.returnCard(3);
        int nextChestCard = -1;
        for (int index = 0; index < chestOrder.length; index++) {
            if (chestOrder[index] != 3) {
                if (nextChestCard == -1) { nextChestCard = chestOrder[index]; }
                chest.returnCard(chestOrder[index]);
            }
        }
        game.getPlayer(0).setPosition(36);
        game.applyCard(0, true, 8);
        check(game.getPlayer(0).getPosition() == 33 && game.getPlayer(0).getCash() == 1700,
                "Chance back three from 36 resolves Chest bank error at 33");
        check(chest.size() == 16, "Nested Chest effect returns drawn card");
        int nextDraw = chest.draw();
        check(nextDraw == nextChestCard, "Nested Chest effect consumes top card and rotates deck");
        chest.returnCard(nextDraw);
        invariants(game, "Nested card regression");

        game = game(new int[0]);
        game.getPlayer(0).setPosition(10);
        game.land(0, 7, 0);
        check(!game.getPlayer(0).isJailed() && game.getPlayer(0).getPosition() == 10,
                "Direct landing on Jail is Just Visiting");
        game.getPlayer(0).setPosition(24);
        check(!game.processRoll(0, 3, 3) && game.getPlayer(0).isJailed()
                && game.getPlayer(0).getPosition() == 10,
                "Doubles landing on Go To Jail ends turn without bonus");

        game = new MonopolyGame(new String[] {"Ada", "Grace", "James"},
                new ConsoleInput(new Scanner("1\n0\n"), false, true),
                new Dice(new int[] {2, 2, 1, 2}), new Random(42));
        own(game, 0, new int[] {14});
        game.sendToJail(0);
        game.takeTurn(0);
        check(!game.getPlayer(0).isJailed() && game.getPlayer(0).getPosition() == 17,
                "Pay before rolling permits doubles bonus roll after release");
        check(game.getPlayer(0).getCash() == 1450, "Paid jail release charges exactly fifty");

        game = game(new int[0]);
        own(game, 0, new int[] {5});
        game.mortgage(0, 5);
        check(game.trade(0, 1, 5, 40, 0, 0), "Acquire mortgaged railroad by trade");
        check(game.getPlayer(1).getCash() == 1490 && game.getBoard().get(5).isMortgaged(),
                "Acquisition pays ten interest and preserves mortgage principal");
        check(game.unmortgage(1, 5) && game.getPlayer(1).getCash() == 1380
                && !game.getBoard().get(5).isMortgaged(),
                "Later redemption charges another 110 after transfer fee");

        game = interactive("0\n10\n0\n0\n");
        game.getPlayer(0).setPosition(1);
        game.land(0, 7, 0);
        check(game.getBoard().get(1).getOwner() == 0 && game.getPlayer(0).getCash() == 1490,
                "Player declining list price can win landing auction");

        int[] seeds = {9, 42, 123, 2026};
        for (int index = 0; index < seeds.length; index++) {
            MonopolyGame uninterrupted = seededGame(seeds[index]);
            MonopolyGame resumed = seededGame(seeds[index]);
            uninterrupted.play(20000);
            for (int cap = 1; cap <= 20000 && resumed.activeCount() > 1; cap++) { resumed.play(cap); }
            boolean samePlayers = true;
            for (int player = 0; player < 3; player++) {
                Player expected = uninterrupted.getPlayer(player);
                Player actual = resumed.getPlayer(player);
                samePlayers = samePlayers && expected.getCash() == actual.getCash()
                        && expected.getPosition() == actual.getPosition()
                        && expected.isJailed() == actual.isJailed()
                        && expected.getJailTurns() == actual.getJailTurns()
                        && expected.isBankrupt() == actual.isBankrupt()
                        && expected.hasChanceCard() == actual.hasChanceCard()
                        && expected.hasChestCard() == actual.hasChestCard();
            }
            check(samePlayers && uninterrupted.result().equals(resumed.result()),
                    "Resumed player state and final result equal uninterrupted seed " + seeds[index]);
            boolean sameBoard = true;
            for (int position = 0; position < 40; position++) {
                Space expected = uninterrupted.getBoard().get(position);
                Space actual = resumed.getBoard().get(position);
                sameBoard = sameBoard && expected.getOwner() == actual.getOwner()
                        && expected.getLevel() == actual.getLevel()
                        && expected.isMortgaged() == actual.isMortgaged();
            }
            check(sameBoard && uninterrupted.getHouses() == resumed.getHouses()
                    && uninterrupted.getHotels() == resumed.getHotels(),
                    "Resumed board and stock equal uninterrupted seed " + seeds[index]);
            boolean sameDecks = uninterrupted.getChance().size() == resumed.getChance().size()
                    && uninterrupted.getChest().size() == resumed.getChest().size();
            int chanceSize = uninterrupted.getChance().size();
            int chestSize = uninterrupted.getChest().size();
            for (int card = 0; card < chanceSize; card++) {
                int expected = uninterrupted.getChance().draw();
                int actual = resumed.getChance().draw();
                sameDecks = sameDecks && expected == actual;
                uninterrupted.getChance().returnCard(expected);
                resumed.getChance().returnCard(actual);
            }
            for (int card = 0; card < chestSize; card++) {
                int expected = uninterrupted.getChest().draw();
                int actual = resumed.getChest().draw();
                sameDecks = sameDecks && expected == actual;
                uninterrupted.getChest().returnCard(expected);
                resumed.getChest().returnCard(actual);
            }
            check(sameDecks, "Resumed deck sizes and full order equal uninterrupted seed " + seeds[index]);
            invariants(resumed, "Resumed regression seed " + seeds[index]);
        }
    }

    public static void main(String[] arguments) {
        boardAndRent();
        buildingsAndMortgages();
        tradesAndCards();
        movementAndJail();
        cardEffects();
        bankruptcyAndAuctions();
        regressionChecks();
        smokeGames();
        System.out.println(checks + " checks, " + failures + " failures.");
        if (failures > 0) { System.exit(1); }
    }
}
