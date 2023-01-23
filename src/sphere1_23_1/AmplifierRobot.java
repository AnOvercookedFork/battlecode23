package sphere1_23_1;

import battlecode.common.*;

public strictfp class AmplifierRobot extends Robot {
    StinkyNavigation snav;
    MapCache cache;
    MapLocation leader;
    
    public AmplifierRobot(RobotController rc) {
        super(rc);
        snav = new StinkyNavigation(rc);
        cache = new MapCache(rc);
    }

    public void run() throws GameActionException {
        Communications.readArray(rc);
        Communications.incrementAmpCount(rc);
        while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryMove()) {
            processNearbyRobots();
        }
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
                            lowestID = robot.ID;
                            leader = robot.location;
                        }
                        break;
                }
            } else {
                if(robot.type != RobotType.HEADQUARTERS) {
                    Communications.tryAddEnemy(rc, robot.location);
                }
            }
        }
        //cache.updateEnemyCache(nearbyRobots);
    }
    
    public boolean tryMove() throws GameActionException {
        MapLocation curr = rc.getLocation();
        boolean success = false;

        RobotInfo[] targets = rc.senseNearbyRobots(-1);
        RobotInfo nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        int dist;
        for (RobotInfo target : targets) {
            if (target.type != RobotType.HEADQUARTERS && target.team != rc.getTeam()) {
                dist = target.getLocation().distanceSquaredTo(curr);
                if (dist < nearestDist) {
                    nearest = target;
                    nearestDist = dist;
                }
            } else if (nearest == null && target.team == rc.getTeam() && target.type == RobotType.HEADQUARTERS) {
                Communications.tryAddHQ(rc, target.getLocation());
                dist = target.getLocation().distanceSquaredTo(curr);
                if (dist < nearestDist) {
                    nearest = target;
                    nearestDist = dist;
                }
            }
        }

        if (nearest != null) {
            Direction d = nearest.getLocation().directionTo(curr);
            success = tryFuzzy(d);
        } else {
            Direction d = Direction.allDirections()[rng.nextInt(8)];
            success = tryFuzzy(d);
        }
        return success;
    }
}
