package sphere;

import battlecode.common.*;

public strictfp class CarrierRobot extends Robot {
    public static final double RANDOM_LOC_WEIGHT = 0;
    public static final double KNOWN_LOC_WEIGHT = 100;
    public static final double MIN_HEALTH_TAKE_ANCHOR = 10;
    public static final int PANIC_HEALTH = 12;
    public static final int FLEE_TURNS = 2;
    
    public static MapLocation[] hqs;
    int turnsSinceSeenEnemy = 0;
    double collectTargetWeight;
    double islandTargetWeight;
    MapLocation collectTarget;
    MapLocation islandTarget;
    MapLocation enemyLastSeenLoc = null;
    StinkyNavigation snav;
    MapCache cache;

    public CarrierRobot(RobotController rc) {
        super(rc);
        hqs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
        cache = new MapCache(rc, 16, 8, 4);
        snav = new StinkyNavigation(rc);
    }

    public void run() throws GameActionException {
        processNearbyRobots();
        Communications.readArray(rc);

        Communications.readWells(rc, cache);
        Communications.reportWell(rc, cache);

        cache.updateIslandCache();
        Communications.readIslands(rc, cache);
        Communications.reportIsland(rc, cache);

        if (getWeight() == GameConstants.CARRIER_CAPACITY) {
            tryTransferHQ();
        }

        if (shouldTakeAnchor() && rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
            tryTakeAnchor();
        }

        if (rc.getAnchor() != null) {
            tryPlaceAnchor();
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && tryFindIsland()) {
                processNearbyRobots();
                if (tryPlaceAnchor()) break;
            }
        }
        if (rc.getAnchor() == null) {
            boolean finishedDeposit = tryFinishDeposit();
            while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY
                    && tryCollect()) {}
            while (finishedDeposit
                    && rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY
                    && tryFindResources()) {
                processNearbyRobots();
                while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                        && getWeight() < GameConstants.CARRIER_CAPACITY
                        && tryCollect()) {}
            }
        }
        
        if (getWeight() == GameConstants.CARRIER_CAPACITY) {
            tryTransferHQ();
        }

        cache.debugWellCache();
    }

    public void updateHeadquarterList(MapLocation location) {
        int emptyPos = -1;
        for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (hqs[i] == null) {
                if (emptyPos < 0) {
                    emptyPos = i;
                }
            } else if (hqs[i].equals(location)) {
                emptyPos = -1;
                break;
            }
        }
        if (emptyPos >= 0) {
            hqs[emptyPos] = location;
        }
    }

    public void processNearbyRobots() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        MapLocation nearestDangerousEnemy = null;
        int nearestDangerousEnemyDist = Integer.MAX_VALUE;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == rc.getTeam()) {
                if (robot.type == RobotType.HEADQUARTERS) {
                    // Update our list of headquarters, if not already on the list
                    updateHeadquarterList(robot.location);
                }
            } else {
                // Process enemy robots here
                if (robot.type.damage > 0) {
                    int dist = robot.location.distanceSquaredTo(curr);
                    if (dist < nearestDangerousEnemyDist) {
                        nearestDangerousEnemyDist = dist;
                        nearestDangerousEnemy = robot.location;
                    }
                }
            }
        }

        if (nearestDangerousEnemy != null) {
            enemyLastSeenLoc = nearestDangerousEnemy;
            turnsSinceSeenEnemy = 0;

            if (rc.getHealth() <= PANIC_HEALTH && getWeight() >= 5 && rc.canAttack(nearestDangerousEnemy)) {
                rc.attack(nearestDangerousEnemy);
            }

            if (collectTargetWeight < KNOWN_LOC_WEIGHT) {
                collectTarget = null;
            }
        } else {
            turnsSinceSeenEnemy++;
        }

        if (enemyLastSeenLoc != null && turnsSinceSeenEnemy <= FLEE_TURNS) {
            Direction fleeDir = enemyLastSeenLoc.directionTo(curr);
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && tryFuzzy(fleeDir)) {}
        }
    }

    public int getWeight() throws GameActionException {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM)
            + rc.getResourceAmount(ResourceType.MANA)
            + rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public boolean tryCollect() throws GameActionException {
        MapLocation curr = rc.getLocation();
        boolean success = false;
        if (rc.canCollectResource(curr, -1)) {
            rc.collectResource(curr, -1);
            success = true;
        }
        for (Direction d : directions) {
            MapLocation adj = curr.add(d);
            if (rc.canCollectResource(adj, -1)) {
                rc.collectResource(adj, -1);
                success = true;
            }
        }
        return success;
    }

    public MapLocation selectWell() {
        MapLocation best = null;
        double bestScore = 0;
        MapLocation curr = rc.getLocation();
        double score = 0;
        for (MapCache.WellData wdata : cache.wellCache) {
            if (wdata == null) continue;
            switch (wdata.type) {
                case MANA:
                    score = 10;
                case ADAMANTIUM:
                    score = 1;
                case ELIXIR:
                    score = 1.5;
            }
            score /= (Math.sqrt(curr.distanceSquaredTo(wdata.location)) + 1);
            if (score > bestScore) {
                bestScore = score;
                best = wdata.location;
            }
        }
        return best;
    }

    public boolean tryFindResources() throws GameActionException {
        if (collectTarget != null && collectTargetWeight < KNOWN_LOC_WEIGHT) {
            if (rc.canSenseLocation(collectTarget)) {
                WellInfo well = rc.senseWell(collectTarget);
                if (well == null) {
                    collectTarget = null;
                }
            }
        }

        if (collectTarget == null) {
            collectTarget = randomLocation();
            collectTargetWeight = RANDOM_LOC_WEIGHT;
        }
        
        WellInfo[] nearbyWells = rc.senseNearbyWells(-1);
        cache.updateWellCache(nearbyWells);
        
        if (collectTargetWeight < KNOWN_LOC_WEIGHT) {
            MapLocation selectedWell = selectWell();
            if (selectedWell != null) {
                collectTarget = selectedWell;
                collectTargetWeight = KNOWN_LOC_WEIGHT;
            }
        }

        MapLocation curr = rc.getLocation();

        return curr.distanceSquaredTo(collectTarget) > 2 && snav.tryNavigate(collectTarget);
    }

    public MapLocation selectHQ() throws GameActionException {
        MapLocation best = null;
        int bestScore = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (hqs[i] != null) {
                int score = hqs[i].distanceSquaredTo(curr);
                if (score < bestScore) {
                    bestScore = score;
                    best = hqs[i];
                }
            }
        }
        return best;
    }

    public boolean tryTransferHQ() throws GameActionException {
        MapLocation hq = selectHQ();
        MapLocation curr = rc.getLocation();
        while (curr.distanceSquaredTo(hq) > 2 && snav.tryNavigate(hq)) {
            curr = rc.getLocation();
        }

        boolean success = false;

        for (ResourceType type : resourceTypes) {
            int amount = rc.getResourceAmount(type);
            if (amount == 0) continue;
            for (MapLocation hqLoc : hqs) {
                if (hqLoc == null) continue;
                if (rc.canTransferResource(hqLoc, type, amount)) {
                    rc.transferResource(hqLoc, type, amount);
                    success = true;
                }
            }
        }

        return success;
    }
    
    // check for resources to deposit other than the one that was just deposited
    public boolean tryFinishDeposit() throws GameActionException {

        boolean hqNearby = false;
        MapLocation curr = rc.getLocation();
        
        for (ResourceType type : resourceTypes) {
            int amount = rc.getResourceAmount(type);
            if (amount == 0) continue;
            for (MapLocation hqLoc : hqs) {
                if (hqLoc == null) continue;
                if (hqLoc.distanceSquaredTo(curr) <= 2) {
                    hqNearby = true;
                    if (rc.canTransferResource(hqLoc, type, amount)) {
                        rc.transferResource(hqLoc, type, amount);
                    }
                }
            }
        }

        return !hqNearby || getWeight() == 0;
    }

    public boolean shouldTakeAnchor() throws GameActionException {
        return rc.getHealth() > MIN_HEALTH_TAKE_ANCHOR && getWeight() == 0;
    }

    public boolean tryTakeAnchor() throws GameActionException {
        for (int i = 0; i < GameConstants.MAX_STARTING_HEADQUARTERS; i++) {
            if (hqs[i] != null) {
                if (rc.canTakeAnchor(hqs[i], Anchor.ACCELERATING)) {
                    rc.takeAnchor(hqs[i], Anchor.STANDARD);
                    return true;
                }
                if (rc.canTakeAnchor(hqs[i], Anchor.STANDARD)) {
                    rc.takeAnchor(hqs[i], Anchor.STANDARD);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean tryPlaceAnchor() throws GameActionException {
        MapLocation curr = rc.getLocation();
        int island = rc.senseIsland(curr);
        if (island != -1 && rc.senseAnchor(island) == null && rc.canPlaceAnchor()) {
            rc.placeAnchor();
            return true;
        }
        return false;
    }

    public boolean tryFindIsland() throws GameActionException {
        if (islandTarget != null) {
            if (rc.canSenseLocation(islandTarget)) {
                int island = rc.senseIsland(islandTarget);
                if (island == -1 || rc.senseAnchor(island) != null) {
                    islandTarget = null;
                }
            }
        }

        if (islandTarget == null) {
            islandTarget = randomLocation();
            islandTargetWeight = RANDOM_LOC_WEIGHT;
        }

        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation nearestLoc = null;
        int nearestLocDist = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        int dist;
        for (int nearbyIsland : nearbyIslands) {
            if (rc.senseAnchor(nearbyIsland) == null) {
                MapLocation[] locs = rc.senseNearbyIslandLocations(nearbyIsland);
                for (MapLocation loc : locs) {
                    dist = curr.distanceSquaredTo(loc);
                    if (dist < nearestLocDist) {
                        nearestLoc = loc;
                        nearestLocDist = dist;
                    }
                }
            }
        }

        if (nearestLoc != null) {
            islandTarget = nearestLoc;
            islandTargetWeight = KNOWN_LOC_WEIGHT;
        } else if (islandTargetWeight < KNOWN_LOC_WEIGHT) {
            for (MapCache.IslandData island : cache.islandCache) {
                if (island != null && island.team == Team.NEUTRAL) {
                    dist = curr.distanceSquaredTo(island.location);
                    if (dist < nearestLocDist) {
                        nearestLoc = island.location;
                        nearestLocDist = dist;
                    }
                }
            }
            if (nearestLoc != null) {
                islandTarget = nearestLoc;
                islandTargetWeight = KNOWN_LOC_WEIGHT;
            }
        }

        return curr.distanceSquaredTo(islandTarget) > 0 && snav.tryNavigate(islandTarget);
    }
}
