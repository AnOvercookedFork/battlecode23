package sphere;

import battlecode.common.*;

public strictfp class Communications {
    /*
    public static final int ISLAND_INDEX = 0; // size 10
    public static final int AD_INDEX = 10; // size 5
    public static final int MN_INDEX = 15; // size 5
    public static final int EL_INDEX = 20; // size 5
    public static final int HQ_INDEX = 25; // size 4
    public static final int AMP_INDEX = 29; // size 2
    public static final int CAROUSEL_INDEX = 31; // size 5
    public static final int CAROUSEL_SIZE = 5; // special case because this may be changed
                                               // */
    
    public static final int WELL_START = 0;
    public static final int WELL_SIZE = 8;
    public static final int WELL_REPORT_START = 8;
    public static final int WELL_REPORT_SIZE = 8;
    public static final int ISLAND_START = 16;
    public static final int ISLAND_SIZE = 8;
    public static final int ISLAND_REPORT_START = 24;
    public static final int ISLAND_REPORT_SIZE = 8;
    public static final int ENEMIES_START = 32;
    public static final int ENEMIES_SIZE = 16;
    public static final int HQ_INDEX = 48;
    public static final int AMP_INDEX = 52;

    public static int[] array = new int[64];
    private static int lastRead = -1;
    public static boolean canWrite = false;
    public static boolean canAlwaysWrite = false;
    
    // should be called at the start of each robots turn, ideally
    public static void readArray(RobotController rc) throws GameActionException {
        canWrite = false;
        if (lastRead < rc.getRoundNum()) {
            lastRead = rc.getRoundNum();
            for (int i = 0; i < 64; i++) {
                array[i] = rc.readSharedArray(i);
            }
            canWrite = canAlwaysWrite || rc.canWriteSharedArray(0, 0);
        }
    }


    public static int locToInt(MapLocation loc) {
        return loc.x + (loc.y << 6);
    }

    public static MapLocation intToLoc(int i) {
        return new MapLocation(i % 64, (i >> 6) % 64);
    }

    /*

    public static void tryAddIsland(RobotController rc, MapLocation location) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {
            for (int i = 0; i < 10; i++) {
                if (array[ISLAND_INDEX + i] >> 12 == 0) { // 13th bit will be set to 1 so 0,0 is a valid location
                    array[ISLAND_INDEX + i] = 1 << 12 + locToInt(location);
                    rc.writeSharedArray(ISLAND_INDEX + i, array[ISLAND_INDEX + i]);
                } else if (location.equals(intToLoc(array[ISLAND_INDEX + i]))) {
                    System.out.println("This island has already been added");
                    break;
                }
            }
        }
    }

    public static void tryAddWell(RobotController rc, WellInfo well) throws GameActionException {
        int index = AD_INDEX;
        switch (well.getResourceType()) {
        case ADAMANTIUM:
            index = AD_INDEX;
            break;
        case MANA:
            index = MN_INDEX;
            break;
        case ELIXIR:
            index = EL_INDEX;
            break;
        default:
            System.out.println("Unknown resource type?");
        }

        if (rc.canWriteSharedArray(0, 0)) {
            for (int i = 0; i < 5; i++) {
                if (array[index + i] >> 12 == 0) {
                    array[index + i] = 1 << 12 + locToInt(well.getMapLocation());
                    rc.writeSharedArray(index + i, array[index + i]);
                } else if (well.getMapLocation().equals(intToLoc(array[index + i]))) {
                    System.out.println("This well has already been added");
                    break;
                }
            }
        }
    }
    */

    public static void reportWell(RobotController rc, MapCache cache) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return;
        for (int i = WELL_REPORT_START; i < WELL_REPORT_START + WELL_REPORT_SIZE; i++) {
            if (array[i] == 0) {
                MapCache.WellData sample = cache.sampleWellCache(true);
                if (sample != null) {
                   array[i] = sample.encode() + 1;
                   rc.writeSharedArray(i, array[i]);
                   sample.fromComms = true;
                }
                return;
            }
        }
    }

    public static void cycleWells(RobotController rc, MapCache cache) throws GameActionException {

        for (int i = WELL_START; i < WELL_START + WELL_SIZE; i++) {
            MapCache.WellData sample = cache.sampleWellCache(false);
            if (sample == null) break;
            array[i] = sample.encode() + 1;
            rc.writeSharedArray(i, array[i]);
        }

        for (int i = WELL_REPORT_START; i < WELL_REPORT_START + WELL_REPORT_SIZE; i++) {
            array[i] = 0;
            rc.writeSharedArray(i, 0);
        }

    }

    public static void readWells(RobotController rc, MapCache cache) {
        for (int i = WELL_START; i < WELL_START + WELL_SIZE + WELL_REPORT_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateWellCacheFromComms(array[i] - 1);
            }
        }
    }

    public static void readReportingWells(RobotController rc, MapCache cache) {
        for (int i = WELL_REPORT_START; i < WELL_REPORT_START + WELL_REPORT_SIZE; i++) {
            if (array[i] != 0) {
                System.out.println("array["+i+"]="+array[i]);
                cache.updateWellCacheFromComms(array[i] - 1);
            }
        }
    }

    public static void tryAddHQ(RobotController rc, MapLocation location) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) {
            for (int i = 0; i < 4; i++) {
                if (array[HQ_INDEX + i] >> 12 == 0) { // 13th bit will be set to 1 so 0,0 is a valid location
                    array[HQ_INDEX + i] = (1 << 12) + locToInt(location);
                    rc.writeSharedArray(HQ_INDEX + i, array[HQ_INDEX + i]);
                    return;
                } else if (location.equals(intToLoc(array[HQ_INDEX + i]))) {
                    System.out.println("This hq has already been added");
                    break;
                }
            }
        }
        System.out.println("Failed to add HQ");
    }

    public static boolean isFirstHQ(RobotController rc) throws GameActionException {
        MapLocation l = rc.getLocation();
        return l.equals(intToLoc(array[HQ_INDEX]));
    }
    
    public static int getAmpCount() {
        return array[AMP_INDEX + 1];
    }
    
    public static void updateAmpCount(RobotController rc) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) { // should always be able to write
            array[AMP_INDEX + 1] = array[AMP_INDEX];
            array[AMP_INDEX] = 0;
            
            rc.writeSharedArray(AMP_INDEX + 1, array[AMP_INDEX + 1]);
            rc.writeSharedArray(AMP_INDEX, array[AMP_INDEX]);
        } else {
            System.out.println("Ruh roh, unable to write to shared array in updateAmpCount! :o");
        }
    }
    
    public static void incrementAmpCount(RobotController rc) throws GameActionException {
        array[AMP_INDEX] += 1;
        rc.writeSharedArray(AMP_INDEX, array[AMP_INDEX]);
    }
}
