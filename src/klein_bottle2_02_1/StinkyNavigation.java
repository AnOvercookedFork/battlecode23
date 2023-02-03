package klein_bottle2_02_1;

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
        // if(rc.getType() == RobotType.CARRIER) {
        //     if(rc.getMovementCooldownTurns() + 5 + (int) (3 * rc.getWeight() / 8) < GameConstants.COOLDOWN_LIMIT) {
        //         return loc;
        //     }
        // }
        if (rc.canSenseLocation(loc)) {
            loc = loc.add(rc.senseMapInfo(loc).getCurrentDirection());
            /*if (rc.canSenseLocation(loc)) {
                return loc.add(rc.senseMapInfo(loc).getCurrentDirection());
            }*/
            return loc;
        } else {
            return loc;
        }
    }

    public void reset() {
        resetRound = rc.getRoundNum();
        minDist = INF;
    }

    //
    public int isInHQRange(MapLocation loc, MapLocation[] avoidHQs) {
        if (avoidHQs == null || avoidHQs.length == 0) {
            return 0;
        }

        for (int i = 0; i < avoidHQs.length; i++) {
            if (loc.distanceSquaredTo(avoidHQs[i]) <= 9) {
                //System.out.println("debug hq pathfinding score: " + (9 - loc.distanceSquaredTo(avoidHQs[i])));
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

        double bestScore = Math.sqrt(curr.distanceSquaredTo(loc)) + stinkyFactor(curr, round) + isInHQRange(curr, avoidHQs);

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
        //if (greedyMoves < BUG_THRESHOLD) {
            Direction dir = navigate(loc, avoidHQs);
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
