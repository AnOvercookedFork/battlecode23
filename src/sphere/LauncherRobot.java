package sphere;

import battlecode.common.*;
import java.util.ArrayList;

public strictfp class LauncherRobot extends Robot {
    private static final int EXECUTE_THRESHOLD = 6; // increased priority against robots with this hp or under
    private static final int RESOURCE_THRESHOLD = 30; // increased priority on resource holding carriers
    
    public LauncherRobot(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        if(rc.getActionCooldownTurns() == 0) {
            MapLocation target = getTarget(rc);
            if(rc.canAttack(target)) {
                rc.attack(target);
            }
        }
    }
    
    public MapLocation getTarget(RobotController rc) throws GameActionException {
    	RobotInfo[] targets = rc.senseNearbyRobots(16, rc.getTeam().opponent());  // costs about 100 bytecode
    	RobotInfo finalTarget = targets[0];
    	int maxScore = scoreTarget(targets[0]);
    	for(int i = 1; i < targets.length; i++) { // find max score (can optimize for bytecode if needed later)
            int score = scoreTarget(targets[i]);
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
    
    public int scoreTarget(RobotInfo info) {
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
    	
        if(info.getHealth() <= EXECUTE_THRESHOLD) {
            score += 100;   // could add variable for this too
    	}
    	
    	return score;
    }
}
