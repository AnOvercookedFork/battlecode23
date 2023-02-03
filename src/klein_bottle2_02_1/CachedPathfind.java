package klein_bottle2_02_1;

import battlecode.common.*;
import java.util.ArrayDeque;

public class CachedPathfind {
    /*int[][] distance;
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
    
    public Direction[] pathHome(MapLocation loc) {
        int count = 0;
        int best = Integer.MAX_VALUE;
        MapLocation check;
        for(Direction d: Micro.dirs) {
            check = loc.add(d);
            if(distance[check.y][check.x] < best) {
                best = distance[check.y][check.x];
                count = 1;
            }
            else if (distance[check.y][check.x] == best){
                count++;
            }
        }
        
        Direction[] moves = new Direction[count];
        count = 0; // reusing, is not the same thing as before
        for(Direction d: Micro.dirs) {
            check = loc.add(d);
            if(distance[check.y][check.x] == best) {
                moves[count] = d;
                count++;
            }
        }
        
        return moves;
    }
    
    public MapLocation getDestination(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            loc = loc.add(rc.senseMapInfo(loc).getCurrentDirection());
            return loc;
        } else {
            return loc;
        }
    }*/

}
