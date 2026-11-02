public class Space {
    public static final int GO = 0;
    public static final int STREET = 1;
    public static final int RAILROAD = 2;
    public static final int UTILITY = 3;
    public static final int TAX = 4;
    public static final int CHANCE = 5;
    public static final int CHEST = 6;
    public static final int JAIL = 7;
    public static final int FREE = 8;
    public static final int GO_TO_JAIL = 9;

    private String name;
    private int type;
    private int price;
    private int group;
    private int buildingCost;
    private int[] rents;
    private int owner;
    private int level;
    private boolean mortgaged;

    public Space(String name, int type, int price, int group,
                 int buildingCost, int[] rents) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.group = group;
        this.buildingCost = buildingCost;
        this.rents = new int[6];
        for (int index = 0; index < 6; index++) {
            this.rents[index] = rents[index];
        }
        owner = -1;
        level = 0;
        mortgaged = false;
    }

    public String getName() { return name; }
    public int getType() { return type; }
    public int getPrice() { return price; }
    public int getGroup() { return group; }
    public int getBuildingCost() { return buildingCost; }
    public int getRent(int level) { return rents[level]; }
    public int getOwner() { return owner; }
    public void setOwner(int owner) { this.owner = owner; }
    public int getLevel() { return level; }
    public void setLevel(int level) {
        if (level >= 0 && level <= 5) {
            this.level = level;
        }
    }
    public boolean isMortgaged() { return mortgaged; }
    public void setMortgaged(boolean mortgaged) { this.mortgaged = mortgaged; }
    public int getMortgage() { return price / 2; }
}
