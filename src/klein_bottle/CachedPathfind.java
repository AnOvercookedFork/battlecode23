package klein_bottle;

import battlecode.common.*;
import java.util.ArrayDeque;

public class CachedPathfind {
    int[][] distance;
    ArrayDeque<MapLocation> fringe;
    RobotController rc;
    
    public CachedPathfind(RobotController rc) {
        distance = new int[rc.getMapHeight()][rc.getMapWidth()];
        fringe = new ArrayDeque<MapLocation>(1000);
        this.rc = rc;
    }
    
    public void updateNearestHQ(MapLocation ml) {
        distance[ml.x][ml.y] = 0; 
    }
    
    // if a current on the tile points to a node in the edge, add node to front of queue
    public void update() {
        while(Clock.getBytecodesLeft() > 500) {
            
        }
    }
    
    public void add(MapLocation ml, Direction d) {
        for(int i = 0; i < 9; i++) {
            if(directions[ml.y][ml.x][i] == Direction.CENTER) {
                directions[ml.y][ml.x][i] = d;
                return;
            }
        }
    }
    
    public Direction[] pathHome(MapLocation loc) {
        if(directions[loc.y][loc.x][0] != Direction.CENTER) {
            return directions[loc.y][loc.x];
        }
        else {
            return null;
        }
    }
    
    public MapLocation getDestination(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            loc = loc.add(rc.senseMapInfo(loc).getCurrentDirection());
            return loc;
        } else {
            return loc;
        }
    }

}
