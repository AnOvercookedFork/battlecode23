package sphere1_19_1;

import battlecode.common.*;

public strictfp class CarrierRobot extends Robot {
    public static final double RANDOM_LOC_WEIGHT = 0;
    public static final double KNOWN_LOC_WEIGHT = 100;
    public static final double MIN_HEALTH_TAKE_ANCHOR = 10;
    public static final int PANIC_HEALTH = 12;
    public static final int FLEE_TURNS = 1;
    public static final int EXECUTE_MODIFIER = 100;
    public static final int DAMAGED_MODIFIER = 15;

    public static MapLocation[] hqs;
    MapLocation[] nearbyEnemyHQs;
    int turnsSinceSeenEnemy = 0;
    double collectTargetWeight;
    MapLocation collectTarget;
    MapLocation enemyLastSeenLoc = null;
    StinkyNavigation snav;
    MapCache cache;
    ResourceType prevResource = ResourceType.ADAMANTIUM;

    public CarrierRobot(RobotController rc) {
        super(rc);
        hqs = null;
        cache = new MapCache(rc, 16);
        snav = new StinkyNavigation(rc);
    }

    public void run() throws GameActionException {
        Communications.readArray(rc);
        if (hqs == null) {
            hqs = Communications.getHQs(rc);
        }

        Communications.readWells(rc, cache);
        Communications.reportWell(rc, cache);

        Communications.readIslands(rc, cache);
        cache.updateIslandCache();
        Communications.reportIsland(rc, cache);

        processNearbyRobots();

        if (getWeight() == GameConstants.CARRIER_CAPACITY) {
            tryTransferHQ();
        }

        if (shouldTakeAnchor() && rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
            tryTakeAnchor();
        }

        if (rc.getAnchor() != null) {
            tryPlaceAnchor();
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryFindIsland()) {
                processNearbyRobots();
                if (tryPlaceAnchor())
                    break;
            }
        }
        if (rc.getAnchor() == null) {
            boolean finishedDeposit = tryFinishDeposit();
            while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY && tryCollect()) {
            }
            while (finishedDeposit && rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY && tryFindResources()) {
                if (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
                    processNearbyRobots();
                }
                while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                        && getWeight() < GameConstants.CARRIER_CAPACITY && tryCollect()) {
                }
            }
        }

        if (getWeight() == GameConstants.CARRIER_CAPACITY) {
            tryTransferHQ();
        }

        //cache.debugWellCache();
        cache.debugIslandCache();
    }

    public void processNearbyRobots() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        RobotInfo nearestEnemy = null;
        MapLocation nearestDangerousEnemy = null;
        int nearestDangerousEnemyDist = Integer.MAX_VALUE;
        int potentialDamage = getWeight() / 5;
        int enemyHp = 0;

        Team team = rc.getTeam();

        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == team) {
                if(robot.type == RobotType.CARRIER) {
                    potentialDamage += (robot.getResourceAmount(ResourceType.ADAMANTIUM) + robot.getResourceAmount(ResourceType.MANA)) * 2;
                }
            } else {
                // Process enemy robots here
                if (robot.type.damage > 0 && robot.type != RobotType.HEADQUARTERS) {
                    int dist = robot.location.distanceSquaredTo(curr);
                    if (dist < nearestDangerousEnemyDist) {
                        nearestDangerousEnemyDist = dist;
                        nearestEnemy = robot;
                        nearestDangerousEnemy = robot.location;
                    }
                }
                enemyHp += robot.getHealth();
            }
        }

        MapLocation nearestHQ = hqs[0];
        int nearestHQdist = nearestHQ.distanceSquaredTo(curr);

        int dist;
        for (int i = 1; i < hqs.length; i++) {
            dist = hqs[i].distanceSquaredTo(curr);
            if (dist < nearestHQdist) {
                nearestHQ = hqs[i];
                nearestHQdist = dist;
            }
        }

        if (nearestDangerousEnemy != null) {
            enemyLastSeenLoc = nearestDangerousEnemy;
            turnsSinceSeenEnemy = 0;

            if (potentialDamage > enemyHp && getWeight() > 5 && nearestHQdist > 8) {
                System.out.println("defending aggressively");
                if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
                    snav.tryNavigate(nearestDangerousEnemy);
                }
                if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
                    MapLocation target = getTarget(rc);
                    if (target != null) {
                        if (rc.canAttack(target)) {
                            rc.attack(target);
                        } else {
                            System.out.println("wat");
                        }
                    }
                }
            }

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
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryFuzzy(fleeDir)) {
            }
        }
    }

    public MapLocation getTarget(RobotController rc) throws GameActionException {
        RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent()); // costs about 100 bytecode
        MapLocation finalTarget = null;
        int maxScore = -1;
        MapLocation curr = rc.getLocation();
        for (RobotInfo target : targets) { // find max score (can optimize for bytecode if needed later)
            if (!target.location.isWithinDistanceSquared(curr, RobotType.CARRIER.actionRadiusSquared))
                continue;
            int score = scoreTarget(target, rc);
            if (score > maxScore) {
                maxScore = score;
                finalTarget = target.location;
            }
        }
        if (maxScore > 0) {
            return finalTarget;
        }
        int round = rc.getRoundNum();
        /*for (MapCache.EnemyData enemy : cache.enemyCache) {
            if (enemy == null || enemy.roundSeen < round
                    || !enemy.location.isWithinDistanceSquared(curr, RobotType.CARRIER.actionRadiusSquared))
                continue;
            if (enemy.priority > maxScore) {
                maxScore = enemy.priority;
                finalTarget = enemy.location;
            }
        }*/
        if (maxScore > 0 && finalTarget != null) {
            return finalTarget;
        }

        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(curr, 16)) {
            if (rc.canAttack(loc)) {
                return loc;
            }
        }

        return null;
    }

    public int scoreTarget(RobotInfo info, RobotController rc) throws GameActionException {
        int score = 0;

        switch (info.getType()) {
        case AMPLIFIER:
            score = 1;
            break;
        case BOOSTER:
            score = 5;
            break;
        case CARRIER:
            score = 1;
            break;
        case DESTABILIZER:
            score = 6;
            break;
        case HEADQUARTERS:
            return 0; // can't attack HQ
        case LAUNCHER:
            score = 2;
            break;
        }

        if (info.getHealth() < info.getType().getMaxHealth()) {
            score += DAMAGED_MODIFIER;
        }

        if (info.getHealth() <= getWeight() / 5) {
            score += EXECUTE_MODIFIER; // could add variable for this too
        }

        return score;
    }

    public int getWeight() throws GameActionException {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA)
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
            if (wdata == null)
                continue;
            switch (wdata.type) {
            case MANA:
                if (prevResource != ResourceType.MANA) {
                    score = 1.5;
                } else {
                    score = 1;
                }
                break;
            case ADAMANTIUM:
                if (prevResource != ResourceType.ADAMANTIUM) {
                    score = 1.5;
                } else {
                    score = 1;
                }
                break;
            case ELIXIR:
                score = 1.5;
                break;
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

        MapLocation selectedWell = selectWell();
        if (selectedWell != null) {
            collectTarget = selectedWell;
            collectTargetWeight = KNOWN_LOC_WEIGHT;
        }

        MapLocation curr = rc.getLocation();
        
        //rc.setIndicatorLine(curr, collectTarget, 255, 255, 0);

        return curr.distanceSquaredTo(collectTarget) > 2 && snav.tryNavigate(collectTarget);
    }

    public MapLocation selectHQ() throws GameActionException {
        MapLocation best = null;
        int bestScore = Integer.MAX_VALUE;
        MapLocation curr = rc.getLocation();
        for (int i = 0; i < hqs.length; i++) {
            int score = hqs[i].distanceSquaredTo(curr);
            if (score < bestScore) {
                bestScore = score;
                best = hqs[i];
            }
        }
        return best;
    }

    // find an HQ and deposit resources there
    public boolean tryTransferHQ() throws GameActionException {
        MapLocation hq = selectHQ();
        MapLocation curr = rc.getLocation();
        while (curr.distanceSquaredTo(hq) > 2 && snav.tryNavigate(hq)) {
            curr = rc.getLocation();
        }

        boolean success = false;

        for (ResourceType type : resourceTypes) {
            int amount = rc.getResourceAmount(type);
            if (amount == 0)
                continue;
            for (MapLocation hqLoc : hqs) {
                if (hqLoc == null)
                    continue;
                if (rc.canTransferResource(hqLoc, type, amount)) {
                    rc.transferResource(hqLoc, type, amount);
                    prevResource = type;
                    collectTarget = null;
                    collectTargetWeight = 0;
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
            if (amount == 0)
                continue;
            for (MapLocation hqLoc : hqs) {
                if (hqLoc == null)
                    continue;
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
        for (int i = 0; i < hqs.length; i++) {
            if (rc.canTakeAnchor(hqs[i], Anchor.ACCELERATING)) {
                rc.takeAnchor(hqs[i], Anchor.STANDARD);
                return true;
            }
            if (rc.canTakeAnchor(hqs[i], Anchor.STANDARD)) {
                rc.takeAnchor(hqs[i], Anchor.STANDARD);
                return true;
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
        MapLocation islandTarget = getIslandTarget(Team.NEUTRAL, cache, false);

        return rc.getLocation().distanceSquaredTo(islandTarget) > 0 && snav.tryNavigate(islandTarget);
    }
}
