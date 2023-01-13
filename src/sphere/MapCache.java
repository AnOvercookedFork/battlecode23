package sphere;

import battlecode.common.*;

import java.util.Random;

public strictfp class MapCache {

    RobotController rc;

    class WellData {
        MapLocation location;
        ResourceType type;
        int rate;

        public WellData(MapLocation location, ResourceType type, int rate) {
            this.location = location;
            this.type = type;
            this.rate = rate;
        }
    }

    class IslandData {
        MapLocation location;
        int idx;
        Team team;
        public IslandData(MapLocation location, int idx, Team team) {
            this.location = location;
            this.idx = idx;
            this.team = team;
        }
    }

    class EnemyData {
        MapLocation location;
        int priority;
        int roundSeen;

        public EnemyData(MapLocation location, int priority, int roundSeen) {
            this.location = location;
            this.priority = priority;
            this.roundSeen = roundSeen;
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

    public static final int WELL_CACHE_SIZE = 16;
    public static final int ISLAND_CACHE_SIZE = 35;
    public static final int ENEMY_CACHE_SIZE = 16;

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
}
