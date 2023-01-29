package torus1_28_3_navchanges;

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
        is_bugging = false;
    }

    public void reset() {
        is_bugging = false;
        lastObstacle = null;
        visited = new StringBuilder();
    }

    public boolean canMove(Direction d, MapLocation dest) throws GameActionException {
        //if (visited.indexOf(dest.add(rc.senseMapInfo(dest).getCurrentDirection()).toString()) >= 0) {
        /*if (visited.indexOf(dest.toString()) >= 0) {
            return false;
        }*/
        if (rc.senseMapInfo(dest).getCurrentDirection() != Direction.CENTER) return false;
        return rc.canMove(d);
    }

    public boolean tryNavigate(MapLocation target) throws GameActionException {
        MapLocation[] hqs = new MapLocation[1];
        return tryNavigate(target, hqs);
    }

    public boolean tryNavigate(MapLocation target, MapLocation[] hqs) throws GameActionException {

        MapLocation curr = rc.getLocation();
        int dist = curr.distanceSquaredTo(target);

        if (lastTarget == null || target.distanceSquaredTo(lastTarget) > 2) {
            reset();
            minDist = dist;
        }

        lastTarget = target;

        if (dist < minDist) {reset(); minDist = dist;}

        if (!is_bugging) {
            Direction r = curr.directionTo(target);
            MapLocation next;
            Direction best = null;
            int score;
            int bestScore = curr.distanceSquaredTo(target);
            if (rc.canMove(r)) {
                next = curr.add(r);
                next = next.add(rc.senseMapInfo(next).getCurrentDirection());
                score = next.distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    best = r;
                }
            }
            Direction l = r.rotateLeft();
            r = r.rotateRight();
            if (rc.canMove(r)) {
                next = curr.add(r);
                next = next.add(rc.senseMapInfo(next).getCurrentDirection());
                score = next.distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    best = r;
                }
            }
            if (rc.canMove(l)) {
                next = curr.add(l);
                next = next.add(rc.senseMapInfo(next).getCurrentDirection());
                score = next.distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    best = l;
                }
            }
            r = r.rotateRight();
            l = l.rotateLeft();
            if (rc.canMove(r)) {
                next = curr.add(r);
                next = next.add(rc.senseMapInfo(next).getCurrentDirection());
                score = next.distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    best = r;
                }
            }
            if (rc.canMove(l)) {
                next = curr.add(l);
                next = next.add(rc.senseMapInfo(next).getCurrentDirection());
                score = next.distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    best = l;
                }
            }
            if (best != null) {
                rc.move(best);
                return true;
            } else {
                reset();
                minDist = curr.distanceSquaredTo(target);
                rc.setIndicatorString("started bugging");
                is_bugging = true;
            }
        }

        String currStr = curr.toString();

        if (visited.indexOf(currStr) >= 0) {
            reset();
            minDist = curr.distanceSquaredTo(target);
            is_bugging = true;
        }
        visited.append(currStr).append("|");

        Direction d;
        if (lastObstacle == null) {
            d = curr.directionTo(target);
        } else {
            rc.setIndicatorLine(curr, lastObstacle, 30, 30, 30);
            d = curr.directionTo(lastObstacle);
        }

        if (canMove(d, curr.add(d))) {
            rc.setIndicatorString("canMove, reset!");
            reset();
            minDist = curr.distanceSquaredTo(target);
            is_bugging = true;
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
