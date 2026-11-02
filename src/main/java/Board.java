public class Board {
    private Space[] spaces;

    public Board() {
        spaces = new Space[40];
        addOther(0, "GO", Space.GO, 0);
        addStreet(1, "Mediterranean Avenue", 60, 0, 50, 2, 10, 30, 90, 160, 250);
        addOther(2, "Community Chest", Space.CHEST, 0);
        addStreet(3, "Baltic Avenue", 60, 0, 50, 4, 20, 60, 180, 320, 450);
        addOther(4, "Income Tax", Space.TAX, 200);
        addRailroad(5, "Reading Railroad");
        addStreet(6, "Oriental Avenue", 100, 1, 50, 6, 30, 90, 270, 400, 550);
        addOther(7, "Chance", Space.CHANCE, 0);
        addStreet(8, "Vermont Avenue", 100, 1, 50, 6, 30, 90, 270, 400, 550);
        addStreet(9, "Connecticut Avenue", 120, 1, 50, 8, 40, 100, 300, 450, 600);
        addOther(10, "Jail / Just Visiting", Space.JAIL, 0);
        addStreet(11, "St. Charles Place", 140, 2, 100, 10, 50, 150, 450, 625, 750);
        addUtility(12, "Electric Company");
        addStreet(13, "States Avenue", 140, 2, 100, 10, 50, 150, 450, 625, 750);
        addStreet(14, "Virginia Avenue", 160, 2, 100, 12, 60, 180, 500, 700, 900);
        addRailroad(15, "Pennsylvania Railroad");
        addStreet(16, "St. James Place", 180, 3, 100, 14, 70, 200, 550, 750, 950);
        addOther(17, "Community Chest", Space.CHEST, 0);
        addStreet(18, "Tennessee Avenue", 180, 3, 100, 14, 70, 200, 550, 750, 950);
        addStreet(19, "New York Avenue", 200, 3, 100, 16, 80, 220, 600, 800, 1000);
        addOther(20, "Free Parking", Space.FREE, 0);
        addStreet(21, "Kentucky Avenue", 220, 4, 150, 18, 90, 250, 700, 875, 1050);
        addOther(22, "Chance", Space.CHANCE, 0);
        addStreet(23, "Indiana Avenue", 220, 4, 150, 18, 90, 250, 700, 875, 1050);
        addStreet(24, "Illinois Avenue", 240, 4, 150, 20, 100, 300, 750, 925, 1100);
        addRailroad(25, "B. & O. Railroad");
        addStreet(26, "Atlantic Avenue", 260, 5, 150, 22, 110, 330, 800, 975, 1150);
        addStreet(27, "Ventnor Avenue", 260, 5, 150, 22, 110, 330, 800, 975, 1150);
        addUtility(28, "Water Works");
        addStreet(29, "Marvin Gardens", 280, 5, 150, 24, 120, 360, 850, 1025, 1200);
        addOther(30, "Go To Jail", Space.GO_TO_JAIL, 0);
        addStreet(31, "Pacific Avenue", 300, 6, 200, 26, 130, 390, 900, 1100, 1275);
        addStreet(32, "North Carolina Avenue", 300, 6, 200, 26, 130, 390, 900, 1100, 1275);
        addOther(33, "Community Chest", Space.CHEST, 0);
        addStreet(34, "Pennsylvania Avenue", 320, 6, 200, 28, 150, 450, 1000, 1200, 1400);
        addRailroad(35, "Short Line");
        addOther(36, "Chance", Space.CHANCE, 0);
        addStreet(37, "Park Place", 350, 7, 200, 35, 175, 500, 1100, 1300, 1500);
        addOther(38, "Luxury Tax", Space.TAX, 100);
        addStreet(39, "Boardwalk", 400, 7, 200, 50, 200, 600, 1400, 1700, 2000);
    }

    private void addStreet(int index, String name, int price, int group,
                           int cost, int r0, int r1, int r2, int r3, int r4, int r5) {
        int[] rents = {r0, r1, r2, r3, r4, r5};
        spaces[index] = new Space(name, Space.STREET, price, group, cost, rents);
    }

    private void addOther(int index, String name, int type, int price) {
        spaces[index] = new Space(name, type, price, -1, 0, new int[6]);
    }

    private void addRailroad(int index, String name) {
        int[] rents = {25, 50, 100, 200, 0, 0};
        spaces[index] = new Space(name, Space.RAILROAD, 200, -1, 0, rents);
    }

    private void addUtility(int index, String name) {
        int[] rents = {4, 10, 0, 0, 0, 0};
        spaces[index] = new Space(name, Space.UTILITY, 150, -1, 0, rents);
    }

    public Space get(int index) { return spaces[index]; }
    public int size() { return spaces.length; }
}
