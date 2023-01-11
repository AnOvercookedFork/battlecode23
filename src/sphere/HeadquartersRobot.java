package sphere;

import battlecode.common.*;

public strictfp class HeadquartersRobot extends Robot {
    
    public HeadquartersRobot(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        processArea();
        
        MapLocation curr = rc.getLocation();
        MapLocation l;
        for (Direction d : directions) {
            l = curr.add(d);
            if (rc.canBuildRobot(RobotType.CARRIER, l)) {
                rc.buildRobot(RobotType.CARRIER, l);
            }
        }
        for (Direction d : directions) {
            l = curr.add(d);
            if (rc.canBuildRobot(RobotType.LAUNCHER, curr.add(d))) {
                rc.buildRobot(RobotType.LAUNCHER, l);
            }
        }

    }
}
