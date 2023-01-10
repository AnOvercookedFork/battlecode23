package sphere;

import battlecode.common.*;
import java.util.Random;

public strictfp abstract class Robot {

    static RobotController rc;

    static final Random rng = new Random(2023);

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
        int x = rng.nextInt(width);
        int y = rng.nextInt(height);
        return new MapLocation(x, y);
    }

    public boolean tryFuzzy(MapLocation location) throws GameActionException {
        MapLocation curr = rc.getLocation();
        Direction d = curr.directionTo(location);

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
