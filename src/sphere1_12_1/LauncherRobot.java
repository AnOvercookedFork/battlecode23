package sphere1_12_1;

import battlecode.common.*;

public strictfp class LauncherRobot extends Robot {
    private static final int EXECUTE_THRESHOLD = 6; // increased priority against robots with this hp or under
    private static final int RESOURCE_THRESHOLD = 30; // increased priority on resource holding carriers
    
    MapLocation target;
    double targetWeight;
    MapLocation leader;
    StinkyNavigation snav;

    public static final double RANDOM_LOC_WEIGHT = 100;
    public static final double INITIAL_LOC_WEIGHT = 100;
    public static final double FOUND_BASE_WEIGHT = 100;
    public static final double GIVE_UP_WEIGHT = 10;
    public static final int GIVE_UP_RADIUS_SQ = 2;

    public LauncherRobot(RobotController rc) {
        super(rc);
        target = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
        targetWeight = INITIAL_LOC_WEIGHT;
        snav = new StinkyNavigation(rc);
    }

    public void run() throws GameActionException {
        processNearbyRobots();

        while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                && tryAttack()) {}
        while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                && tryMove()) {
            while (rc.getActionCooldownTurns() < GameConstants.COOLDOWN_LIMIT
                    && tryAttack()) {}
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

            }
        }
    }

    public boolean tryAttack() throws GameActionException {
        MapLocation target = getTarget(rc);
        if(target != null && rc.canAttack(target)) {
            rc.attack(target);
            return true;
        }
        return false;
    }

    public boolean tryMove() throws GameActionException {
        MapLocation curr = rc.getLocation();
        if (target != null) {
            if (curr.isWithinDistanceSquared(target, GIVE_UP_RADIUS_SQ)
                    || targetWeight < GIVE_UP_WEIGHT) {
                target = null;
            }
        }

        if (target == null) {
            target = randomLocation();
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
                    if (dist < nearestDangerousDist
                            && target.type.actionRadiusSquared >= dist) {
                        nearestDangerous = target;
                        nearestDangerousDist = dist;
                    }
                }
            }
        }
        
        if (nearest != null) {
            target = nearest.getLocation();
            targetWeight = FOUND_BASE_WEIGHT;
        }

        boolean success = false;

        targetWeight *= 0.8;

        if (nearestDangerous != null) {
            Direction away = nearestDangerous.location.directionTo(curr);
            tryFuzzy(away);
            success = true;
        } else {
            if (leader != null && curr.distanceSquaredTo(leader) >= 2
                    && (rc.getRoundNum() % 2 == 0 || numAttackingOpponents == 0)
                    && snav.tryNavigate(leader)) {
                success = true;
            } else if (curr.distanceSquaredTo(target) > RobotType.LAUNCHER.actionRadiusSquared
                    && rc.getRoundNum() % 2 == 0 && snav.tryNavigate(target)) {
                success = true;
            }
        }
        
        return success;

    }
    
    public MapLocation getTarget(RobotController rc) throws GameActionException {
    	RobotInfo[] targets = rc.senseNearbyRobots(RobotType.LAUNCHER.actionRadiusSquared, rc.getTeam().opponent());  // costs about 100 bytecode
    	RobotInfo finalTarget = null;
        int maxScore = -1;
        for(int i = 0; i < targets.length; i++) { // find max score (can optimize for bytecode if needed later)
            int score = scoreTarget(targets[i], rc);
            if(score > maxScore) {
                maxScore = score;
                finalTarget = targets[i];
            }
    	}
    	if(maxScore > 0) {
    	    return finalTarget.location;
    	}
    	else {
    	    return null; // maybe?
    	}
    }
    
    public int scoreTarget(RobotInfo info, RobotController rc) throws GameActionException {
        int score = 0;
        
        switch(info.getType()) {
            case AMPLIFIER:
                score = 1;
                break;
            case BOOSTER:
                score = 5;
                break;
            case CARRIER:
                if(info.getTotalAnchors() > 0) {
                    score = 4;
                }
                else if(info.getResourceAmount(ResourceType.ADAMANTIUM) + info.getResourceAmount(ResourceType.ELIXIR) + info.getResourceAmount(ResourceType.MANA) > RESOURCE_THRESHOLD){
                    score = 3;
                }
                else {
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
    	
        
        if(rc.senseIsland(info.location) != -1) {
        	score += 10;
        }
        
        if(info.getHealth() < info.getType().getMaxHealth()) {
        	score += 15;
        }
        
        if(info.getHealth() <= EXECUTE_THRESHOLD) {
            score += 100;   // could add variable for this too
    	}
    	
    	return score;
    }
}
