package klein_bottle2_02_2;

import battlecode.common.*;

public strictfp class BugNavigation {

    static RobotController rc;
    static MapLocation lastTarget;

    static boolean is_bugging;
    static boolean right;
    static int minDist;
    static MapLocation lastObstacle;
    static StringBuilder visited;

    static int maxHQdist;
    static MapLocation[] enemyHQs;
    public static final int INF = 1000000;
    static int[] HQdists;
    static boolean[] isPassables;
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER
    };
    static final Direction[] directionsNoCenter = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    public BugNavigation(RobotController rc) {
        this.rc = rc;
        lastTarget = null;
        right = true;
        HQdists = new int[9];
        isPassables = new boolean[9];
    }

    public void reset() {
        is_bugging = false;
        minDist = 1000000;
        lastObstacle = null;
        visited = new StringBuilder();
    }

    /*public boolean isPassable(Direction d, MapLocation dest) throws GameActionException {
        return ;
    }*/

    public boolean tryNavigate(MapLocation target) throws GameActionException {
        MapLocation[] hqs = new MapLocation[1];
        return tryNavigate(target, hqs);
    }

    public int minHQDist(MapLocation dest) {
        int minDist = 10;
        int dist;
        for (MapLocation hq : enemyHQs) {
            dist = dest.distanceSquaredTo(hq);
            if (dist <= 9 && dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
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
        
        enemyHQs = hqs;
        maxHQdist = 0;

        MapLocation nearestHQ = new MapLocation(-50, -50);
        int nearestHQdist = 1000000;
        int hqDist;
        for (MapLocation hqLoc : hqs) {
            hqDist = hqLoc.distanceSquaredTo(curr);
            if (hqDist < nearestHQdist) {
                nearestHQ = hqLoc;
                nearestHQdist = hqDist;
            }
        }
        
        MapLocation dirDest;
        MapLocation currDest;
        boolean isPassableDir;
        for (Direction dir : directions) {
            dirDest = curr.add(dir);
            if (!rc.onTheMap(dirDest)) continue;
            //hqDist = minHQDist(dirDest);
            hqDist = nearestHQ.distanceSquaredTo(dirDest);
            HQdists[dir.ordinal()] = hqDist;
            currDest = dirDest.add(rc.senseMapInfo(dirDest).getCurrentDirection());
            isPassableDir = rc.sensePassability(dirDest) && visited.indexOf(currDest.toString()) < 0 && currDest.distanceSquaredTo(target) <= dirDest.distanceSquaredTo(target);
            isPassables[dir.ordinal()] = isPassableDir;
            if (hqDist > maxHQdist && isPassableDir) {
                maxHQdist = hqDist;
            }
        }

        for (int i = 9; i-->0;) {
            if (HQdists[i] <= 9 && HQdists[i] < maxHQdist) isPassables[i] = false;
        }

        if (HQdists[Direction.CENTER.ordinal()] <= 9) {
            reset();
        }

        /*for (Direction dir : directionsNoCenter) {
            dirDest = curr.add(dir);
            if (!rc.onTheMap(dirDest)) continue;
            isPassables[dir.ordinal()] = isPassable(dir, curr.add(dir));
            if (isPassables[dir.ordinal()]) {
                rc.setIndicatorDot(dirDest, 0, 255, 0);
            }
        }*/

        Direction d;
        if (lastObstacle == null) {
            d = curr.directionTo(target);
        } else {
            d = curr.directionTo(lastObstacle);
        }

        if (isPassables[d.ordinal()] && rc.canMove(d)) {
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
                if (isPassables[l.ordinal()]) {
                    if (rc.canMove(l)) {
                        destLeft = dest;
                        break;
                    }
                } else {
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
                if (isPassables[r.ordinal()]) {
                    if (rc.canMove(r)) {
                        destRight = dest;
                        break;
                    }
                } else {
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
        for (int i = 7; i-- > 0;) {
            if (right) d = d.rotateRight();
            else d = d.rotateLeft();
            dest = curr.add(d);
            if (!rc.onTheMap(dest)) {right = !right; i = 7;}
            else {
                if (isPassables[d.ordinal()]) {
                    if (rc.canMove(d)) {
                        rc.move(d);
                        return true;
                    }
                } else {
                    lastObstacle = dest;
                }
            }
        }

        return false;

    }

}
