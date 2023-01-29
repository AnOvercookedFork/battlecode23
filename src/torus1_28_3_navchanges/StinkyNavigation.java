package torus1_28_3_navchanges;

import battlecode.common.*;

public strictfp class StinkyNavigation {
    int resetRound;
    //int[][] roundVisited;
    StringBuilder visited;

    MapLocation lastTarget;
    RobotController rc;
    BugNavigation bnav;

    public static final double STINKY_BASE = 4;
    public static final double STINKY_DECAY = 0.9;
    public static final double RESET_THRESHOLD = 2;
    //public static final int BUG_THRESHOLD = 8; // After making this many moves without progress, start bugging
    public static final int INF = Integer.MAX_VALUE;

    int minDist;
    int greedyMoves;

    public StinkyNavigation(RobotController rc) {
        resetRound = 0;
        //roundVisited = new int[rc.getMapWidth()][rc.getMapHeight()];
        visited = new StringBuilder();
        lastTarget = null;
        bnav = new BugNavigation(rc);
        minDist = INF;
        this.rc = rc;
    }

    public double stinkyFactor(MapLocation loc, int round) {
        int idx = visited.indexOf(loc.toString());
        if (idx >= 0) return STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0);
        else return 0;
        /*int rv = roundVisited[loc.x][loc.y];
        if (rv > resetRound) {
            return STINKY_BASE * Math.pow(STINKY_DECAY, round - rv);
        }
        return 0;*/
    }

    public MapLocation getDestination(MapLocation loc) throws GameActionException {

        return loc.add(rc.senseMapInfo(loc).getCurrentDirection());
        /*if (rc.canSenseLocation(loc)) {
            loc = loc.add(rc.senseMapInfo(loc).getCurrentDirection());
            return loc;
        } else {
            return loc;
        }*/
    }

    public void reset() {
        resetRound = rc.getRoundNum();
        minDist = INF;
    }

    //
    public int isInHQRange(MapLocation loc, MapLocation[] avoidHQs) {

        for (int i = avoidHQs.length; i-->0;) {
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
        //int round = rc.getRoundNum();
        //roundVisited[curr.x][curr.y] = round;
        //double cooldownMultiplier = rc.senseMapInfo(curr).getCooldownMultiplier();
        RobotType type = rc.getType();
        boolean current = rc.getMovementCooldownTurns() + 
            (type == RobotType.CARRIER? 5 + (3 * rc.getWeight()) / 8: type.movementCooldown) >= GameConstants.COOLDOWN_LIMIT;

        if (lastTarget == null || lastTarget.distanceSquaredTo(loc) >= RESET_THRESHOLD) {
            //resetRound = round;
            visited = new StringBuilder();
            //minDist = INF;
        }
        lastTarget = loc;
        visited.insert(0, curr);


        MapLocation next = curr;
        if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());

        int idx = visited.indexOf(next.toString());

        double bestScore = Math.sqrt(next.distanceSquaredTo(loc))
            + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
            + isInHQRange(next, avoidHQs);


        if (rc.canMove(Direction.NORTH)) {
            next = curr.add(Direction.NORTH);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTH;
            }
        }

        if (rc.canMove(Direction.NORTHEAST)) {
            next = curr.add(Direction.NORTHEAST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHEAST;
            }
        }

        if (rc.canMove(Direction.EAST)) {
            next = curr.add(Direction.EAST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.EAST;
            }
        }

        if (rc.canMove(Direction.SOUTHEAST)) {
            next = curr.add(Direction.SOUTHEAST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHEAST;
            }
        }

        if (rc.canMove(Direction.SOUTH)) {
            next = curr.add(Direction.SOUTH);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTH;
            }
        }

        if (rc.canMove(Direction.SOUTHWEST)) {
            next = curr.add(Direction.SOUTHWEST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHWEST;
            }
        }

        if (rc.canMove(Direction.WEST)) {
            next = curr.add(Direction.WEST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.WEST;
            }
        }

        if (rc.canMove(Direction.NORTHWEST)) {
            next = curr.add(Direction.NORTHWEST);
            if (current) next = next.add(rc.senseMapInfo(next).getCurrentDirection());
            idx = visited.indexOf(next.toString());
            score = Math.sqrt(next.distanceSquaredTo(loc))
                + (idx >= 0? STINKY_BASE * Math.pow(STINKY_DECAY, idx / 7.0): 0)
                + isInHQRange(next, avoidHQs);
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
                /*int dist = rc.getLocation().distanceSquaredTo(loc);
                if (dist >= minDist) {
                    greedyMoves++;
                } else {
                    greedyMoves = 0;
                    minDist = dist;
                }*/
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
