package sphere;

import battlecode.common.*;

public strictfp class HeadquartersRobot extends Robot {
    
    public HeadquartersRobot(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        MapLocation curr = rc.getLocation();
        for (Direction d : directions) {
            if (rc.canBuildRobot(RobotType.CARRIER, curr.add(d))) {
                rc.buildRobot(RobotType.CARRIER, curr.add(d));
            }
        }

    }
}
