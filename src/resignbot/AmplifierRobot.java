package resignbot;

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
            if(target.team == rc.getTeam()) {
                // should try to avoid allied hqs and amps
                if (target.type == RobotType.HEADQUARTERS || target.type == RobotType.AMPLIFIER) {
                    dist = target.getLocation().distanceSquaredTo(curr);
                    if (dist < nearestDist) {
                        nearest = target;
                        nearestDist = dist;
                    }
                }
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
        
        if (nearestEnemyDist <= 25) {
            Direction d = nearestEnemy.getLocation().directionTo(curr);
            success = tryFuzzy(d);
        }
        else if (nearest != null) {
            Direction d = nearest.getLocation().directionTo(curr);
            success = tryFuzzy(d);
        } 
        else if (leader != null) {
            success = snav.tryNavigate(leader, enemyHQs);
        }
        else {
            
            Direction d = Direction.allDirections()[rng.nextInt(8)];
            success = tryFuzzy(d);
        }
        return success;
    }
}
