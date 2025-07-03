package com.anesthesia;

import java.util.*;

public class AnesthesiaCalculator {
    
    public enum SeverityLevel {
        DIFFICULT("イ"),
        NORMAL("ロ");
        
        private final String value;
        
        SeverityLevel(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum EpiduralLocation {
        CERVICAL_THORACIC("頸・胸部"),
        LUMBAR("腰部"),
        SACRAL("仙骨部");
        
        private final String value;
        
        EpiduralLocation(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public enum NerveBlockType {
        EPIDURAL_SUBSTITUTE("硬膜外代用"),
        OTHER("その他");
        
        private final String value;
        
        NerveBlockType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public static class AnesthesiaLevelTime {
        private int level;
        private int minutes;
        
        public AnesthesiaLevelTime(int level) {
            this(level, 0);
        }
        
        public AnesthesiaLevelTime(int level, int minutes) {
            this.level = level;
            this.minutes = minutes;
        }
        
        public int getLevel() { return level; }
        public int getMinutes() { return minutes; }
        public void setMinutes(int minutes) { this.minutes = minutes; }
    }
    
    public static class CalculationResult {
        private int basePoints;
        private List<TimeExtensionDetail> timeExtensionPoints;
        private int totalTimeExtension;
        
        public CalculationResult(int basePoints, List<TimeExtensionDetail> timeExtensionPoints, int totalTimeExtension) {
            this.basePoints = basePoints;
            this.timeExtensionPoints = new ArrayList<>(timeExtensionPoints);
            this.totalTimeExtension = totalTimeExtension;
        }
        
        public int getBasePoints() { return basePoints; }
        public List<TimeExtensionDetail> getTimeExtensionPoints() { return timeExtensionPoints; }
        public int getTotalTimeExtension() { return totalTimeExtension; }
    }
    
    public static class TimeExtensionDetail {
        private int level;
        private int points;
        
        public TimeExtensionDetail(int level, int points) {
            this.level = level;
            this.points = points;
        }
        
        public int getLevel() { return level; }
        public int getPoints() { return points; }
    }
    
    private List<AnesthesiaLevelTime> anesthesiaLevels;
    private SeverityLevel severityLevel;
    private boolean hasEpiduralAnesthesia;
    private EpiduralLocation epiduralLocation;
    private boolean hasNerveBlock;
    private NerveBlockType nerveBlockType;
    private boolean hasOvertimeAddition;
    private boolean hasNightHolidayAddition;
    
    private final Map<Integer, Map<SeverityLevel, Integer>> basePointsTable;
    private final Map<Integer, Integer> timeExtensionTable;
    private final Map<EpiduralLocation, Integer> epiduralPointsTable;
    private final Map<EpiduralLocation, Integer> epiduralTimeExtensionTable;
    private final Map<NerveBlockType, Integer> nerveBlockPointsTable;
    
    public AnesthesiaCalculator() {
        initializeAnesthesiaLevels();
        this.severityLevel = SeverityLevel.NORMAL;
        this.hasEpiduralAnesthesia = false;
        this.epiduralLocation = EpiduralLocation.LUMBAR;
        this.hasNerveBlock = false;
        this.nerveBlockType = NerveBlockType.OTHER;
        this.hasOvertimeAddition = false;
        this.hasNightHolidayAddition = false;
        
        basePointsTable = initializeBasePointsTable();
        timeExtensionTable = initializeTimeExtensionTable();
        epiduralPointsTable = initializeEpiduralPointsTable();
        epiduralTimeExtensionTable = initializeEpiduralTimeExtensionTable();
        nerveBlockPointsTable = initializeNerveBlockPointsTable();
    }
    
    private void initializeAnesthesiaLevels() {
        anesthesiaLevels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            anesthesiaLevels.add(new AnesthesiaLevelTime(i));
        }
    }
    
    private Map<Integer, Map<SeverityLevel, Integer>> initializeBasePointsTable() {
        Map<Integer, Map<SeverityLevel, Integer>> table = new HashMap<>();
        
        Map<SeverityLevel, Integer> level1 = new HashMap<>();
        level1.put(SeverityLevel.DIFFICULT, 24900);
        level1.put(SeverityLevel.NORMAL, 18200);
        table.put(1, level1);
        
        Map<SeverityLevel, Integer> level2 = new HashMap<>();
        level2.put(SeverityLevel.DIFFICULT, 16720);
        level2.put(SeverityLevel.NORMAL, 12190);
        table.put(2, level2);
        
        Map<SeverityLevel, Integer> level3 = new HashMap<>();
        level3.put(SeverityLevel.DIFFICULT, 12610);
        level3.put(SeverityLevel.NORMAL, 9170);
        table.put(3, level3);
        
        Map<SeverityLevel, Integer> level4 = new HashMap<>();
        level4.put(SeverityLevel.DIFFICULT, 9130);
        level4.put(SeverityLevel.NORMAL, 6610);
        table.put(4, level4);
        
        Map<SeverityLevel, Integer> level5 = new HashMap<>();
        level5.put(SeverityLevel.DIFFICULT, 8300);
        level5.put(SeverityLevel.NORMAL, 6000);
        table.put(5, level5);
        
        return table;
    }
    
    private Map<Integer, Integer> initializeTimeExtensionTable() {
        Map<Integer, Integer> table = new HashMap<>();
        table.put(1, 1800);
        table.put(2, 1200);
        table.put(3, 900);
        table.put(4, 660);
        table.put(5, 600);
        return table;
    }
    
    private Map<EpiduralLocation, Integer> initializeEpiduralPointsTable() {
        Map<EpiduralLocation, Integer> table = new HashMap<>();
        table.put(EpiduralLocation.CERVICAL_THORACIC, 750);
        table.put(EpiduralLocation.LUMBAR, 400);
        table.put(EpiduralLocation.SACRAL, 170);
        return table;
    }
    
    private Map<EpiduralLocation, Integer> initializeEpiduralTimeExtensionTable() {
        Map<EpiduralLocation, Integer> table = new HashMap<>();
        table.put(EpiduralLocation.CERVICAL_THORACIC, 375);
        table.put(EpiduralLocation.LUMBAR, 200);
        table.put(EpiduralLocation.SACRAL, 85);
        return table;
    }
    
    private Map<NerveBlockType, Integer> initializeNerveBlockPointsTable() {
        Map<NerveBlockType, Integer> table = new HashMap<>();
        table.put(NerveBlockType.EPIDURAL_SUBSTITUTE, 450);
        table.put(NerveBlockType.OTHER, 45);
        return table;
    }
    
    public List<AnesthesiaLevelTime> getActiveLevels() {
        return anesthesiaLevels.stream()
                .filter(level -> level.getMinutes() > 0)
                .sorted(Comparator.comparingInt(AnesthesiaLevelTime::getLevel))
                .toList();
    }
    
    public int getHighestLevel() {
        return getActiveLevels().stream()
                .mapToInt(AnesthesiaLevelTime::getLevel)
                .min()
                .orElse(5);
    }
    
    public int getTotalMinutes() {
        return getActiveLevels().stream()
                .mapToInt(AnesthesiaLevelTime::getMinutes)
                .sum();
    }
    
    public CalculationResult getCalculationResult() {
        List<AnesthesiaLevelTime> actives = getActiveLevels();
        
        if (actives.isEmpty()) {
            return new CalculationResult(0, new ArrayList<>(), 0);
        }
        
        int basePoints = basePointsTable.get(getHighestLevel()).get(severityLevel);
        
        int remainingBaseTime = 120;
        List<LevelTimeCalculation> levelTimes = new ArrayList<>();
        
        for (AnesthesiaLevelTime active : actives) {
            int availableTime = Math.min(remainingBaseTime, active.getMinutes());
            levelTimes.add(new LevelTimeCalculation(active.getLevel(), active.getMinutes(), availableTime, 0));
            remainingBaseTime -= availableTime;
            if (remainingBaseTime <= 0) break;
        }
        
        List<Integer> availableExtensionTimes = new ArrayList<>();
        for (LevelTimeCalculation lt : levelTimes) {
            availableExtensionTimes.add(lt.minutes - lt.baseUsed);
        }
        
        for (int i = 0; i < levelTimes.size(); i++) {
            int availableTime = availableExtensionTimes.get(i);
            if (availableTime > 0) {
                int neededBlocks = (int) Math.ceil((double) availableTime / 30.0);
                int neededTime = neededBlocks * 30;
                
                if (neededTime <= availableTime) {
                    levelTimes.get(i).extensionMinutes = neededTime;
                } else {
                    int shortage = neededTime - availableTime;
                    int totalBorrowed = 0;
                    
                    for (int j = i + 1; j < availableExtensionTimes.size(); j++) {
                        if (totalBorrowed >= shortage) break;
                        
                        int canBorrow = Math.min(availableExtensionTimes.get(j), shortage - totalBorrowed);
                        availableExtensionTimes.set(j, availableExtensionTimes.get(j) - canBorrow);
                        totalBorrowed += canBorrow;
                    }
                    
                    levelTimes.get(i).extensionMinutes = neededTime;
                }
            }
        }
        
        List<TimeExtensionDetail> timeExtensionPoints = new ArrayList<>();
        int totalTimeExtension = 0;
        
        for (LevelTimeCalculation levelTime : levelTimes) {
            if (levelTime.extensionMinutes > 0) {
                int extensionBlocks = (int) Math.ceil((double) levelTime.extensionMinutes / 30.0);
                int pointsPerBlock = timeExtensionTable.get(levelTime.level);
                int points = extensionBlocks * pointsPerBlock;
                
                if (points > 0) {
                    timeExtensionPoints.add(new TimeExtensionDetail(levelTime.level, points));
                    totalTimeExtension += points;
                }
            }
        }
        
        return new CalculationResult(basePoints, timeExtensionPoints, totalTimeExtension);
    }
    
    public int getBasePoints() {
        return getCalculationResult().getBasePoints();
    }
    
    public int getTimeExtensionPoints() {
        return getCalculationResult().getTotalTimeExtension();
    }
    
    public List<TimeExtensionDetail> getTimeExtensionDetails() {
        return getCalculationResult().getTimeExtensionPoints();
    }
    
    public int getEpiduralPoints() {
        if (!hasEpiduralAnesthesia || getTotalMinutes() <= 0) return 0;
        if (hasNerveBlock && nerveBlockType == NerveBlockType.EPIDURAL_SUBSTITUTE) return 0;
        return epiduralPointsTable.get(epiduralLocation);
    }
    
    public int getEpiduralTimeExtensionPoints() {
        if (!hasEpiduralAnesthesia || getTotalMinutes() <= 0) return 0;
        if (hasNerveBlock && nerveBlockType == NerveBlockType.EPIDURAL_SUBSTITUTE) return 0;
        
        int overtimeMinutes = Math.max(0, getTotalMinutes() - 120);
        if (overtimeMinutes <= 0) return 0;
        
        int extensionBlocks = (int) Math.ceil((double) overtimeMinutes / 30.0);
        int pointsPerBlock = epiduralTimeExtensionTable.get(epiduralLocation);
        return extensionBlocks * pointsPerBlock;
    }
    
    public int getNerveBlockPoints() {
        if (!hasNerveBlock || getTotalMinutes() <= 0) return 0;
        return nerveBlockPointsTable.get(nerveBlockType);
    }
    
    public int getTotalPoints() {
        int baseTotal = getBasePoints() + getTimeExtensionPoints() + getEpiduralPoints() + 
                       getEpiduralTimeExtensionPoints() + getNerveBlockPoints();
        
        double multiplier = 1.0;
        if (hasOvertimeAddition) {
            multiplier = 1.4;
        }
        if (hasNightHolidayAddition) {
            multiplier = 1.8;
        }
        
        return (int) (baseTotal * multiplier);
    }
    
    public void updateTime(int level, int minutes) {
        for (AnesthesiaLevelTime anesthesiaLevel : anesthesiaLevels) {
            if (anesthesiaLevel.getLevel() == level) {
                anesthesiaLevel.setMinutes(Math.max(0, minutes));
                break;
            }
        }
    }
    
    public int getTime(int level) {
        for (AnesthesiaLevelTime anesthesiaLevel : anesthesiaLevels) {
            if (anesthesiaLevel.getLevel() == level) {
                return anesthesiaLevel.getMinutes();
            }
        }
        return 0;
    }
    
    public void resetAllTimes() {
        for (AnesthesiaLevelTime anesthesiaLevel : anesthesiaLevels) {
            anesthesiaLevel.setMinutes(0);
        }
    }
    
    private static class LevelTimeCalculation {
        int level;
        int minutes;
        int baseUsed;
        int extensionMinutes;
        
        LevelTimeCalculation(int level, int minutes, int baseUsed, int extensionMinutes) {
            this.level = level;
            this.minutes = minutes;
            this.baseUsed = baseUsed;
            this.extensionMinutes = extensionMinutes;
        }
    }
    
    public SeverityLevel getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(SeverityLevel severityLevel) { this.severityLevel = severityLevel; }
    
    public boolean isHasEpiduralAnesthesia() { return hasEpiduralAnesthesia; }
    public void setHasEpiduralAnesthesia(boolean hasEpiduralAnesthesia) { this.hasEpiduralAnesthesia = hasEpiduralAnesthesia; }
    
    public EpiduralLocation getEpiduralLocation() { return epiduralLocation; }
    public void setEpiduralLocation(EpiduralLocation epiduralLocation) { this.epiduralLocation = epiduralLocation; }
    
    public boolean isHasNerveBlock() { return hasNerveBlock; }
    public void setHasNerveBlock(boolean hasNerveBlock) { this.hasNerveBlock = hasNerveBlock; }
    
    public NerveBlockType getNerveBlockType() { return nerveBlockType; }
    public void setNerveBlockType(NerveBlockType nerveBlockType) { this.nerveBlockType = nerveBlockType; }
    
    public boolean isHasOvertimeAddition() { return hasOvertimeAddition; }
    public void setHasOvertimeAddition(boolean hasOvertimeAddition) { this.hasOvertimeAddition = hasOvertimeAddition; }
    
    public boolean isHasNightHolidayAddition() { return hasNightHolidayAddition; }
    public void setHasNightHolidayAddition(boolean hasNightHolidayAddition) { this.hasNightHolidayAddition = hasNightHolidayAddition; }
}