package sphere1_14_2;

import battlecode.common.*;

import java.util.Random;

public strictfp class MapCache {

    RobotController rc;

    static class WellData {
        MapLocation location;
        ResourceType type;
        int rate;
        boolean fromComms;

        public WellData(MapLocation location, ResourceType type, int rate) {
            this.location = location;
            this.type = type;
            this.rate = rate;
            this.fromComms = false;
        }


        public WellData(MapLocation location, ResourceType type, int rate, boolean fromComms) {
            this.location = location;
            this.type = type;
            this.rate = rate;
            this.fromComms = fromComms;
        }

        int encode() {
            int code12_15 = 0;
            switch (type) {
                case MANA:
                    code12_15 = 1;
                    break;
                case ELIXIR:
                    code12_15 = 2;
                    break;
            }
            if (rate == GameConstants.WELL_ACCELERATED_RATE) {
                code12_15 += 4;
            }
            return location.x + (location.y << 6) + (code12_15 << 12);
        }

        static WellData decode(int code, boolean fromComms) {
            int x = code % 64;
            code = code >> 6;
            int y = code % 64;
            code = code >> 6;
            ResourceType type = ResourceType.ADAMANTIUM;
            switch (code % 4) {
                case 1:
                    type = ResourceType.MANA;
                    break;
                case 2:
                    type = ResourceType.ELIXIR;
                    break;
            }
            code = code >> 2;
            int rate;
            if (code == 1) {
                rate = GameConstants.WELL_ACCELERATED_RATE;
            } else {
                rate = GameConstants.WELL_STANDARD_RATE;
            }
            return new MapCache.WellData(new MapLocation(x, y), type, rate, fromComms);
        }
    }

    static class IslandData {
        MapLocation location;
        int idx;
        Team team;
        boolean fromComms;

        public IslandData(MapLocation location, int idx, Team team) {
            this.location = location;
            this.idx = idx;
            this.team = team;
            this.fromComms = false;
        }

        public IslandData(MapLocation location, int idx, Team team, boolean fromComms) {
            this.location = location;
            this.idx = idx;
            this.team = team;
            this.fromComms = fromComms;
        }

        int encode() {
            int code14_15 = 2; // NEUTRAL, by default
            switch (team) {
                case A:
                    code14_15 = 0;
                    break;
                case B:
                    code14_15 = 1;
                    break;
            }
            return (location.x >> 2) + ((location.y >> 2) << 4) + (idx << 8) + (code14_15 << 14);
        }

        static IslandData decode(int code, boolean fromComms) {
            int x = 4 * (code % 16) + 1;
            code = code >> 4;
            int y = 4 * (code % 16) + 1;
            code = code >> 4;
            int idx = code % 64;
            code = code >> 6;
            Team team = Team.NEUTRAL;
            switch (code) {
                case 0:
                    team = Team.A;
                    break;
                case 1:
                    team = Team.B;
                    break;
            }
            return new IslandData(new MapLocation(x, y), idx, team, fromComms);
        }
    }

    static class EnemyData {
        MapLocation location;
        int priority;
        int roundSeen;
        boolean fromComms;

        public EnemyData(MapLocation location, int priority, int roundSeen) {
            this.location = location;
            this.priority = priority;
            this.roundSeen = roundSeen;
            this.fromComms = false;
        }
        
        public EnemyData(MapLocation location, int priority, int roundSeen, boolean fromComms) {
            this.location = location;
            this.priority = priority;
            this.roundSeen = roundSeen;
            this.fromComms = fromComms;
        }

        int encode() {
            assert(priority < 8 && priority >= 0);
            return location.x + (location.y << 6) + (priority << 12);
        }

        static EnemyData decode(int code, int roundNum, boolean fromComms) {
            int x = code % 64;
            code = code >> 6;
            int y = code % 64;
            code = code >> 6;
            int priority = code % 8;
            return new EnemyData(new MapLocation(x, y), code % 8, roundNum - (code >> 3), fromComms);
        }
    }

    WellData[] wellCache;
    IslandData[] islandCache;
    EnemyData[] enemyCache;

    int wellCachePtr;
    int islandCachePtr;
    int enemyCachePtr;

    int wellCacheSize;
    int islandCacheSize;
    int enemyCacheSize;

    int wellSamplePtr;
    int islandSamplePtr;
    int enemySamplePtr;

    public static int WELL_CACHE_SIZE = 8;
    public static int ISLAND_CACHE_SIZE = 16;
    public static int ENEMY_CACHE_SIZE = 16;

    public MapCache(RobotController rc) {
        this.rc = rc;

        wellCache = new WellData[WELL_CACHE_SIZE];
        islandCache = new IslandData[ISLAND_CACHE_SIZE];
        enemyCache = new EnemyData[ENEMY_CACHE_SIZE];

        wellCachePtr = 0;
        islandCachePtr = 0;
        enemyCachePtr = 0;

        wellCacheSize = 0;
        islandCacheSize = 0;
        enemyCacheSize = 0;

        wellSamplePtr = 0;
        islandSamplePtr = 0;
        enemySamplePtr = 0;
    }

    public MapCache(RobotController rc, int wells_size, int islands_size, int enemies_size) {
        WELL_CACHE_SIZE = wells_size;
        ISLAND_CACHE_SIZE = islands_size;
        ENEMY_CACHE_SIZE = enemies_size;

        this.rc = rc;

        wellCache = new WellData[WELL_CACHE_SIZE];
        islandCache = new IslandData[ISLAND_CACHE_SIZE];
        enemyCache = new EnemyData[ENEMY_CACHE_SIZE];

        wellCachePtr = 0;
        islandCachePtr = 0;
        enemyCachePtr = 0;

        wellCacheSize = 0;
        islandCacheSize = 0;
        enemyCacheSize = 0;

        wellSamplePtr = 0;
        islandSamplePtr = 0;
        enemySamplePtr = 0;
    }


    public void setWellCache(int i, WellInfo winfo) {
        int idx = (wellCachePtr + i) % WELL_CACHE_SIZE;
        if (wellCache[idx] == null) {
            wellCache[idx] = new WellData(winfo.getMapLocation(), winfo.getResourceType(), winfo.getRate());
        } else {
            wellCache[idx].location = winfo.getMapLocation();
            wellCache[idx].type = winfo.getResourceType();
            wellCache[idx].rate = winfo.getRate();
        }
    }

    public void updateWellCache(WellInfo[] nearbyWells) {
        WellData wdata;
        for (WellInfo well : nearbyWells) {
            int idx = -1;
            for (int i = 0; i < wellCacheSize; i++) {
                wdata = wellCache[(wellCachePtr + i) % WELL_CACHE_SIZE];
                if (well.getMapLocation().equals(wdata.location)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                setWellCache(idx, well);
            } else {
                if (wellCacheSize < WELL_CACHE_SIZE) {
                    setWellCache((wellCachePtr + wellCacheSize) % WELL_CACHE_SIZE, well);
                    wellCacheSize++;
                } else {
                    setWellCache(wellCachePtr, well);
                    wellCachePtr = (wellCachePtr + 1) % WELL_CACHE_SIZE;
                }
            }
        }
    }

    public WellData sampleWellCache(boolean excludeComms) {
        WellData wdata;
        for (int i = 0; i < WELL_CACHE_SIZE; i++) {
            wdata = wellCache[(wellSamplePtr + i) % WELL_CACHE_SIZE];
            if (wdata != null && (!excludeComms || !wdata.fromComms)) {
                wellSamplePtr = (wellSamplePtr + i + 1) % WELL_CACHE_SIZE;
                return wdata;
            }
        }
        return null;
    }

    public void updateWellCacheFromComms(int code) {
        WellData newdata = WellData.decode(code, true);
        WellData wdata;
        for (int i = 0; i < wellCacheSize; i++) {
            wdata = wellCache[(wellCachePtr + i) % WELL_CACHE_SIZE];
            if (wdata.location.equals(newdata.location)) {
                if ((wdata.type != ResourceType.ELIXIR && newdata.type == ResourceType.ELIXIR)
                        || (wdata.rate == GameConstants.WELL_STANDARD_RATE && newdata.rate == GameConstants.WELL_ACCELERATED_RATE)) {
                    wellCache[(wellCachePtr + i) % WELL_CACHE_SIZE] = newdata;
                } else if (wdata.type == newdata.type && wdata.rate == newdata.rate) {
                    wellCache[(wellCachePtr + i) % WELL_CACHE_SIZE].fromComms = true;
                }
                return;
            }
        }
        
        if (wellCacheSize < WELL_CACHE_SIZE) {
            wellCache[(wellCachePtr + wellCacheSize) % WELL_CACHE_SIZE] = newdata;
            wellCacheSize++;
        } else {
            wellCache[wellCachePtr] = newdata;
            wellCachePtr = (wellCachePtr + 1) % WELL_CACHE_SIZE;
        }
    }

    public void debugWellCache() {
        WellData wdata;
        for (int i = 0; i < wellCacheSize; i++) {
            wdata = wellCache[(wellCachePtr + i) % WELL_CACHE_SIZE];
            int red = 0;
            int green = 0;
            int blue = 0;
            switch (wdata.type) {
                case ADAMANTIUM:
                    red = 125;
                    break;
                case MANA:
                    blue = 125;
                    break;
                case ELIXIR:
                    green = 125;
                    break;
            }
            if (wdata.rate > GameConstants.WELL_STANDARD_RATE) {
                red *= 2;
                blue *= 2;
                green *= 2;
            }
            rc.setIndicatorDot(wdata.location, red, green, blue);
        }
    }

    public void setIslandCache(int i, MapLocation l, int islandIdx, Team team) {
        int idx = (islandCachePtr + i) % ISLAND_CACHE_SIZE;
        if (islandCache[idx] == null) {
            islandCache[idx] = new IslandData(l, islandIdx, team);
        } else {
            islandCache[idx].location = l;
            islandCache[idx].idx = islandIdx;
            islandCache[idx].team = team;
        }
    }

    public void updateIslandCache() throws GameActionException {
        int[] nearbyIslands = rc.senseNearbyIslands();
        IslandData idata;
        for (int islandIdx : nearbyIslands) {
            MapLocation islandLoc = rc.senseNearbyIslandLocations(islandIdx)[0];
            Team controllingTeam = rc.senseTeamOccupyingIsland(islandIdx);
            int idx = -1; // This is the index in the cache, not the island index!
            for (int i = 0; i < islandCacheSize; i++) {
                idata = islandCache[(islandCachePtr + i) % ISLAND_CACHE_SIZE];
                if (islandIdx == idata.idx) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                setIslandCache(idx, islandLoc, islandIdx, controllingTeam);
            } else {
                if (islandCacheSize < ISLAND_CACHE_SIZE) {
                    setIslandCache((islandCachePtr + islandCacheSize) % ISLAND_CACHE_SIZE,
                            islandLoc, islandIdx, controllingTeam);
                    islandCacheSize++;
                } else {
                    setIslandCache(islandCachePtr, islandLoc, islandIdx, controllingTeam);
                    islandCachePtr = (islandCachePtr + 1) % ISLAND_CACHE_SIZE;
                }
            }
        }
    }

    public IslandData sampleIslandCache(boolean excludeComms) {
        IslandData idata;
        for (int i = 0; i < ISLAND_CACHE_SIZE; i++) {
            idata = islandCache[(islandSamplePtr + i) % ISLAND_CACHE_SIZE];
            if (idata != null && (!excludeComms || !idata.fromComms)) {
                islandSamplePtr = (islandSamplePtr + i + 1) % ISLAND_CACHE_SIZE;
                return idata;
            }
        }
        return null;
    }

    public void updateIslandCacheFromComms(int code) {
        IslandData newdata = IslandData.decode(code, true);
        IslandData idata;
        Team team = rc.getTeam();
        for (int i = 0; i < islandCacheSize; i++) {
            idata = islandCache[(islandCachePtr + i) % ISLAND_CACHE_SIZE];
            if (idata.idx == newdata.idx) {
                if (idata.team == team && newdata.team != team) {
                    islandCache[(islandCachePtr + i) % ISLAND_CACHE_SIZE] = newdata;
                } else if (idata.team == newdata.team) {
                    islandCache[(islandCachePtr + i) % ISLAND_CACHE_SIZE].fromComms = true;
                }
                return;
            }
        }
        
        if (islandCacheSize < ISLAND_CACHE_SIZE) {
            islandCache[(islandCachePtr + islandCacheSize) % ISLAND_CACHE_SIZE] = newdata;
            islandCacheSize++;
        } else {
            islandCache[islandCachePtr] = newdata;
            islandCachePtr = (islandCachePtr + 1) % ISLAND_CACHE_SIZE;
        }
    }

    public void setEnemyCache(int i, MapLocation l, int priority, int roundSeen) {
        int idx = (enemyCachePtr + i) % ENEMY_CACHE_SIZE;
        if (enemyCache[idx] == null) {
            enemyCache[idx] = new EnemyData(l, priority, roundSeen);
        } else {
            enemyCache[idx].location = l;
            enemyCache[idx].priority = priority;
            enemyCache[idx].roundSeen = roundSeen;
        }
    }

    public void updateEnemyCache(RobotInfo[] nearbyRobots) throws GameActionException {
        EnemyData edata;

        Team team = rc.getTeam();
        for (int i = 0; i < enemyCacheSize; i++) {
            edata = enemyCache[(enemyCachePtr + i) % ENEMY_CACHE_SIZE];
            if (edata == null) continue;
            if (rc.canSenseLocation(edata.location)) {
                RobotInfo robot = rc.senseRobotAtLocation(edata.location);
                if (robot == null || robot.getTeam() == team) {
                    enemyCache[(enemyCachePtr + i) % ENEMY_CACHE_SIZE] = null;
                }
            }
        }
        for (RobotInfo nearbyRobot : nearbyRobots) {
            if (nearbyRobot.team == team || nearbyRobot.type == RobotType.HEADQUARTERS) continue;
            int idx = -1;
            for (int i = 0; i < enemyCacheSize; i++) {
                edata = enemyCache[(enemyCachePtr + i) % ENEMY_CACHE_SIZE];
                if (edata == null) continue;
                if (edata.location.equals(nearbyRobot.location)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                setEnemyCache(idx, nearbyRobot.location, 1, rc.getRoundNum());
            } else {
                if (enemyCacheSize < ENEMY_CACHE_SIZE) {
                    setEnemyCache((enemyCachePtr + enemyCacheSize) % ENEMY_CACHE_SIZE,
                            nearbyRobot.location, 1, rc.getRoundNum());
                    enemyCacheSize++;
                } else {
                    setEnemyCache(enemyCachePtr, nearbyRobot.location, 1, rc.getRoundNum());
                    enemyCachePtr = (enemyCachePtr + 1) % ENEMY_CACHE_SIZE;
                }
            }
        }
    }

    public EnemyData sampleEnemyCache(boolean excludeComms) {
        EnemyData edata;
        for (int i = 0; i < ENEMY_CACHE_SIZE; i++) {
            edata = enemyCache[(enemySamplePtr + i) % ENEMY_CACHE_SIZE];
            if (edata != null && (!excludeComms || !edata.fromComms)) {
                enemySamplePtr = (enemySamplePtr + i + 1) % ENEMY_CACHE_SIZE;
                return edata;
            }
        }
        return null;
    }
    
    /*
    public void updateEnemyCacheFromComms(int code) {
        EnemyData newdata = EnemyData.decode(code, true);
        EnemyData edata;
        for (int i = 0; i < enemyCacheSize; i++) {
            edata = enemyCache;
        }
    }
    */
}
