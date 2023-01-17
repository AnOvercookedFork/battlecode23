package sphere;

import battlecode.common.*;

public strictfp class LauncherRobot extends Robot {
    public static final double RANDOM_LOC_WEIGHT = 100;
    public static final double INITIAL_LOC_WEIGHT = 100;
    public static final double FOUND_BASE_WEIGHT = 100;
    public static final double GIVE_UP_WEIGHT = 10;
    public static final int GIVE_UP_RADIUS_SQ = 2;
    private static final int EXECUTE_THRESHOLD = 6; // increased priority against robots with this hp or under
    private static final int RESOURCE_THRESHOLD = 30; // increased priority on resource holding carriers
    public static final int EXECUTE_MODIFIER = 100;
    public static final int DAMAGED_MODIFIER = 15;
    public static final int ISLAND_MODIFIER = 0;

    MapLocation target;
    double targetWeight;
    MapLocation leader;
    MapCache cache;
    StinkyNavigation snav;
    MapLocation[] reflectedHQs;
    int hqTargetIndex = 0;

    HQLocations hqLocs;
    MapLocation hqTarget;
    Micro micro;

    RobotInfo[] prevTargets;

    public LauncherRobot(RobotController rc) throws GameActionException {
        super(rc);
        snav = new StinkyNavigation(rc);
        cache = new MapCache(rc, 2, 4, 16);
        hqLocs = new HQLocations(rc);
        hqTarget = null;
        micro = new Micro(rc);
    }

    public void run() throws GameActionException {
        processNearbyRobots();
        prevTargets = null;

        Communications.readArray(rc);
        cache.updateWellCache(rc.senseNearbyWells());
        Communications.readWells(rc, cache);
        Communications.reportWell(rc, cache);
        cache.updateIslandCache();
        Communications.readIslands(rc, cache);
        Communications.reportIsland(rc, cache);
        hqLocs.updateHQSymms(rc);

        while (rc.isActionReady() && tryAttack()) {
        }
        while (rc.isMovementReady() && tryMove()) {
            while (rc.isActionReady() && tryAttack()) {
            }
        }

        //rc.setIndicatorLine(rc.getLocation(), target, 255, 255, 0);

    }

    public void processNearbyRobots() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        Team team = rc.getTeam();
        leader = null;
        int lowestID = rc.getID();

        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == team) {
                switch (robot.type) {
                case LAUNCHER:
                    if (robot.ID < lowestID) {
                        leader = robot.location;
                    }
                    break;
                }
            } else {

            }
        }
        //cache.updateEnemyCache(nearbyRobots);
    }

    public boolean tryAttack() throws GameActionException {
        MapLocation target = getTarget(rc);
        if (target != null && rc.canAttack(target)) {
            rc.attack(target);
            return true;
        }
        MapLocation curr = rc.getLocation();
        int x = curr.x;
        int y = curr.y;
        do {
        if (rc.canAttack(new MapLocation(x + 4, y + 0))) {rc.attack(new MapLocation(x + 4, y + 0));break;}
        if (rc.canAttack(new MapLocation(x + 0, y + 4))) {rc.attack(new MapLocation(x + 0, y + 4));break;}
        if (rc.canAttack(new MapLocation(x + 0, y + -4))) {rc.attack(new MapLocation(x + 0, y + -4));break;}
        if (rc.canAttack(new MapLocation(x + -4, y + 0))) {rc.attack(new MapLocation(x + -4, y + 0));break;}
        if (rc.canAttack(new MapLocation(x + 3, y + 2))) {rc.attack(new MapLocation(x + 3, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + 3, y + -2))) {rc.attack(new MapLocation(x + 3, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + 3))) {rc.attack(new MapLocation(x + 2, y + 3));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + -3))) {rc.attack(new MapLocation(x + 2, y + -3));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + 3))) {rc.attack(new MapLocation(x + -2, y + 3));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + -3))) {rc.attack(new MapLocation(x + -2, y + -3));break;}
        if (rc.canAttack(new MapLocation(x + -3, y + 2))) {rc.attack(new MapLocation(x + -3, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + -3, y + -2))) {rc.attack(new MapLocation(x + -3, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + 3, y + 1))) {rc.attack(new MapLocation(x + 3, y + 1));break;}
        if (rc.canAttack(new MapLocation(x + 3, y + -1))) {rc.attack(new MapLocation(x + 3, y + -1));break;}
        if (rc.canAttack(new MapLocation(x + 1, y + 3))) {rc.attack(new MapLocation(x + 1, y + 3));break;}
        if (rc.canAttack(new MapLocation(x + 1, y + -3))) {rc.attack(new MapLocation(x + 1, y + -3));break;}
        if (rc.canAttack(new MapLocation(x + -1, y + 3))) {rc.attack(new MapLocation(x + -1, y + 3));break;}
        if (rc.canAttack(new MapLocation(x + -1, y + -3))) {rc.attack(new MapLocation(x + -1, y + -3));break;}
        if (rc.canAttack(new MapLocation(x + -3, y + 1))) {rc.attack(new MapLocation(x + -3, y + 1));break;}
        if (rc.canAttack(new MapLocation(x + -3, y + -1))) {rc.attack(new MapLocation(x + -3, y + -1));break;}
        if (rc.canAttack(new MapLocation(x + 3, y + 0))) {rc.attack(new MapLocation(x + 3, y + 0));break;}
        if (rc.canAttack(new MapLocation(x + 0, y + 3))) {rc.attack(new MapLocation(x + 0, y + 3));break;}
        if (rc.canAttack(new MapLocation(x + 0, y + -3))) {rc.attack(new MapLocation(x + 0, y + -3));break;}
        if (rc.canAttack(new MapLocation(x + -3, y + 0))) {rc.attack(new MapLocation(x + -3, y + 0));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + 2))) {rc.attack(new MapLocation(x + 2, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + -2))) {rc.attack(new MapLocation(x + 2, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + 2))) {rc.attack(new MapLocation(x + -2, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + -2))) {rc.attack(new MapLocation(x + -2, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + 1))) {rc.attack(new MapLocation(x + 2, y + 1));break;}
        if (rc.canAttack(new MapLocation(x + 2, y + -1))) {rc.attack(new MapLocation(x + 2, y + -1));break;}
        if (rc.canAttack(new MapLocation(x + 1, y + 2))) {rc.attack(new MapLocation(x + 1, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + 1, y + -2))) {rc.attack(new MapLocation(x + 1, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + -1, y + 2))) {rc.attack(new MapLocation(x + -1, y + 2));break;}
        if (rc.canAttack(new MapLocation(x + -1, y + -2))) {rc.attack(new MapLocation(x + -1, y + -2));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + 1))) {rc.attack(new MapLocation(x + -2, y + 1));break;}
        if (rc.canAttack(new MapLocation(x + -2, y + -1))) {rc.attack(new MapLocation(x + -2, y + -1));break;}
        } while (false);
        return false;
    }

    public boolean tryMove() throws GameActionException {
        MapLocation curr = rc.getLocation();
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
        /*RobotInfo nearest = null;
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
        }*/

        boolean areAttackableEnemies = false;
        for (RobotInfo enemy : targets) {
            if (enemy.type != RobotType.HEADQUARTERS) {
                areAttackableEnemies = true;
                target = enemy.location;
                targetWeight = FOUND_BASE_WEIGHT;
                break;
            }
        }

        boolean success = false;

        targetWeight *= 0.8;

        if (areAttackableEnemies) {
            success = micro.doMicro();
            if (success) {
                snav.reset();
            }
        } else {
            if (leader != null && curr.distanceSquaredTo(leader) >= 2
                    && rc.getRoundNum() % 2 == 0 && snav.tryNavigate(leader)) {
                success = true;
            } else if (curr.distanceSquaredTo(target) > RobotType.LAUNCHER.actionRadiusSquared
                    && rc.getRoundNum() % 2 == 0 && snav.tryNavigate(target)) {
                success = true;
            }
        }

        return success;

    }

    public MapLocation getTarget(RobotController rc) throws GameActionException {
        RobotInfo[] targets = rc.senseNearbyRobots(-1, rc.getTeam().opponent()); // costs about 100 bytecode
        //cache.updateEnemyCache(targets);
        MapLocation finalTarget = null;
        int maxScore = -1;
        MapLocation curr = rc.getLocation();
        for (RobotInfo target : targets) { // find max score (can optimize for bytecode if needed later)
            if (!target.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                continue;
            int score = scoreTarget(target, rc);
            if (score > maxScore) {
                maxScore = score;
                finalTarget = target.location;
            }
        }
        /*int round = rc.getRoundNum();
        for (MapCache.EnemyData enemy : cache.enemyCache) {
            if (enemy == null || enemy.roundSeen < round
                    || !enemy.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                continue;
            if (enemy.priority > maxScore) {
                maxScore = enemy.priority;
                finalTarget = enemy.location;
            }
        }*/
        if (prevTargets != null) {
            for (RobotInfo target : prevTargets) {
                if (target.type == RobotType.HEADQUARTERS) {

                }
                if (!target.location.isWithinDistanceSquared(curr, RobotType.LAUNCHER.actionRadiusSquared))
                    continue;
                
                int score = scoreTarget(target, rc);
                if (score > maxScore) {
                    maxScore = score;
                    finalTarget = target.location;
                }
            }
        }

        prevTargets = targets;

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
            if (info.getResourceAmount(ResourceType.ADAMANTIUM) + info.getResourceAmount(ResourceType.ELIXIR)
                    + info.getResourceAmount(ResourceType.MANA) > RESOURCE_THRESHOLD) {
                score = 3;
            } else {
                score = 1; // could rewrite this to set to 1 then add if conditions are met
            }
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

        if (rc.canSenseLocation(info.location) && rc.senseIsland(info.location) != -1) {
            score += ISLAND_MODIFIER;
        }

        if (info.getHealth() < info.getType().getMaxHealth()) {
            score += DAMAGED_MODIFIER;
        }

        if (info.getHealth() <= EXECUTE_THRESHOLD) {
            score += EXECUTE_MODIFIER; // could add variable for this too
        }

        return score;
    }
}
