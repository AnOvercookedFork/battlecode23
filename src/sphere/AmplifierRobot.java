package sphere;

import battlecode.common.*;

public strictfp class AmplifierRobot extends Robot {

    public AmplifierRobot(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        Communications.readArray(rc);
        while (rc.getMovementCooldownTurns() < GameConstants.COOLDOWN_LIMIT && tryMove()) {
            processArea();
        }
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
