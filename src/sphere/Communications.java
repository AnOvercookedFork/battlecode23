package sphere;

import battlecode.common.*;

public strictfp class Communications {
	private static final int ISLAND_INDEX = 0; // size 10
	private static final int AD_INDEX = 10; // size 5
	private static final int MN_INDEX = 15; // size 5
	private static final int EL_INDEX = 20; // size 5
	private static final int HQ_INDEX = 25; // size 4
	
    private static int[] array = new int[64];
    private static int lastRead = -1;
    private static boolean canWrite = false;
    
    // should be called at the start of each robots turn, ideally
	private static void readArray(RobotController rc) throws GameActionException {
        if (lastRead < rc.getRoundNum()) {
            lastRead = rc.getRoundNum();
            for(int i = 0; i < 64; i++) {
            	array[i] = rc.readSharedArray(i);
            }
            canWrite = rc.canWriteSharedArray(0, 0);
        }
    }
	
	private static int locToInt(MapLocation loc) {
		return loc.x + loc.y << 6;
	}
	
	private static MapLocation intToLoc(int i) {
		return new MapLocation(i % 64, (i >> 6) % 64);
	}
	
	private static void tryAddWell(RobotController rc, WellInfo well) throws GameActionException {
		
	}
	
	private static void tryAddHQ(RobotController rc, MapLocation location) throws GameActionException {
		if(canWrite) {
			for(int i = 0; i < 4; i++) {
				if(array[HQ_INDEX + i] >> 12 == 0) { // 13th bit will be set to 1 so 0,0 is a valid location
					array[HQ_INDEX + i] = 1 << 12 + locToInt(location);
				}
			}
		}
	}
}
