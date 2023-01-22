package sphere;

import battlecode.common.*;

public strictfp class StinkyNavigation {
    int resetRound;
    int[][] roundVisited;

    MapLocation lastTarget;
    RobotController rc;
    BugNavigation bnav;

    public static final double STINKY_BASE = 4;
    public static final double STINKY_DECAY = 0.9;
    public static final double RESET_THRESHOLD = 2;
    public static final int BUG_THRESHOLD = 8; // After making this many moves without progress, start bugging
    public static final int INF = Integer.MAX_VALUE;

    int minDist;
    int greedyMoves;

    public StinkyNavigation(RobotController rc) {
        resetRound = 0;
        roundVisited = new int[rc.getMapWidth()][rc.getMapHeight()];
        lastTarget = null;
        bnav = new BugNavigation(rc);
        minDist = INF;
        this.rc = rc;
    }

    public double stinkyFactor(MapLocation loc, int round) {
        int rv = roundVisited[loc.x][loc.y];
        if (rv > resetRound) {
            return STINKY_BASE * Math.pow(STINKY_DECAY, round - rv);
        }
        return 0;
    }

    public MapLocation getDestination(MapLocation loc) throws GameActionException {
        if (!rc.canSenseLocation(loc)) {
            return loc;
        }
        MapInfo info = rc.senseMapInfo(loc);
        if (info.getCurrentDirection() == Direction.CENTER) {
            return loc;
        } else {
            MapLocation next = loc.add(info.getCurrentDirection());
            if (rc.canSenseRobotAtLocation(next)) {
                if (rc.senseRobotAtLocation(next) != null) {
                    return loc;
                }

                return getDestination(next);
            }
            return loc;
        }
    }

    public void reset() {
        resetRound = rc.getRoundNum();
        minDist = INF;
    }

    // deprecating, use navigate(loc, avoidHqs) instead
    public Direction navigate(MapLocation loc) throws GameActionException {
        Direction bestDir = Direction.CENTER;
        double score;
        MapLocation curr = rc.getLocation();
        int round = rc.getRoundNum();
        roundVisited[curr.x][curr.y] = round;

        if (lastTarget == null || lastTarget.distanceSquaredTo(loc) >= RESET_THRESHOLD) {
            resetRound = round;
            minDist = INF;
        }
        lastTarget = loc;

        double bestScore = Math.sqrt(curr.distanceSquaredTo(loc)) + stinkyFactor(curr, round);

        MapLocation next;
        if (rc.canMove(Direction.NORTH)) {
            next = curr.add(Direction.NORTH);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTH;
            }
        }

        if (rc.canMove(Direction.NORTHEAST)) {
            next = curr.add(Direction.NORTHEAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHEAST;
            }
        }

        if (rc.canMove(Direction.EAST)) {
            next = curr.add(Direction.EAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.EAST;
            }
        }

        if (rc.canMove(Direction.SOUTHEAST)) {
            next = curr.add(Direction.SOUTHEAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHEAST;
            }
        }

        if (rc.canMove(Direction.SOUTH)) {
            next = curr.add(Direction.SOUTH);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTH;
            }
        }

        if (rc.canMove(Direction.SOUTHWEST)) {
            next = curr.add(Direction.SOUTHWEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHWEST;
            }
        }

        if (rc.canMove(Direction.WEST)) {
            next = curr.add(Direction.WEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.WEST;
            }
        }

        if (rc.canMove(Direction.NORTHWEST)) {
            next = curr.add(Direction.NORTHWEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHWEST;
            }
        }

        return bestDir;
    }

    //
    public int isInHQRange(MapLocation loc, MapLocation[] avoidHQs) {
        if (avoidHQs == null || avoidHQs.length == 0) {
            return 0;
        }

        for (int i = 0; i < avoidHQs.length; i++) {
            if (loc.distanceSquaredTo(avoidHQs[i]) <= 9) {
                System.out.println("debug hq pathfinding score: " + (9 - loc.distanceSquaredTo(avoidHQs[i])));
                return (9 - loc.distanceSquaredTo(avoidHQs[i])) * 1000;
            }
        }

        return 0;
    }

    public Direction navigate(MapLocation loc, MapLocation[] avoidHQs) throws GameActionException {
        Direction bestDir = Direction.CENTER;
        double score;
        MapLocation curr = rc.getLocation();
        int round = rc.getRoundNum();
        roundVisited[curr.x][curr.y] = round;

        if (lastTarget == null || lastTarget.distanceSquaredTo(loc) >= RESET_THRESHOLD) {
            resetRound = round;
            minDist = INF;
        }
        lastTarget = loc;

        double bestScore = Math.sqrt(curr.distanceSquaredTo(loc)) + stinkyFactor(curr, round);

        MapLocation next;
        if (rc.canMove(Direction.NORTH)) {
            next = getDestination(curr.add(Direction.NORTH));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTH;
            }
        }

        if (rc.canMove(Direction.NORTHEAST)) {
            next = getDestination(curr.add(Direction.NORTHEAST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHEAST;
            }
        }

        if (rc.canMove(Direction.EAST)) {
            next = getDestination(curr.add(Direction.EAST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.EAST;
            }
        }

        if (rc.canMove(Direction.SOUTHEAST)) {
            next = getDestination(curr.add(Direction.SOUTHEAST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHEAST;
            }
        }

        if (rc.canMove(Direction.SOUTH)) {
            next = getDestination(curr.add(Direction.SOUTH));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTH;
            }
        }

        if (rc.canMove(Direction.SOUTHWEST)) {
            next = getDestination(curr.add(Direction.SOUTHWEST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHWEST;
            }
        }

        if (rc.canMove(Direction.WEST)) {
            next = getDestination(curr.add(Direction.WEST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.WEST;
            }
        }

        if (rc.canMove(Direction.NORTHWEST)) {
            next = getDestination(curr.add(Direction.NORTHWEST));
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHWEST;
            }
        }

        return bestDir;
    }

    public boolean tryNavigate(MapLocation loc) throws GameActionException {
        MapLocation[] blank = {};
        return tryNavigate(loc, blank);
    }

    public boolean tryNavigate(MapLocation loc, MapLocation[] avoidHQs) throws GameActionException {
        MapLocation[] blank = {};
        //if (greedyMoves < BUG_THRESHOLD) {
            Direction dir = navigate(loc, blank);
            if (dir != Direction.CENTER) {
                rc.move(dir);
                int dist = rc.getLocation().distanceSquaredTo(loc);
                if (dist >= minDist) {
                    greedyMoves++;
                } else {
                    greedyMoves = 0;
                    minDist = dist;
                }
                return true;
            }
        /*} else {
            boolean success = bnav.tryNavigate(loc);
            int dist = rc.getLocation().distanceSquaredTo(loc);
            if (dist < minDist) {
                greedyMoves = 0;
                minDist = dist;
            }
            return success;
        }*/
        return false;
    }
}
