package net.enelson.soppillars.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArenaSettings {

    private int countdownSeconds;
    private int cageSeconds;
    private int preBorderDelaySeconds;
    private int borderShrinkSeconds;
    private double endBorderDiameter;
    private boolean lavaEnabled;
    private int lavaStartDelaySeconds;
    private int lavaRiseIntervalSeconds;
    private int postShrinkEndDelaySeconds;
    private int minPlayers;
    private int minFilledTeams;
    private boolean allowPlaceBlocks;
    private boolean allowBreakOriginalBlocks;
    private boolean allowBreakPlayerBlocks;
    private boolean allowSmoothFall;
    private int smoothFallSeconds;
    private boolean friendlyFire;
    private boolean blacklistMode;
    private boolean allowEnchantedBooks;
    private boolean allowPotions;
    private boolean allowTippedArrows;
    private boolean allowSpawnEggs;
    private List<String> allowedCommands;
    private boolean lootEnabled;
    private int lootIntervalSeconds;
    private List<String> lootWhitelist;
    private List<String> lootBlacklist;
    private int celebrationSeconds;
    private VictoryEffectShape victoryEffectShape;
    private double victoryEffectRadius;
    private int victoryEffectIntervalTicks;
    private double victoryEffectSpawnHeight;
    private int victoryEffectAmountPerWave;
    private List<String> victoryCommands;

    public static ArenaSettings defaults(int countdownSeconds,
                                         int cageSeconds,
                                         int preBorderDelaySeconds,
                                         int borderShrinkSeconds,
                                         double endBorderDiameter,
                                         boolean lavaEnabled,
                                         int lavaStartDelaySeconds,
                                         int lavaRiseIntervalSeconds,
                                         int postShrinkEndDelaySeconds,
                                         int minPlayers,
                                         int minFilledTeams,
                                         boolean allowPlaceBlocks,
                                         boolean allowBreakOriginalBlocks,
                                         boolean allowBreakPlayerBlocks,
                                         boolean allowSmoothFall,
                                         boolean friendlyFire,
                                         List<String> allowedCommands) {
        ArenaSettings settings = new ArenaSettings();
        settings.countdownSeconds = countdownSeconds;
        settings.cageSeconds = cageSeconds;
        settings.preBorderDelaySeconds = preBorderDelaySeconds;
        settings.borderShrinkSeconds = borderShrinkSeconds;
        settings.endBorderDiameter = endBorderDiameter;
        settings.lavaEnabled = lavaEnabled;
        settings.lavaStartDelaySeconds = lavaStartDelaySeconds;
        settings.lavaRiseIntervalSeconds = lavaRiseIntervalSeconds;
        settings.postShrinkEndDelaySeconds = postShrinkEndDelaySeconds;
        settings.minPlayers = minPlayers;
        settings.minFilledTeams = minFilledTeams;
        settings.allowPlaceBlocks = allowPlaceBlocks;
        settings.allowBreakOriginalBlocks = allowBreakOriginalBlocks;
        settings.allowBreakPlayerBlocks = allowBreakPlayerBlocks;
        settings.allowSmoothFall = allowSmoothFall;
        settings.smoothFallSeconds = 10;
        settings.friendlyFire = friendlyFire;
        settings.blacklistMode = false;
        settings.allowEnchantedBooks = false;
        settings.allowPotions = true;
        settings.allowTippedArrows = true;
        settings.allowSpawnEggs = false;
        settings.allowedCommands = new ArrayList<String>(allowedCommands);
        settings.lootEnabled = true;
        settings.lootIntervalSeconds = 8;
        settings.lootWhitelist = new ArrayList<String>();
        settings.lootBlacklist = new ArrayList<String>();
        settings.celebrationSeconds = 10;
        settings.victoryEffectShape = VictoryEffectShape.SQUARE;
        settings.victoryEffectRadius = 8.0D;
        settings.victoryEffectIntervalTicks = 20;
        settings.victoryEffectSpawnHeight = 14.0D;
        settings.victoryEffectAmountPerWave = 2;
        settings.victoryCommands = new ArrayList<String>();
        return settings;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public int getCageSeconds() {
        return cageSeconds;
    }

    public void setCageSeconds(int cageSeconds) {
        this.cageSeconds = cageSeconds;
    }

    public int getPreBorderDelaySeconds() {
        return preBorderDelaySeconds;
    }

    public void setPreBorderDelaySeconds(int preBorderDelaySeconds) {
        this.preBorderDelaySeconds = preBorderDelaySeconds;
    }

    public int getBorderShrinkSeconds() {
        return borderShrinkSeconds;
    }

    public void setBorderShrinkSeconds(int borderShrinkSeconds) {
        this.borderShrinkSeconds = borderShrinkSeconds;
    }

    public double getEndBorderDiameter() {
        return endBorderDiameter;
    }

    public void setEndBorderDiameter(double endBorderDiameter) {
        this.endBorderDiameter = endBorderDiameter;
    }

    public boolean isLavaEnabled() {
        return lavaEnabled;
    }

    public void setLavaEnabled(boolean lavaEnabled) {
        this.lavaEnabled = lavaEnabled;
    }

    public int getLavaStartDelaySeconds() {
        return lavaStartDelaySeconds;
    }

    public void setLavaStartDelaySeconds(int lavaStartDelaySeconds) {
        this.lavaStartDelaySeconds = lavaStartDelaySeconds;
    }

    public int getLavaRiseIntervalSeconds() {
        return lavaRiseIntervalSeconds;
    }

    public void setLavaRiseIntervalSeconds(int lavaRiseIntervalSeconds) {
        this.lavaRiseIntervalSeconds = lavaRiseIntervalSeconds;
    }

    public int getPostShrinkEndDelaySeconds() {
        return postShrinkEndDelaySeconds;
    }

    public void setPostShrinkEndDelaySeconds(int postShrinkEndDelaySeconds) {
        this.postShrinkEndDelaySeconds = postShrinkEndDelaySeconds;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMinFilledTeams() {
        return minFilledTeams;
    }

    public void setMinFilledTeams(int minFilledTeams) {
        this.minFilledTeams = minFilledTeams;
    }

    public boolean isAllowPlaceBlocks() {
        return allowPlaceBlocks;
    }

    public void setAllowPlaceBlocks(boolean allowPlaceBlocks) {
        this.allowPlaceBlocks = allowPlaceBlocks;
    }

    public boolean isAllowBreakOriginalBlocks() {
        return allowBreakOriginalBlocks;
    }

    public void setAllowBreakOriginalBlocks(boolean allowBreakOriginalBlocks) {
        this.allowBreakOriginalBlocks = allowBreakOriginalBlocks;
    }

    public boolean isAllowBreakPlayerBlocks() {
        return allowBreakPlayerBlocks;
    }

    public void setAllowBreakPlayerBlocks(boolean allowBreakPlayerBlocks) {
        this.allowBreakPlayerBlocks = allowBreakPlayerBlocks;
    }

    public boolean isAllowSmoothFall() {
        return allowSmoothFall;
    }

    public void setAllowSmoothFall(boolean allowSmoothFall) {
        this.allowSmoothFall = allowSmoothFall;
    }

    public int getSmoothFallSeconds() {
        return smoothFallSeconds;
    }

    public void setSmoothFallSeconds(int smoothFallSeconds) {
        this.smoothFallSeconds = smoothFallSeconds;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public boolean isBlacklistMode() {
        return blacklistMode;
    }

    public void setBlacklistMode(boolean blacklistMode) {
        this.blacklistMode = blacklistMode;
    }

    public boolean isAllowEnchantedBooks() {
        return allowEnchantedBooks;
    }

    public void setAllowEnchantedBooks(boolean allowEnchantedBooks) {
        this.allowEnchantedBooks = allowEnchantedBooks;
    }

    public boolean isAllowPotions() {
        return allowPotions;
    }

    public void setAllowPotions(boolean allowPotions) {
        this.allowPotions = allowPotions;
    }

    public boolean isAllowTippedArrows() {
        return allowTippedArrows;
    }

    public void setAllowTippedArrows(boolean allowTippedArrows) {
        this.allowTippedArrows = allowTippedArrows;
    }

    public boolean isAllowSpawnEggs() {
        return allowSpawnEggs;
    }

    public void setAllowSpawnEggs(boolean allowSpawnEggs) {
        this.allowSpawnEggs = allowSpawnEggs;
    }

    public List<String> getAllowedCommands() {
        return Collections.unmodifiableList(allowedCommands);
    }

    public void setAllowedCommands(List<String> allowedCommands) {
        this.allowedCommands = new ArrayList<String>(allowedCommands);
    }

    public boolean isLootEnabled() {
        return lootEnabled;
    }

    public void setLootEnabled(boolean lootEnabled) {
        this.lootEnabled = lootEnabled;
    }

    public int getLootIntervalSeconds() {
        return lootIntervalSeconds;
    }

    public void setLootIntervalSeconds(int lootIntervalSeconds) {
        this.lootIntervalSeconds = lootIntervalSeconds;
    }

    public List<String> getLootWhitelist() {
        return lootWhitelist;
    }

    public void setLootWhitelist(List<String> lootWhitelist) {
        this.lootWhitelist = lootWhitelist == null ? new ArrayList<String>() : new ArrayList<String>(lootWhitelist);
    }

    public List<String> getLootBlacklist() {
        return lootBlacklist;
    }

    public void setLootBlacklist(List<String> lootBlacklist) {
        this.lootBlacklist = lootBlacklist == null ? new ArrayList<String>() : new ArrayList<String>(lootBlacklist);
    }

    public int getCelebrationSeconds() {
        return celebrationSeconds;
    }

    public void setCelebrationSeconds(int celebrationSeconds) {
        this.celebrationSeconds = celebrationSeconds;
    }

    public VictoryEffectShape getVictoryEffectShape() {
        return victoryEffectShape;
    }

    public void setVictoryEffectShape(VictoryEffectShape victoryEffectShape) {
        this.victoryEffectShape = victoryEffectShape == null ? VictoryEffectShape.SQUARE : victoryEffectShape;
    }

    public double getVictoryEffectRadius() {
        return victoryEffectRadius;
    }

    public void setVictoryEffectRadius(double victoryEffectRadius) {
        this.victoryEffectRadius = victoryEffectRadius;
    }

    public int getVictoryEffectIntervalTicks() {
        return victoryEffectIntervalTicks;
    }

    public void setVictoryEffectIntervalTicks(int victoryEffectIntervalTicks) {
        this.victoryEffectIntervalTicks = victoryEffectIntervalTicks;
    }

    public double getVictoryEffectSpawnHeight() {
        return victoryEffectSpawnHeight;
    }

    public void setVictoryEffectSpawnHeight(double victoryEffectSpawnHeight) {
        this.victoryEffectSpawnHeight = victoryEffectSpawnHeight;
    }

    public int getVictoryEffectAmountPerWave() {
        return victoryEffectAmountPerWave;
    }

    public void setVictoryEffectAmountPerWave(int victoryEffectAmountPerWave) {
        this.victoryEffectAmountPerWave = victoryEffectAmountPerWave;
    }

    public List<String> getVictoryCommands() {
        return victoryCommands;
    }

    public void setVictoryCommands(List<String> victoryCommands) {
        this.victoryCommands = victoryCommands == null ? new ArrayList<String>() : new ArrayList<String>(victoryCommands);
    }
}
