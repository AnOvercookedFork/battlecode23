package klein_bottle2_02_1;

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
    static MapLocation[] destinations;

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
        destinations = new MapLocation[9];
    }

    public void reset() {
        is_bugging = false;
        lastObstacle = null;
    }

    public boolean isPassable(Direction d, MapLocation dest) throws GameActionException {
        return rc.sensePassability(dest) && visited.indexOf(dest.toString()) < 0;
    }

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
            minDist = INF;
            visited = new StringBuilder();
        }

        lastTarget = target;

        MapLocation curr = rc.getLocation();
        String currStr = curr.toString();
        int dist = curr.distanceSquaredTo(target);

        if (dist < minDist) {lastObstacle = null; minDist = dist; is_bugging = false;}
        else if (visited.indexOf(currStr) >= 0) {
            if (is_bugging) visited = new StringBuilder();
            is_bugging = true;
        }
        visited.append(currStr).append("|");
        
        enemyHQs = hqs;
        maxHQdist = 0;

        rc.setIndicatorLine(curr, target, 100, 100, 100);

        MapLocation dirDest;
        MapLocation destCurr;
        int hqDist;
        boolean isPassableDir;
        for (Direction dir : directions) {
            dirDest = curr.add(dir);
            if (!rc.onTheMap(dirDest)) continue;
            hqDist = minHQDist(dirDest);
            HQdists[dir.ordinal()] = hqDist;
            destCurr = dirDest.add(rc.senseMapInfo(dirDest).getCurrentDirection());
            destinations[dir.ordinal()] = destCurr;
            isPassableDir = rc.sensePassability(dirDest) && visited.indexOf(destCurr.toString()) < 0 && destCurr.distanceSquaredTo(target) <= dirDest.distanceSquaredTo(target);
            isPassables[dir.ordinal()] = isPassableDir;
            if (hqDist > maxHQdist && isPassableDir) {
                maxHQdist = hqDist;
            }
        }


        for (int i = 9; i-->0;) {
            if (HQdists[i] < maxHQdist) isPassables[i] = false;
        }

        if (HQdists[Direction.CENTER.ordinal()] <= 9) {
            lastObstacle = null;
            is_bugging = true;
            visited = new StringBuilder();
        }

        rc.setIndicatorString("is bug? " + is_bugging + ", md: " + minDist);

        if (!is_bugging) {
            Direction dirTo = curr.directionTo(target);
            Direction bestDir = Direction.CENTER;
            int bestScore = minDist;
            int score;
            Direction d = dirTo;
            boolean isObstacle = false;
            if (isPassables[d.ordinal()]) {
                score = destinations[d.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = d;
                }
            } else {
                isObstacle = true;
            }
            d = dirTo.rotateRight();
            if (isPassables[d.ordinal()]) {
                score = destinations[d.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = d;
                }
            } else {
                isObstacle = true;
            }
            d = dirTo.rotateLeft();
            if (isPassables[d.ordinal()]) {
                score = destinations[d.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = d;
                }
            } else {
                isObstacle = true;
            }
            /*if (rc.canMove(Direction.SOUTHEAST) && HQdists[Direction.SOUTHEAST.ordinal()] > 9) {
                score = destinations[Direction.SOUTHEAST.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = Direction.SOUTHEAST;
                }
            }
            if (rc.canMove(Direction.SOUTH) && HQdists[Direction.SOUTH.ordinal()] > 9) {
                score = destinations[Direction.SOUTH.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = Direction.SOUTH;
                }
            }
            if (rc.canMove(Direction.SOUTHWEST) && HQdists[Direction.SOUTHWEST.ordinal()] > 9) {
                score = destinations[Direction.SOUTHWEST.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = Direction.SOUTHWEST;
                }
            }
            if (rc.canMove(Direction.WEST) && HQdists[Direction.WEST.ordinal()] > 9) {
                score = destinations[Direction.WEST.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = Direction.WEST;
                }
            }
            if (rc.canMove(Direction.NORTHWEST) && HQdists[Direction.NORTHWEST.ordinal()] > 9) {
                score = destinations[Direction.NORTHWEST.ordinal()].distanceSquaredTo(target);
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = Direction.NORTHWEST;
                }
            }*/
            if (bestDir == Direction.CENTER || isObstacle || !rc.canMove(bestDir)) {
                is_bugging = true;
            } else {
                rc.move(bestDir);
                return true;
            }
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
            lastObstacle = null;
            //visited = new StringBuilder();
            //is_bugging = false;
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
