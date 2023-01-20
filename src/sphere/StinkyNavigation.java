package sphere;

import battlecode.common.*;

public strictfp class StinkyNavigation {
    int resetRound;
    int[][] roundVisited;

    MapLocation lastTarget;
    RobotController rc;

    public static final double STINKY_BASE = 4;
    public static final double STINKY_DECAY = 0.9;
    public static final double RESET_THRESHOLD = 2;

    public StinkyNavigation(RobotController rc) {
        resetRound = 0;
        roundVisited = new int[rc.getMapWidth()][rc.getMapHeight()];
        lastTarget = null;
        this.rc = rc;
    }

    public double stinkyFactor(MapLocation loc, int round) {
        int rv = roundVisited[loc.x][loc.y];
        if (rv > resetRound) {
            return STINKY_BASE * Math.pow(STINKY_DECAY, round - rv);
        }
        return 0;
    }

    public MapLocation getDestination(RobotController rc, MapLocation loc) throws GameActionException {
        if(!rc.canSenseLocation(loc)) {
            return loc;
        }
        MapInfo info = rc.senseMapInfo(loc);
        if(info.getCurrentDirection() == Direction.CENTER) {
            return loc;
        }
        else {
            MapLocation next = loc.add(info.getCurrentDirection());
            if (rc.canSenseRobotAtLocation(next)) {
                if (rc.senseRobotAtLocation(next) == null) {
                    return loc;
                }
                
                return getDestination(rc, next);
            }
        }
        return null;
    }
    
    public void reset() {
        resetRound = rc.getRoundNum();
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
        if(avoidHQs == null || avoidHQs.length == 0) {
            return 0;
        }
        
        for(int i = 0; i <= avoidHQs.length; i++) {
            if(loc.distanceSquaredTo(avoidHQs[i]) <= 9) {
                System.out.println("debug hq pathfinding score: " + (9 - loc.distanceSquaredTo(avoidHQs[i])));
                return (9 - loc.distanceSquaredTo(avoidHQs[i])) * 10;
            }
        }
        
        return 0;
    }
    
    public Direction navigate(MapLocation loc, MapLocation[] avoidHQs) {
        Direction bestDir = Direction.CENTER;
        double score;
        MapLocation curr = rc.getLocation();
        int round = rc.getRoundNum();
        roundVisited[curr.x][curr.y] = round;

        if (lastTarget == null || lastTarget.distanceSquaredTo(loc) >= RESET_THRESHOLD) {
            resetRound = round;
        }
        lastTarget = loc;


        double bestScore = Math.sqrt(curr.distanceSquaredTo(loc)) + stinkyFactor(curr, round);

        MapLocation next;
        if (rc.canMove(Direction.NORTH)) {
            next = curr.add(Direction.NORTH);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTH;
            }
        }
        
        if (rc.canMove(Direction.NORTHEAST)) {
            next = curr.add(Direction.NORTHEAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHEAST;
            }
        }

        if (rc.canMove(Direction.EAST)) {
            next = curr.add(Direction.EAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.EAST;
            }
        }

        if (rc.canMove(Direction.SOUTHEAST)) {
            next = curr.add(Direction.SOUTHEAST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHEAST;
            }
        }

        if (rc.canMove(Direction.SOUTH)) {
            next = curr.add(Direction.SOUTH);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTH;
            }
        }

        if (rc.canMove(Direction.SOUTHWEST)) {
            next = curr.add(Direction.SOUTHWEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.SOUTHWEST;
            }
        }

        if (rc.canMove(Direction.WEST)) {
            next = curr.add(Direction.WEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.WEST;
            }
        }
        
        if (rc.canMove(Direction.NORTHWEST)) {
            next = curr.add(Direction.NORTHWEST);
            score = Math.sqrt(next.distanceSquaredTo(loc)) + stinkyFactor(next, round) + isInHQRange(next, avoidHQs);
            if (score < bestScore) {
                bestScore = score;
                bestDir = Direction.NORTHWEST;
            }
        }
        
        return bestDir;
    }

    public boolean tryNavigate(MapLocation loc) throws GameActionException {
        Direction dir = navigate(loc);
        if (dir != Direction.CENTER) {
            rc.move(dir);
            return true;
        }
        return false;
    }

}
