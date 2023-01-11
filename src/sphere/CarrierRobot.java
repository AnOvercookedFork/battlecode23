package sphere;

import battlecode.common.*;

public strictfp class CarrierRobot extends Robot {

    MapLocation collectTarget;
    double collectTargetWeight;

    MapLocation islandTarget;
    double islandTargetWeight;

    public static final double RANDOM_LOC_WEIGHT = 0;
    public static final double KNOWN_LOC_WEIGHT = 100;
    public static final double MIN_HEALTH_TAKE_ANCHOR = 10;

    public static MapLocation[] hqs;

    public CarrierRobot(RobotController rc) {
        super(rc);
        hqs = new MapLocation[GameConstants.MAX_STARTING_HEADQUARTERS];
    }

    public void run() throws GameActionException {
        processNearbyRobots();

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
                if (tryPlaceAnchor()) break;
            }
        } else {
            while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY
                    && tryCollect()) {}
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY
                    && tryFindResources()) {
                while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                        && getWeight() < GameConstants.CARRIER_CAPACITY
                        && tryCollect()) {}
            }
        }
        
        if (getWeight() == GameConstants.CARRIER_CAPACITY) {
            tryTransferHQ();
        }
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

    public void processNearbyRobots() {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == rc.getTeam()) {
                if (robot.type == RobotType.HEADQUARTERS) {
                    // Update our list of headquarters, if not already on the list
                    updateHeadquarterList(robot.location);
                }
            } else {
                // Process enemy robots here
            }
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

    public WellInfo selectNearbyWell(WellInfo[] nearbyWells) {
        WellInfo best = null;
        int bestScore = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (WellInfo well : nearbyWells) {
            int score = well.getMapLocation().distanceSquaredTo(curr);
            if (score < bestScore) {
                bestScore = score;
                best = well;
            }
        }
        return best;
    }

    public boolean tryFindResources() throws GameActionException {
        if (collectTarget == null) {
            collectTarget = randomLocation();
            collectTargetWeight = RANDOM_LOC_WEIGHT;
        }
        
        WellInfo[] nearbyWells = rc.senseNearbyWells(-1);
        if (collectTargetWeight < KNOWN_LOC_WEIGHT) {
            WellInfo selectedWell = selectNearbyWell(nearbyWells);
            if (selectedWell != null) {
                collectTarget = selectedWell.getMapLocation();
                collectTargetWeight = KNOWN_LOC_WEIGHT;
            }
        }
        
        MapLocation curr = rc.getLocation();
        boolean success = false;
        while (curr.distanceSquaredTo(collectTarget) > 2 && tryFuzzy(collectTarget)) {
            curr = rc.getLocation();
            success = true;
        }
        return success;
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
        while (curr.distanceSquaredTo(hq) > 2 && tryFuzzy(hq)) {
            curr = rc.getLocation();
        }

        boolean success = false;

        for (ResourceType type : resourceTypes) {
            int amount = rc.getResourceAmount(type);
            for (MapLocation hqLoc : hqs) {
                if (hqLoc == null) continue;
                if (amount > 0
                        && rc.canTransferResource(hqLoc, type, amount)) {
                    rc.transferResource(hqLoc, type, amount);
                    success = true;
                }
            }
        }

        return success;
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
        }

        boolean success = false;
        while (curr.distanceSquaredTo(islandTarget) > 0 && tryFuzzy(islandTarget)) {
            curr = rc.getLocation();
            success = true;
        }
        return success;
    }
}
