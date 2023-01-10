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
    
    public Robot(RobotController rc) {
        this.rc = rc;
    }

    public abstract void run() throws GameActionException;
}
