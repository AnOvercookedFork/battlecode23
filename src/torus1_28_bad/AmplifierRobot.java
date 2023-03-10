package torus1_28_bad;

import battlecode.common.*;

public strictfp class AmplifierRobot extends Robot {
    StinkyNavigation snav;
    MapCache cache;
    MapLocation leader;
    MapLocation[] enemyHQs;

    public AmplifierRobot(RobotController rc) {
        super(rc);
        snav = new StinkyNavigation(rc);
        cache = new MapCache(rc);
    }

    public void run() throws GameActionException {
        
        Communications.readArray(rc);
        Communications.incrementAmpCount(rc);
        while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryMove()) {
//            processNearbyRobots(); // easier to do this in try move, since movement will depend entirely on where robots are
        }
        Communications.readWells(rc, cache);
        Communications.reportWell(rc, cache);
        cache.updateIslandCache();
        Communications.readIslands(rc, cache);
        Communications.reportIsland(rc, cache);
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
                if (robot.type != RobotType.HEADQUARTERS) {
                    Communications.tryAddEnemy(rc, robot.location);
                }
            }
        }

    }

    public boolean tryMove() throws GameActionException {
        MapLocation curr = rc.getLocation();
        Team team = rc.getTeam();
        leader = null;
        int lowestID = Integer.MAX_VALUE;
        boolean success = false;

        RobotInfo[] targets = rc.senseNearbyRobots(-1);
        RobotInfo nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        int dist;
        RobotInfo nearestEnemy = null;
        int nearestEnemyDist = Integer.MAX_VALUE;
        MapLocation[] tempEnemyHQs = new MapLocation[4];
        int enemyHQCt = 0;
        for (RobotInfo target : targets) {
            if(target.team == team) {
                switch(target.type) {
                case HEADQUARTERS:
                case AMPLIFIER:
                    dist = target.getLocation().distanceSquaredTo(curr);
                    if (dist < nearestDist) {
                        nearest = target;
                        nearestDist = dist;
                    }
                    break;
                case LAUNCHER:
                    if (target.ID < lowestID) {
                        lowestID = target.ID;
                        leader = target.location;
                    }
                    break;
                }
                // should try to avoid allied hqs and amps
            }
            else {
                if (target.type != RobotType.HEADQUARTERS) {
                    dist = target.getLocation().distanceSquaredTo(curr);
                    if (dist < nearestEnemyDist) {
                        nearestEnemy = target;
                        nearestEnemyDist = dist;
                    }
                }
                else {
                    tempEnemyHQs[enemyHQCt] = target.getLocation();
                    enemyHQCt++;
                }
            }

        }
        
        enemyHQs = new MapLocation[enemyHQCt];
        for(int i = 0; i < enemyHQCt; i++) {
            enemyHQs[i] = tempEnemyHQs[i];
        }
        
        if (nearestEnemy != null) {
            rc.setIndicatorString("fleeing");
            Direction d = nearestEnemy.getLocation().directionTo(curr);
            success = tryFuzzy(d);
        }
        else if (leader != null) {
            success = snav.tryNavigate(leader, enemyHQs);
            rc.setIndicatorString("following leader");
        }
        else if (nearest != null) {
            Direction d = nearest.getLocation().directionTo(curr);
            success = tryFuzzy(d);
            rc.setIndicatorString("leaving allied amp");
        } 
        else {
            
            Direction d = Direction.allDirections()[rng.nextInt(8)];
            success = tryFuzzy(d);
            rc.setIndicatorString("moving randomly");
        }
        return success;
    }
}
