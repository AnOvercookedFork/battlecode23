package sphere;

import battlecode.common.*;

public strictfp class HeadquartersRobot extends Robot {

    int turns = 0;

    public HeadquartersRobot(RobotController rc) {
        super(rc);
    }

    public void run() throws GameActionException {
        processArea();
        
        MapLocation curr = rc.getLocation();
        MapLocation l;

        if (rc.canBuildAnchor(Anchor.STANDARD) && turns > 100) {
            rc.buildAnchor(Anchor.STANDARD);
        }
        if (turns < 100 || rc.canBuildAnchor(Anchor.STANDARD)) {
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

        turns++;

    }
}
