package torus1_25_3;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.HashSet;

public strictfp class HeadquartersRobot extends Robot {

    int turns = 0;

    public static final int MAX_ANCHORS = 1;
    public static final int MIN_TURN_BUILD_HEAL_ANCHOR = 300;
    public static final int ANCHOR_BUILD_COOLDOWN = 50;
    public static final int MIN_TURN_BUILD_ANCHOR = 750;
    public static final int CARRIER_SATURATION = 50;
    public static final int MAX_ENEMIES_TO_REPORT = 6;
    public static final int MANA_POOL_AMOUNT = 4 * RobotType.LAUNCHER.buildCostMana;

    MapLocation farthestLauncher;
    int enemiesNearby = 0;
    int carriersNearby = 0;
    int launchersNearby = 0;
    ArrayList<Integer> resources;
    HashSet<Integer> resourcesSet;
    int carouselIndex = 0;
    int anchorBuildCooldown = 0;
    boolean pool_mana = false;
    MapLocation nearestDangerous = null;
    HQLocations hqLocs;

    MapCache cache;
    
    
    public HeadquartersRobot(RobotController rc) throws GameActionException {
        super(rc);
        Communications.canAlwaysWrite = true;
        cache = new MapCache(rc, 32);
        Communications.readArray(rc);
        Communications.tryAddHQ(rc, rc.getLocation());
        hqLocs = new HQLocations(rc);
    }

    public void run() throws GameActionException {

        Communications.readArray(rc);
        hqLocs.updateHQSymms(rc);
        hqLocs.updateSymmsFromComms();
        System.out.println("IS HORIZONTAL ELIMINATED? " + Communications.isHorizontalSymmetryEliminated(rc));
        System.out.println("IS VERTICAL ELIMINATED? " + Communications.isVerticalSymmetryEliminated(rc));
        System.out.println("IS ROTATIONAL ELIMINATED? " + Communications.isRotationalSymmetryEliminated(rc));
        
        
        if (Communications.isFirstHQ(rc)) {
        }
        cache.updateWellCache(rc.senseNearbyWells());
        cache.updateIslandCache();
        
        if (Communications.isFirstHQ(rc)) {
            primaryComms();

        } else {
            Communications.readWells(rc, cache);
        }

        processNearbyRobots();
        
        MapLocation curr = rc.getLocation();
        MapLocation l;

        boolean buildAnchor = shouldBuildAnchor();

        if (rc.canBuildAnchor(Anchor.STANDARD) && buildAnchor) {
            rc.buildAnchor(Anchor.STANDARD);
            anchorBuildCooldown = ANCHOR_BUILD_COOLDOWN;
        }

        while ((!buildAnchor || rc.getResourceAmount(ResourceType.MANA) >= RobotType.LAUNCHER.buildCostMana + Anchor.STANDARD.manaCost) && (!pool_mana || rc.getResourceAmount(ResourceType.MANA) > MANA_POOL_AMOUNT) && tryBuildLauncher()) {}

        while ((!buildAnchor || rc.getResourceAmount(ResourceType.ADAMANTIUM) >= RobotType.CARRIER.buildCostAdamantium + Anchor.STANDARD.adamantiumCost)
                && carriersNearby < CARRIER_SATURATION
                && tryBuildCarrier()) {}

        turns++;

        if (anchorBuildCooldown > 0) anchorBuildCooldown--;

        cache.debugIslandCache();

    }

    public void processNearbyRobots() throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        MapLocation curr = rc.getLocation();
        farthestLauncher = null;
        int farthestLauncherDist = 0;
        int dist;
        carriersNearby = 0;
        enemiesNearby = 0;
        launchersNearby = 0;
        MapLocation enemyLoc = null;
        nearestDangerous = null;
        int nearestDangerousDist = 1000000;
        int enemiesReported = 0;
        int dangerousEnemies = 0;
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == rc.getTeam()) {
                switch (robot.type) {
                    case LAUNCHER:
                        dist = robot.location.distanceSquaredTo(curr);
                        if (dist > farthestLauncherDist) {
                            farthestLauncherDist = dist;
                            farthestLauncher = robot.location;
                        }
                        launchersNearby++;
                        break;
                    case CARRIER:
                        carriersNearby++;
                        break;
                }
            } else {
                // process enemies here
                if (robot.type != RobotType.HEADQUARTERS) {
                    if (robot.type.damage > 0) {
                        dangerousEnemies++;
                        dist = robot.location.distanceSquaredTo(curr);
                        if (dist < nearestDangerousDist) {
                            nearestDangerousDist = dist;
                            nearestDangerous = robot.location;
                        }
                    }
                    enemiesNearby++;
                    if (enemiesReported < MAX_ENEMIES_TO_REPORT) {
                        Communications.tryAddEnemy(rc, robot.location);
                        enemiesReported++;
                    }
                }
            }
        }

        if (launchersNearby == 0 && dangerousEnemies > 0) {
            pool_mana = true;
        } else {
            pool_mana = false;
        }
        
        /*if(enemyLoc != null) {
            Communications.panicReportEnemy(rc, enemyLoc);
        }*/
        //cache.updateEnemyCache(nearbyRobots);
    }

    // tasks for the first HQ to do for comms
    public void primaryComms() throws GameActionException {
        Communications.readReportingWells(rc, cache);
        Communications.cycleWells(rc, cache);
        Communications.readReportingIslands(rc, cache);
        Communications.cycleIslands(rc, cache);
        Communications.updateAmpCount(rc);
        //Communications.clearReportEnemy(rc);
        Communications.debugEnemies(rc);
        Communications.ageEnemies(rc);
        //cache.debugWellCache();
    }
    
    public boolean shouldBuildAnchor() {
        boolean hasHealAnchor = false;
        Team team = rc.getTeam();

        for (MapCache.IslandData idata : cache.islandCache) {
            if (idata != null && idata.team == team) {
                hasHealAnchor = true;
                break;
            }
        }
        return rc.getNumAnchors(Anchor.STANDARD) + rc.getNumAnchors(Anchor.ACCELERATING) < MAX_ANCHORS 
            && (turns > MIN_TURN_BUILD_ANCHOR
                || (turns > MIN_TURN_BUILD_HEAL_ANCHOR && !hasHealAnchor))
            && anchorBuildCooldown == 0
            && carriersNearby >= 1
            && launchersNearby >= 1
            && enemiesNearby == 0;
    }

    public MapLocation closestBuildLocation(RobotType type, MapLocation target) throws GameActionException {
        MapLocation curr = rc.getLocation();
        MapLocation best = null; int bestDist = Integer.MAX_VALUE; MapLocation l; int dist;
        l = curr.translate(-3, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, -2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, -1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, -2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, -1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -3);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 3);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, -2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, -1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, -2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, -1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 1);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 2);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(3, 0);
        dist = l.distanceSquaredTo(target);
        if (dist < bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        return best;
    }

    public MapLocation farthestBuildLocation(RobotType type, MapLocation target) throws GameActionException {
        MapLocation curr = rc.getLocation();
        MapLocation best = null; int bestDist = 0; MapLocation l; int dist;
        l = curr.translate(-3, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, -2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, -1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-2, 2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, -2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, -1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(-1, 2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -3);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, -1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(0, 3);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, -2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, -1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(1, 2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, -2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, -1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 1);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(2, 2);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        l = curr.translate(3, 0);
        dist = l.distanceSquaredTo(target);
        if (dist > bestDist && rc.canBuildRobot(type, l)) {
            bestDist = dist; best = l;
        }
        return best;
    }

    public boolean tryBuildLauncher() throws GameActionException {

        MapLocation leader = null;
        double highestHealth = 0;
        double health;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : allies) {
            if (ally.type == RobotType.LAUNCHER) {
                health = ally.health + 1.0 / ally.ID;
                if (health > highestHealth) {
                    highestHealth = health;
                    leader = ally.location;
                }
            }
        }

        MapLocation target;
        boolean close = true;
        if (leader != null) {
            target = leader;
        } else if (nearestDangerous != null) {
            target = nearestDangerous;
            close = false;
        } else {
            target = randomLocation();
        }
        MapLocation chosenBuildLocation;
        if (close) {
            chosenBuildLocation = closestBuildLocation(RobotType.LAUNCHER, target);
        } else {
            chosenBuildLocation = farthestBuildLocation(RobotType.LAUNCHER, target);
        }
        if (chosenBuildLocation != null) {
            rc.buildRobot(RobotType.LAUNCHER, chosenBuildLocation);
            pool_mana = false;
            return true;
        }
        return false;
    }

    public boolean tryBuildCarrier() throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        MapLocation target;
        if (wells.length > 0) {
            int idx = rng.nextInt(wells.length);
            target = wells[idx].getMapLocation();
        } else {
            target = randomLocation();
        }
        MapLocation chosenBuildLocation = closestBuildLocation(RobotType.CARRIER, target);
        if (chosenBuildLocation != null) {
            rc.buildRobot(RobotType.CARRIER, chosenBuildLocation);
            return true;
        }
        return false;
    }
}
