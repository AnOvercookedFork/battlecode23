package sphere1_14_2;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.HashSet;

public strictfp class HeadquartersRobot extends Robot {

    int turns = 0;

    public static final int MAX_ANCHORS = 1;
    public static final int MIN_TURN_BUILD_ANCHOR = 750;

    MapLocation farthestLauncher;
    int enemiesNearby = 0;
    int carriersNearby = 0;
    int launchersNearby = 0;
    ArrayList<Integer> resources;
    HashSet<Integer> resourcesSet;
    int carouselIndex = 0;

    MapCache cache;
    
    
    public HeadquartersRobot(RobotController rc) throws GameActionException {
        super(rc);
        Communications.canAlwaysWrite = true;
        cache = new MapCache(rc, 32, 35, 16);
        Communications.readArray(rc);
        Communications.tryAddHQ(rc, rc.getLocation());
    }

    public void run() throws GameActionException {
        Communications.readArray(rc);

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
        }

        if (!buildAnchor || rc.getResourceAmount(ResourceType.ADAMANTIUM) >= RobotType.CARRIER.buildCostAdamantium + Anchor.STANDARD.adamantiumCost) {
            tryBuildCarrier();
        }
        if (!buildAnchor || rc.getResourceAmount(ResourceType.MANA) >= RobotType.LAUNCHER.buildCostMana + Anchor.STANDARD.manaCost) {
            tryBuildLauncher();
        }

        turns++;

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
                    enemiesNearby++;
                }
            }
        }

        cache.updateEnemyCache(nearbyRobots);
    }

    // tasks for the first HQ to do for comms
    public void primaryComms() throws GameActionException {
        Communications.readReportingWells(rc, cache);
        Communications.cycleWells(rc, cache);
        Communications.readReportingIslands(rc, cache);
        Communications.cycleIslands(rc, cache);
        Communications.updateAmpCount(rc);
        cache.debugWellCache();
    }
    
    public boolean shouldBuildAnchor() {
        return rc.getNumAnchors(Anchor.STANDARD) + rc.getNumAnchors(Anchor.ACCELERATING) < MAX_ANCHORS && turns > MIN_TURN_BUILD_ANCHOR && carriersNearby >= 1 && launchersNearby >= 1 && enemiesNearby == 0;
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

    public boolean tryBuildLauncher() throws GameActionException {
        MapLocation target;
        if (farthestLauncher != null) {
            target = farthestLauncher;
        } else {
            target = randomLocation();
        }
        MapLocation chosenBuildLocation = closestBuildLocation(RobotType.LAUNCHER, target);
        if (chosenBuildLocation != null) {
            rc.buildRobot(RobotType.LAUNCHER, chosenBuildLocation);
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
