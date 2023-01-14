package sphere;

import battlecode.common.*;

import java.util.Random;

public strictfp abstract class Robot {

    static RobotController rc;

    static final Random rng = new Random();

    static final int MARGIN = 3;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static final ResourceType[] resourceTypes = {
        ResourceType.ADAMANTIUM,
        ResourceType.MANA,
        ResourceType.ELIXIR,
    };

    public Robot(RobotController rc) {
        this.rc = rc;
    }

    public abstract void run() throws GameActionException;

    public MapLocation randomLocation() {
        int width = rc.getMapWidth();
        int height = rc.getMapHeight();
        int x = rng.nextInt(width - 2 * MARGIN) + MARGIN;
        int y = rng.nextInt(height - 2 * MARGIN) + MARGIN;
        return new MapLocation(x, y);
    }
    
    /*
    public void processArea() throws GameActionException {
        WellInfo[] wells = rc.senseNearbyWells();
        for(WellInfo well: wells) {
            Communications.tryAddWell(rc, well);
        }
        
        int[] islands = rc.senseNearbyIslands();
        for(int island: islands) {
            if(rc.senseTeamOccupyingIsland(island) != rc.getTeam()) {
                Communications.tryAddIsland(rc, rc.senseNearbyIslandLocations(island)[0]);
            }
        }
        
        RobotInfo[] targets = rc.senseNearbyRobots(-1);
        for(RobotInfo target: targets) {
            if(target.type == RobotType.HEADQUARTERS && target.team == rc.getTeam()) {
                Communications.tryAddHQ(rc, target.getLocation());
            }
        }
    }*/
    
    public boolean tryFuzzy(MapLocation location) throws GameActionException {
        MapLocation curr = rc.getLocation();
        Direction d = curr.directionTo(location);

        return tryFuzzy(d);
    }

    public boolean tryFuzzy(Direction d) throws GameActionException {
        if (rc.canMove(d)) {
            rc.move(d);
            return true;
        }

        Direction left = d.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return true;
        }

        Direction right = d.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return true;
        }

        left = left.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return true;
        }

        right = right.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return true;
        }

        left = left.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return true;
        }

        right = right.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return true;
        }

        return false;
    }

}
