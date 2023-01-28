package torus;

import battlecode.common.*;

public strictfp class LauncherRobot extends Robot {
    public static final double RANDOM_LOC_WEIGHT = 100;
    public static final double HQ_LOC_WEIGHT = 120;
    public static final double INITIAL_LOC_WEIGHT = 100;
    public static final double FOUND_BASE_WEIGHT = 100;
    public static final double REPORTED_BASE_WEIGHT = 80;
    public static final double GIVE_UP_WEIGHT = 10;
    public static final int GIVE_UP_RADIUS_SQ = 2;
    private static final int EXECUTE_THRESHOLD = 20; // increased priority against robots with this hp or under
    private static final int RESOURCE_THRESHOLD = 30; // increased priority on resource holding carriers
    public static final int EXECUTE_MODIFIER = 10;
    public static final int ISLAND_MODIFIER = 0;
    public static final int ANCHOR_MODIFIER = 10;
    public static final boolean USE_NEW_MICRO = true;
    public static final int STAY_IN_COMBAT_TURNS = 6;
    public static final int HEAL_HEALTH = 133;
    public static final int LEADER_DIST = 13;

    MapLocation target;
    double targetWeight;
    MapLocation leader;
    MapLocation otherLeader;
    MapCache cache;
    StinkyNavigation snav;
    MapLocation[] reflectedHQs;
    int hqTargetIndex = 0;
    int turnsSinceInCombat = 9001;
    MapLocation[] nearbyEnemyHQs;

    HQLocations hqLocs;
    MapLocation hqTarget;
    Micro micro;
    MapLocation nearestReportedEnemy;

    RobotInfo[] prevTargets;
    RobotInfo[] prevRoundTargets = null;

    public LauncherRobot(RobotController rc) throws GameActionException {
        super(rc);
        snav = new StinkyNavigation(rc);
        cache = new MapCache(rc, 4);
        hqLocs = new HQLocations(rc);
        hqTarget = null;
        micro = new Micro(rc);
    }

    public void run() throws GameActionException {
        processNearbyRobots();
        prevTargets = null;

        Communications.readArray(rc);

        cache.updateIslandCache();
        Communications.readIslands(rc, cache);
        Communications.reportIsland(rc, cache);
        hqLocs.updateHQSymms(rc);
        hqLocs.updateSymmsFromComms();

        nearestReportedEnemy = Communications.getNearestEnemy(rc);

        while (rc.isActionReady() && tryAttack()) {
        }
        while (rc.isMovementReady() && tryMove()) {
            while (rc.isActionReady() && tryAttack()) {
            }
        }

        turnsSinceInCombat++;

        if (Clock.getBytecodesLeft() > 4000) {
            hqLocs.eliminateSymms(rc);
            // hqLocs.debugSymms();
        } else {
            System.out.println("Skipping symmetry detection");
        }
        
        // rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 0);
        //
        if (Clock.getBytecodesLeft() > 1000) {
            cache.updateWellCache(rc.senseNearbyWells());
            Communications.readWells(rc, cache);
            Communications.reportWell(rc, cache);
        } else {
            System.out.println("Skipping wells");
        }

        prevRoundTargets = prevTargets;
    }

    /**
     * Chooses a leader based on health and id.
     */
    public void processNearbyRobots() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        Team team = rc.getTeam();
        leader = null;
        double highestHealth = 0;
        double health;
        MapLocation[] tempEnemyHQs = { null, null, null, null };
        int enemyHQCount = 0;

        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == team) {
                switch (robot.type) {
                case LAUNCHER:
                    health = robot.health + 1.0 / robot.ID;
                    if (health > highestHealth) {
                        leader = robot.location;
                        highestHealth = health;
                    }

                    break;
                }
            } else {
                if (robot.type == RobotType.HEADQUARTERS) {
                    tempEnemyHQs[enemyHQCount] = robot.getLocation();
                    enemyHQCount++;
                    break;
                } else {
                    if (robot.type.damage > 0) {
                        turnsSinceInCombat = 0;
                    }
                    Communications.tryAddEnemy(rc, robot.location);
                }
            }
        }

        otherLeader = leader;
        if (highestHealth < rc.getHealth() + 1.0 / rc.getID()) {
            rc.setIndicatorString("I'm the leader!");
            leader = null;
        }

        nearbyEnemyHQs = new MapLocation[enemyHQCount];
        for (int i = 0; i < enemyHQCount; i++) {
            nearbyEnemyHQs[i] = tempEnemyHQs[i];
        }
        // cache.updateEnemyCache(nearbyRobots);
        // cache.debugIslandCache();
    }

    public boolean tryAttack() throws GameActionException {
        MapLocation target = getTarget(rc);
        if (target != null && rc.canAttack(target)) {
            rc.attack(target);
            return true;
        }
        return false;
    }

    public boolean tryMove() throws GameActionException {
        MapLocation curr = rc.getLocation();
        if (turnsSinceInCombat >= STAY_IN_COMBAT_TURNS) {
            MapLocation islandTarget = getIslandTarget(rc.getTeam(), cache, true);
            if (islandTarget != null) {
                int health = rc.getHealth();
                if (rc.getHealth() <= HEAL_HEALTH
                        || (health < RobotType.LAUNCHER.health && islandTarget.distanceSquaredTo(curr) <= 8)) {
                    return rc.getLocation().distanceSquaredTo(islandTarget) > 0
                            && snav.tryNavigate(islandTarget, nearbyEnemyHQs);
                }
            }
        }

        if (USE_NEW_MICRO) {
            if (target != null) {
                if (rc.canSenseLocation(target)) {
                    if (curr.isWithinDistanceSquared(target, GIVE_UP_RADIUS_SQ) || targetWeight < GIVE_UP_WEIGHT) {
                        hqLocs.markVisited(target);
                        target = null;
                    } else {
                        RobotInfo robot = rc.senseRobotAtLocation(target);
                        if (robot != null && robot.type == RobotType.HEADQUARTERS) {
                            hqLocs.markVisited(target);
                            target = null;
                        }
                    }
                }
            }

            if (target != null && targetWeight == REPORTED_BASE_WEIGHT) {
                if (nearestReportedEnemy != null && target.distanceSquaredTo(nearestReportedEnemy) > 4) {
                    target = null;
                } else {
                    target = nearestReportedEnemy;
                }
            }

            if (target == null || targetWeight == HQ_LOC_WEIGHT) {
                // target = randomLocation();
                // target = new MapLocation(rc.getMapWidth() - curr.x - 1, rc.getMapHeight() -
                // curr.y - 1);
                // targetWeight = RANDOM_LOC_WEIGHT;
                /*
                 * if (hqTarget == null) { hqTarget = hqLocs.getHQRushLocation(rc); }
                 */
                target = hqLocs.getHQRushLocation(rc);
                // rc.setIndicatorLine(curr, target, 255, 0, 0);
                targetWeight = HQ_LOC_WEIGHT;
            }

            RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

            int attackableEnemies = 0;
            MapLocation nearest = null;
            int nearestDist = 1000000;
            int dist;
            for (RobotInfo enemy : targets) {
                if (enemy.type != RobotType.HEADQUARTERS) {
                    attackableEnemies++;
                    dist = enemy.location.distanceSquaredTo(curr);
                    if (dist < nearestDist) {
                        nearest = enemy.location;
                        nearestDist = dist;
                    }
                }
            }

            if (nearest != null) {
                target = nearest;
                targetWeight = FOUND_BASE_WEIGHT;
            } else {
                if (turnsSinceInCombat >= STAY_IN_COMBAT_TURNS && nearestReportedEnemy != null
                        && nearestReportedEnemy.distanceSquaredTo(curr) < target.distanceSquaredTo(curr)) {
                    target = nearestReportedEnemy;
                    targetWeight = REPORTED_BASE_WEIGHT;
                }
            }

            boolean success = false;

            // targetWeight *= 0.8;

            if (turnsSinceInCombat < STAY_IN_COMBAT_TURNS) {
                success = micro.doMicro(target, otherLeader, prevRoundTargets);
            } else {
                if (leader != null)
                    rc.setIndicatorLine(curr, leader, 0, 255, 0);
                else
                    rc.setIndicatorLine(curr, target, 255, 0, 0);
                if (leader != null && curr.distanceSquaredTo(leader) > LEADER_DIST
                        && (rc.getRoundNum() % 2 == 0 || (attackableEnemies == 0 && rc.isActionReady()))
                        && snav.tryNavigate(leader, nearbyEnemyHQs)) {
                    success = true;
                } else if (curr.distanceSquaredTo(target) > GIVE_UP_RADIUS_SQ && rc.getRoundNum() % 2 == 0
                        && snav.tryNavigate(target, nearbyEnemyHQs)) {
                    success = true;
                }
            }

            return success;
        } else {
            if (target != null) {
                if (curr.isWithinDistanceSquared(target, GIVE_UP_RADIUS_SQ) || targetWeight < GIVE_UP_WEIGHT) {
                    if (target.equals(hqTarget)) {
                        hqTarget = null;
                    }
                    target = null;
                }
            }

            if (target == null) {
                // target = randomLocation();

                // target = new MapLocation(rc.getMapWidth() - curr.x - 1, rc.getMapHeight() -
                // curr.y - 1);
                // targetWeight = RANDOM_LOC_WEIGHT;
                if (hqTarget == null) {
                    hqTarget = hqLocs.getHQRushLocation(rc);
                }
                target = hqTarget;
                targetWeight = RANDOM_LOC_WEIGHT;
            }

            RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo nearest = null;
            int nearestDist = Integer.MAX_VALUE;
            int dist;
            RobotInfo nearestDangerous = null;
            int numAttackingOpponents = 0;
            int nearestDangerousDist = Integer.MAX_VALUE;
            for (RobotInfo target : targets) {
                if (target.type != RobotType.HEADQUARTERS) {
                    dist = target.getLocation().distanceSquaredTo(curr);
                    if (dist < nearestDist) {
                        nearest = target;
                        nearestDist = dist;
                    }
                    if (target.type.damage > 0) {
                        numAttackingOpponents++;
                        if (dist < nearestDangerousDist && target.type.actionRadiusSquared >= dist) {
                            nearestDangerous = target;
                            nearestDangerousDist = dist;
                        }
                    }
                }
            }

            if (nearest != null) {
                target = nearest.getLocation();
                targetWeight = FOUND_BASE_WEIGHT;
            } else {
                if (nearestReportedEnemy != null && turnsSinceInCombat >= STAY_IN_COMBAT_TURNS) {
                    target = nearestReportedEnemy;
                    targetWeight = FOUND_BASE_WEIGHT;
                }
            }

            boolean success = false;

            targetWeight *= 0.8;

            if (nearestDangerous != null) {
                Direction away = nearestDangerous.location.directionTo(curr);
                success = tryFuzzy(away);
            } else {
                if (leader != null && curr.distanceSquaredTo(leader) >= 2
                        && (rc.getRoundNum() % 2 == 0 || numAttackingOpponents == 0)
                        && snav.tryNavigate(leader, nearbyEnemyHQs)) {
                    /*
                     * if (turnsSinceInCombat >= STAY_IN_COMBAT_TURNS) { success =
                     * snav.tryNavigate(leader); } else { success = stnav.tryNavigate(leader); }
                     */
                    success = true;
                } else if ((curr.distanceSquaredTo(target) > RobotType.LAUNCHER.actionRadiusSquared
                        || targets.length == 0) && rc.getRoundNum() % 2 == 0
                        && snav.tryNavigate(target, nearbyEnemyHQs)) {
                    success = true;
                }
            }

            // rc.setIndicatorLine(curr, target, 255, 0, 0);

            return success;
        }

    }

    public MapLocation getTarget(RobotController rc) throws GameActionException {
        if (false && !USE_NEW_MICRO) {
            RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent()); // costs about 100 bytecode
            // cache.updateEnemyCache(targets);
            MapLocation finalTarget = null;
            double maxScore = -1;
            MapLocation curr = rc.getLocation();
            for (RobotInfo target : targets) { // find max score (can optimize for bytecode if needed later)
                if (!target.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                    continue;
                double score = scoreTarget(target);
                if (score > maxScore) {
                    maxScore = score;
                    finalTarget = target.location;
                }
            }
            if (maxScore > 0) {
                return finalTarget;
            }
            if (maxScore > 0 && finalTarget != null) {
                return finalTarget;
            }

            return null;
        } else {
            RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent()); // costs about 100 bytecode
            MapLocation finalTarget = null;
            double maxScore = -1;
            double score;
            MapLocation curr = rc.getLocation();
            for (RobotInfo target : targets) { // find max score (can optimize for bytecode if needed later)
                if (!target.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                    continue;
                score = scoreTarget(target);
                if (score > maxScore) {
                    maxScore = score;
                    finalTarget = target.location;
                }
            }
            if (prevTargets != null) {
                for (RobotInfo target : prevTargets) {
                    if (!target.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                        continue;

                    score = scoreTarget(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = target.location;
                    }
                }
            }

        prevTargets = targets;

            if (maxScore > 0) {
                return finalTarget;
            }

            if (!rc.isMovementReady()) {
                if (nearestReportedEnemy != null && nearestReportedEnemy.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared) && !rc.canSenseLocation(nearestReportedEnemy)) {
                    return nearestReportedEnemy;
                }

                /*for (MapLocation l : rc.getAllLocationsWithinRadiusSquared(curr, RobotType.LAUNCHER.actionRadiusSquared)) {
                    if (!rc.canSenseLocation(l)) {
                        System.out.println("GOT RANDO TARGET");
                        rc.setIndicatorDot(l, 0, 255, 0);
                        return l;
                    } else {
                        rc.setIndicatorDot(l, 200, 0, 0);
                    }
                }
                System.out.println("NO TARGETS?");*/

                MapLocation l;
                l = curr.translate(-4, 0);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-3, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-3, -1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-3, 0);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-3, 1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-3, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, -3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, -1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, 1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-2, 3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-1, -3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-1, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-1, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(-1, 3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(0, -4);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(0, -3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(0, 3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(0, 4);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(1, -3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(1, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(1, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(1, 3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, -3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, -1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, 1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(2, 3);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(3, -2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(3, -1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(3, 0);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(3, 1);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(3, 2);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                l = curr.translate(4, 0);
                if (!rc.canSenseLocation(l) && HQLocations.notPassable.indexOf(l.toString()) < 0) {
                    score = 1.0 / l.distanceSquaredTo(target);
                    if (score > maxScore) {
                        maxScore = score;
                        finalTarget = l;
                    }
                }
                if (maxScore > 0) {
                    return finalTarget;
                }
            }
        }
        return null;
    }

    public double scoreTarget(RobotInfo info) throws GameActionException {
        double score = 0.005 / info.ID;

        switch (info.getType()) {
        case AMPLIFIER:
            score += 1;
            break;
        case BOOSTER:
            score += 5;
            break;
        case CARRIER:
            if (info.getResourceAmount(ResourceType.ADAMANTIUM) + info.getResourceAmount(ResourceType.ELIXIR)
                    + info.getResourceAmount(ResourceType.MANA) > RESOURCE_THRESHOLD) {
                score += 3;
            } else if (info.getTotalAnchors() > 0) {
                score += 10;
            } else {
                score += 1; // could rewrite this to set to 1 then add if conditions are met
            }
            break;
        case DESTABILIZER:
            score += 6;
            break;
        case HEADQUARTERS:
            return 0; // can't attack HQ
        case LAUNCHER:
            score += 6;
            break;
        }

        if (rc.canSenseLocation(info.location) && rc.senseIsland(info.location) != -1) {
            score += ISLAND_MODIFIER;
        }

        score += 1.0 / info.health;

        if (info.getHealth() <= EXECUTE_THRESHOLD) {
            score += EXECUTE_MODIFIER; // could add variable for this too
        }

        return score;
    }
}
