package klein_bottle2_2_1;

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
        if (lastTarget == null || target.distanceSquaredTo(lastTarget) > 0) {
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
        } else if (lastObstacle == null) {
            MapLocation dest;
            Direction l = d;
            MapLocation destLeft = null;
            MapLocation loLeft = curr.add(d);
            for (int i = 7; i-->0;) {
                l = l.rotateLeft();
                dest = curr.add(l);
                if (!rc.onTheMap(dest)) {
                    break;
                }
                if (canMove(l, dest)) {
                    destLeft = dest;
                    break;
                } else if (!rc.sensePassability(dest)) {
                    loLeft = dest;
                }
            }
            Direction r = d;
            MapLocation destRight = null;
            MapLocation loRight = curr.add(d);
            for (int i = 7; i-->0;) {
                r = r.rotateRight();
                dest = curr.add(r);
                if (!rc.onTheMap(dest)) {
                    break;
                }
                if (canMove(r, dest)) {
                    destRight = dest;
                    break;
                } else if (!rc.sensePassability(dest)) {
                    loRight = dest;
                }
            }
            if (destLeft == null) {
                if (destRight != null) {
                    rc.move(r);
                    if (loRight != null) {
                        right = true;
                        lastObstacle = loRight;
                    }
                    return true;
                } else {
                    return false;
                }
            } else {
                if (destRight != null) {
                    if (destRight.distanceSquaredTo(target) <= destLeft.distanceSquaredTo(target)) {
                        rc.move(r);
                        if (loRight != null) {
                            right = true;
                            lastObstacle = loRight;
                        }
                        return true;
                    } else {
                        rc.move(l);
                        if (loLeft != null) {
                            right = false;
                            lastObstacle = loLeft;
                        }
                        return true;
                    }
                } else {
                    rc.move(l);
                    if (loLeft != null) {
                        right = false;
                        lastObstacle = loLeft;
                    }
                    return true;
                }
            }
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
                } else if (!rc.sensePassability(dest)) {
                    lastObstacle = dest;
                }
            }
        }

        return false;

    }

}
