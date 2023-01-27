package torus1_26_1_carrier;

import battlecode.common.*;

public strictfp class CarrierRobot extends Robot {
    public static final double RANDOM_LOC_WEIGHT = 20;
    public static final double KNOWN_LOC_WEIGHT = 100;
    public static final double MIN_HEALTH_TAKE_ANCHOR = 10;
    public static final int PANIC_HEALTH = 12;
    public static final int FLEE_TURNS = 1;
    public static final int EXECUTE_MODIFIER = 100;
    public static final int DAMAGED_MODIFIER = 15;
    public static final MapLocation EMPTY = new MapLocation(-50, -50);
    public static final int WELL_SATURATED_THRESHOLD = 9; // how many carriers per well?
    public static final int BLACKLIST_RADIUS = 4;
    public static final int WELL_RADIUS = 4;
    public static final int BLACKLIST_ROUNDS = 50;
    public static final int IS_ENEMY_BLACKLIST_ROUNDS = 20;
    public static final int PREFER_HQ_KG = 30;
    public static final int DO_THE_SHUFFLE_THRESHOLD = 6;
    public static final int THRESHOLD_AD_DISTANCE = 20;
    public static final int TRY_TRANSFER_THRESHOLD = 20;

    public static MapLocation[] hqs;
    MapLocation[] nearbyEnemyHQs;
    int turnsSinceSeenEnemy = 0;
    double collectTargetWeight;
    MapLocation collectTarget;
    MapLocation enemyLastSeenLoc = null;
    StinkyNavigation snav;
    MapCache cache;
    ResourceType prevResource = ResourceType.ADAMANTIUM;

    MapLocation blacklist = EMPTY;
    int blacklistRound = 0;
    int nearbyCarriers;
    MapLocation nearestHQ;
    HQLocations hqLocs;

    int roundCollectAdamantium; // minimum round before starting to collect ad

    public CarrierRobot(RobotController rc) throws GameActionException {
        super(rc);
        hqs = null;
        cache = new MapCache(rc, 16);
        snav = new StinkyNavigation(rc);
        hqLocs = new HQLocations(rc);
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
        hqLocs.updateHQSymms(rc);
        hqLocs.updateSymmsFromComms();
        

        processNearbyRobots();

        if (rc.getAnchor() != null) {
            tryPlaceAnchor();
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryFindIsland()) {
                processNearbyRobots();
                if (tryPlaceAnchor())
                    break;
            }
        }
        if (rc.getAnchor() == null) {

            if (shouldTakeAnchor() && rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
                tryTakeAnchor();
            }
            boolean finishedDeposit = tryFinishDeposit();
            boolean collected = false;
            while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && getWeight() < GameConstants.CARRIER_CAPACITY && tryCollect()) {
                collected = true;
            }
            int weight = rc.getWeight();
            if (weight == GameConstants.CARRIER_CAPACITY || (!collected && weight >= TRY_TRANSFER_THRESHOLD)) {
                tryTransferHQ();
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

            if (getWeight() == GameConstants.CARRIER_CAPACITY) {
                tryTransferHQ();
            }
        }

        if (Clock.getBytecodesLeft() > 4000) {
            //System.out.println("Carrier is updating symms.");
            hqLocs.eliminateSymms(rc);
        }


        //cache.debugWellCache();
        //cache.debugIslandCache();
    }
    
    /**
     * Checks for nearby enemies and either flees or throws resources at them.
     */
    public void processNearbyRobots() throws GameActionException {
        int hqCt = 0;
        MapLocation[] tempEnemyHQs = {EMPTY, EMPTY, EMPTY, EMPTY};
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        RobotInfo nearestEnemy = null;
        MapLocation nearestDangerousEnemy = null;
        int nearestDangerousEnemyDist = Integer.MAX_VALUE;
        int potentialDamage = rc.getWeight() / 5;
        int enemyHp = 0;
        boolean isDefenderNearby = false;
        int defendersNearby = 0;
        int dangerousEnemies = 0;

        Team team = rc.getTeam();
        
        nearbyCarriers = 0;

        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == team) {
                switch (robot.type) {
                    case CARRIER:
                        potentialDamage += (robot.getResourceAmount(ResourceType.ADAMANTIUM) + robot.getResourceAmount(ResourceType.MANA)) * 2;
                        nearbyCarriers++;
                        break;
                    case LAUNCHER:
                        defendersNearby++;
                        break;
                }
            } else {
                // Process enemy robots here
                if (robot.type == RobotType.HEADQUARTERS) {

                    tempEnemyHQs[hqCt] = robot.getLocation();
                    hqCt++;
                } else {
                    if (robot.type.damage > 0) {
                        dangerousEnemies++;
                        int dist = robot.location.distanceSquaredTo(curr);
                        if (dist < nearestDangerousEnemyDist) {
                            nearestDangerousEnemyDist = dist;
                            nearestEnemy = robot;
                            nearestDangerousEnemy = robot.location;
                        }
                    }
                    enemyHp += robot.getHealth();
                    Communications.tryAddEnemy(rc, robot.location);
                }
            }
        }
        
        nearbyEnemyHQs = new MapLocation[hqCt];
        for(int i = 0; i < hqCt; i++) {
            nearbyEnemyHQs[i] = tempEnemyHQs[i];
        }

        nearestHQ = hqs[0];
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
            if (collectTarget != null
                    && nearestDangerousEnemy.isWithinDistanceSquared(collectTarget, RobotType.LAUNCHER.visionRadiusSquared)) {
                BlacklistMap.blacklist(collectTarget.x, collectTarget.y, rc.getRoundNum() + IS_ENEMY_BLACKLIST_ROUNDS);
                collectTarget = null;
                collectTargetWeight = 0;
            }

            if (potentialDamage > enemyHp && rc.getWeight() > 5 && nearestHQdist > 8) {
                //System.out.println("defending aggressively");
                if (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT) {
                    snav.tryNavigate(nearestDangerousEnemy, nearbyEnemyHQs);
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

            if (rc.getHealth() <= PANIC_HEALTH && rc.getWeight() >= 5 && rc.canAttack(nearestDangerousEnemy)) {
                rc.attack(nearestDangerousEnemy);
            }

            if (collectTargetWeight < KNOWN_LOC_WEIGHT) {

                collectTarget = null;
            }
        } else {
            turnsSinceSeenEnemy++;
        }

        if (enemyLastSeenLoc != null && turnsSinceSeenEnemy <= FLEE_TURNS
                && (rc.getHealth() <= PANIC_HEALTH || defendersNearby == 0)) {
            Direction fleeDir = enemyLastSeenLoc.directionTo(curr);
            while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryFuzzy(fleeDir)) {
            }
        }
    }
    
    /**
     * Chooses a target from nearby robots and returns its location.
     */
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
        /*int round = rc.getRoundNum();
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
    
    /**
     * Returns total weight this robot is carrying. DEPRECATED use rc.getWeight() instead
     */
    public int getWeight() throws GameActionException {
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) + rc.getResourceAmount(ResourceType.MANA)
                + rc.getResourceAmount(ResourceType.ELIXIR) + (rc.getAnchor() != null? GameConstants.ANCHOR_WEIGHT : 0);
    }
    
    /**
     * Tries collecting resources from all adjacent locations.
     * @return true if something was collected
     */
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
    
    /**
     * Chooses a well from known well locations.
     */
    public MapLocation selectWell() {
        MapLocation best = null;
        double bestScore = 0;
        MapLocation curr = rc.getLocation();
        double score = 0;
        int round = rc.getRoundNum();
        for (MapCache.WellData wdata : cache.wellCache) {
            if (wdata == null ||
                    BlacklistMap.is_blacklisted(wdata.location.x, wdata.location.y, round))
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
                if (round >= roundCollectAdamantium) {
                if (prevResource != ResourceType.ADAMANTIUM) {
                    score = 1.5;
                } else {
                    score = 1;
                }
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
    
    /**
     * Tries to move towards resources, if not already adjacent to one.
     * @return true if moved, false otherwise
     */
    public boolean tryFindResources() throws GameActionException {

        MapLocation curr = rc.getLocation();


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
        if (rc.canSenseLocation(collectTarget) && curr.distanceSquaredTo(collectTarget) > 2) {
            RobotInfo[] allies = rc.senseNearbyRobots(collectTarget, WELL_RADIUS, rc.getTeam());
            int carriers = 0;
            for (RobotInfo ally : allies) {
                if (ally.type == RobotType.CARRIER) carriers++;
            }
            
            int blocked = 0;
            MapLocation l = curr.add(Direction.NORTH);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.NORTHEAST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.EAST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.SOUTHEAST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.SOUTH);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.SOUTHWEST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.WEST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            l = curr.add(Direction.NORTHWEST);
            if (rc.canSenseLocation(l) && !rc.sensePassability(l)) blocked++;
            if (carriers + blocked > WELL_SATURATED_THRESHOLD) {
                BlacklistMap.blacklist(collectTarget.x, collectTarget.y, rc.getRoundNum() + BLACKLIST_ROUNDS);
            }
        }


        if (hqLocs.getNearestPossibleEnemyHQ(rc).distanceSquaredTo(curr) < THRESHOLD_AD_DISTANCE) {
            roundCollectAdamantium = 20;
        } else {
            roundCollectAdamantium = 0;
        }
            
        MapLocation selectedWell = selectWell();
        if (selectedWell != null) {
            collectTarget = selectedWell;
            collectTargetWeight = KNOWN_LOC_WEIGHT;
        }

        
        //rc.setIndicatorLine(curr, collectTarget, 255, 255, 0);
        
        if (curr.distanceSquaredTo(collectTarget) <= 2) {
            // do micro
            Direction best = Direction.CENTER;
            MapLocation l;
            if (rc.getWeight() >= PREFER_HQ_KG) {
                // Go for closest to HQ
                int bestScore;
                int score;
                l = curr.add(rc.senseMapInfo(curr).getCurrentDirection());
                bestScore = l.distanceSquaredTo(nearestHQ);
                if (!l.isWithinDistanceSquared(collectTarget, 2)) {
                    score = 1000000;
                }
                for (Direction d : directions) {
                    if (!rc.canMove(d)) continue;

                    l = curr.add(d);
                    l = l.add(rc.senseMapInfo(l).getCurrentDirection());

                    if (l.isWithinDistanceSquared(collectTarget, 2)) {
                        score = l.distanceSquaredTo(nearestHQ);
                        if (score < bestScore) {
                            bestScore = score;
                            best = d;
                        }
                    }
                }
            } else if (nearbyCarriers < DO_THE_SHUFFLE_THRESHOLD) {
                // Go for low cooldown
                double bestScore;
                double score;
                Team team = rc.getTeam();
                l = curr.add(rc.senseMapInfo(curr).getCurrentDirection());
                bestScore = rc.senseMapInfo(l).getCooldownMultiplier(team);
                if (!l.isWithinDistanceSquared(collectTarget, 2)) {
                    score = 1000000;
                }
                Direction d = Direction.NORTH;
                do {
                    //System.out.println("BRR direction is " + d);
                    if (!rc.canMove(d)) {
                        d = d.rotateRight();
                        continue;
                    }

                    l = curr.add(d);
                    l = l.add(rc.senseMapInfo(l).getCurrentDirection());

                    if (l.isWithinDistanceSquared(collectTarget, 2)) {
                        score = rc.senseMapInfo(l).getCooldownMultiplier(team);
                        if (score <= bestScore) {
                            bestScore = score;
                            best = d;
                        }
                    }
                    d = d.rotateRight();
                } while (d != Direction.NORTH);
            } else {
                // Do the shuffe! AKA, move to any position around the well to try to make room.
                MapInfo info;
                Direction d = Direction.NORTH;
                do {
                    if (!rc.canMove(d)) {
                        d = d.rotateRight();
                        continue;
                    }

                    l = curr.add(d);
                    l = l.add(rc.senseMapInfo(l).getCurrentDirection());

                    if (l.isWithinDistanceSquared(collectTarget, 2)) {
                        best = d;
                        break;
                    }
                    d = d.rotateRight();
                } while (d != Direction.NORTH);
            }
            if (rc.canMove(best)) {
                rc.move(best);
                return true;
            }
            return false;
        } else {
            return snav.tryNavigate(collectTarget, nearbyEnemyHQs);
        }
    }
    
    /**
     * Returns the closest HQ to the current location.
     */
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
    
    /**
     * Tries to navigate towards an HQ and deposit resources there.
     * @return true if resources were transferred
     */
    public boolean tryTransferHQ() throws GameActionException {
        MapLocation hq = selectHQ();
        MapLocation curr = rc.getLocation();
        while (curr.distanceSquaredTo(hq) > 2 && snav.tryNavigate(hq, nearbyEnemyHQs)) {
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

    /**
     * If we're currently next to an HQ and have resources, drop it before moving away!
     * @return true if we're not next to an HQ, or if we have nothing to transfer
     */
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
    
    /**
     * Decides whether or not we should take an anchor from HQ.
     */
    public boolean shouldTakeAnchor() throws GameActionException {
        return rc.getHealth() > MIN_HEALTH_TAKE_ANCHOR && getWeight() == 0;
    }
    
    /**
     * Tries to take an anchor from an HQ.
     * @return true if we got an anchor
     */
    public boolean tryTakeAnchor() throws GameActionException {
        for (int i = 0; i < hqs.length; i++) {
            if (rc.canTakeAnchor(hqs[i], Anchor.ACCELERATING)) {
                rc.takeAnchor(hqs[i], Anchor.ACCELERATING);
                return true;
            }
            if (rc.canTakeAnchor(hqs[i], Anchor.STANDARD)) {
                rc.takeAnchor(hqs[i], Anchor.STANDARD);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Places an anchor, if we are currently sitting on an unoccupied island.
     */
    public boolean tryPlaceAnchor() throws GameActionException {
        MapLocation curr = rc.getLocation();
        int island = rc.senseIsland(curr);
        if (island != -1 && rc.senseAnchor(island) == null && rc.canPlaceAnchor()) {
            rc.placeAnchor();
            return true;
        }
        return false;
    }
    
    /**
     * Tries to move towards a neutral island, if not currently sitting on one.
     * @return true if we moved
     */
    public boolean tryFindIsland() throws GameActionException {
        MapLocation islandTarget = getIslandTarget(Team.NEUTRAL, cache, false);

        //rc.setIndicatorLine(rc.getLocation(), islandTarget, 255, 255, 0);

        return rc.getLocation().distanceSquaredTo(islandTarget) > 0 && snav.tryNavigate(islandTarget, nearbyEnemyHQs);
    }
}
