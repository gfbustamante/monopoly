public class Player {
    private String name;
    private int cash;
    private int position;
    private boolean jailed;
    private int jailTurns;
    private boolean bankrupt;
    private boolean chanceCard;
    private boolean chestCard;

    public Player(String name) {
        this.name = name;
        cash = 1500;
    }

    public String getName() { return name; }
    public int getCash() { return cash; }
    public int getPosition() { return position; }
    public boolean isJailed() { return jailed; }
    public int getJailTurns() { return jailTurns; }
    public boolean isBankrupt() { return bankrupt; }
    public boolean hasChanceCard() { return chanceCard; }
    public boolean hasChestCard() { return chestCard; }
    public void changeCash(int amount) { cash += amount; }
    public void setPosition(int position) { this.position = position; }
    public void setJailed(boolean jailed) { this.jailed = jailed; }
    public void setJailTurns(int turns) { jailTurns = turns; }
    public void setBankrupt(boolean bankrupt) { this.bankrupt = bankrupt; }
    public void setChanceCard(boolean held) { chanceCard = held; }
    public void setChestCard(boolean held) { chestCard = held; }
}
