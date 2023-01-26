package torus1_25_3;

import battlecode.common.*;

public strictfp class BugNavigation {

    RobotController rc;
    MapLocation lastTarget;

    boolean is_bugging;
    boolean right;
    int minDist;
    MapLocation lastObstacle;
    StringBuilder visited;

    public BugNavigation(RobotController rc) {
        this.rc = rc;
        lastTarget = null;
        right = true;
    }

    public void reset() {
        is_bugging = false;
        minDist = 1000000;
        lastObstacle = null;
        visited = new StringBuilder();
    }

    public boolean canMove(Direction d, MapLocation dest) throws GameActionException {
        if (visited.indexOf(dest.add(rc.senseMapInfo(dest).getCurrentDirection()).toString()) >= 0) {
            return false;
        }
        return rc.canMove(d);
    }

    public boolean tryNavigate(MapLocation target) throws GameActionException {
        MapLocation[] hqs = new MapLocation[1];
        return tryNavigate(target, hqs);
    }

    public boolean tryNavigate(MapLocation target, MapLocation[] hqs) throws GameActionException {
        if (lastTarget == null || target.distanceSquaredTo(lastTarget) > 2) {
            reset();
        }

        lastTarget = target;

        MapLocation curr = rc.getLocation();
        String currStr = curr.toString();
        int dist = curr.distanceSquaredTo(target);

        if (dist < minDist || visited.indexOf(currStr) >= 0) {reset(); minDist = dist;}
        visited.append(currStr).append("|");

        Direction d;
        if (lastObstacle == null) {
            d = curr.directionTo(target);
        } else {
            d = curr.directionTo(lastObstacle);
        }

        if (canMove(d, curr.add(d))) {
            reset();
            rc.move(d);
            return true;
        }
        
        MapLocation dest;
        for (int i = 16; i-- > 0;) {
            if (right) d = d.rotateRight();
            else d = d.rotateLeft();
            dest = curr.add(d);
            if (!rc.onTheMap(dest)) right = !right;
            else {
                if (canMove(d, dest)) {
                    rc.move(d);
                    return true;
                }
                lastObstacle = dest;
            }

        }

        return false;

    }

}
