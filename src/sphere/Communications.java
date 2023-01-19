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
    public static final int WELL_SIZE = 4;
    public static final int WELL_REPORT_START = 4;
    public static final int WELL_REPORT_SIZE = 12;
    public static final int ISLAND_START = 16;
    public static final int ISLAND_SIZE = 4;
    public static final int ISLAND_REPORT_START = 20;
    public static final int ISLAND_REPORT_SIZE = 12;
    public static final int ENEMIES_START = 32;
    public static final int ENEMIES_SIZE = 16;
    public static final int HQ_INDEX = 48;
    public static final int AMP_INDEX = 52; // size 2
    public static final int ENEMY_LOCATION = 54; // just the one
    public static final int ENEMY_SNIPE_START = 55; // fills remainder of array

    public static int[] array = new int[64];
    private static int lastRead = -1;
    public static boolean canAlwaysWrite = false;
    
    // should be called at the start of each robots turn, ideally
    public static void readArray(RobotController rc) throws GameActionException {
        if (lastRead < rc.getRoundNum()) {
            lastRead = rc.getRoundNum();
            array[0] = rc.readSharedArray(0);
            array[1] = rc.readSharedArray(1);
            array[2] = rc.readSharedArray(2);
            array[3] = rc.readSharedArray(3);
            array[4] = rc.readSharedArray(4);
            array[5] = rc.readSharedArray(5);
            array[6] = rc.readSharedArray(6);
            array[7] = rc.readSharedArray(7);
            array[8] = rc.readSharedArray(8);
            array[9] = rc.readSharedArray(9);
            array[10] = rc.readSharedArray(10);
            array[11] = rc.readSharedArray(11);
            array[12] = rc.readSharedArray(12);
            array[13] = rc.readSharedArray(13);
            array[14] = rc.readSharedArray(14);
            array[15] = rc.readSharedArray(15);
            array[16] = rc.readSharedArray(16);
            array[17] = rc.readSharedArray(17);
            array[18] = rc.readSharedArray(18);
            array[19] = rc.readSharedArray(19);
            array[20] = rc.readSharedArray(20);
            array[21] = rc.readSharedArray(21);
            array[22] = rc.readSharedArray(22);
            array[23] = rc.readSharedArray(23);
            array[24] = rc.readSharedArray(24);
            array[25] = rc.readSharedArray(25);
            array[26] = rc.readSharedArray(26);
            array[27] = rc.readSharedArray(27);
            array[28] = rc.readSharedArray(28);
            array[29] = rc.readSharedArray(29);
            array[30] = rc.readSharedArray(30);
            array[31] = rc.readSharedArray(31);
            array[32] = rc.readSharedArray(32);
            array[33] = rc.readSharedArray(33);
            array[34] = rc.readSharedArray(34);
            array[35] = rc.readSharedArray(35);
            array[36] = rc.readSharedArray(36);
            array[37] = rc.readSharedArray(37);
            array[38] = rc.readSharedArray(38);
            array[39] = rc.readSharedArray(39);
            array[40] = rc.readSharedArray(40);
            array[41] = rc.readSharedArray(41);
            array[42] = rc.readSharedArray(42);
            array[43] = rc.readSharedArray(43);
            array[44] = rc.readSharedArray(44);
            array[45] = rc.readSharedArray(45);
            array[46] = rc.readSharedArray(46);
            array[47] = rc.readSharedArray(47);
            array[48] = rc.readSharedArray(48);
            array[49] = rc.readSharedArray(49);
            array[50] = rc.readSharedArray(50);
            array[51] = rc.readSharedArray(51);
            array[52] = rc.readSharedArray(52);
            array[53] = rc.readSharedArray(53);
            array[54] = rc.readSharedArray(54);
            array[55] = rc.readSharedArray(55);
            array[56] = rc.readSharedArray(56);
            array[57] = rc.readSharedArray(57);
            array[58] = rc.readSharedArray(58);
            array[59] = rc.readSharedArray(59);
            array[60] = rc.readSharedArray(60);
            array[61] = rc.readSharedArray(61);
            array[62] = rc.readSharedArray(62);
            array[63] = rc.readSharedArray(63);
        }
    }


    public static int locToInt(MapLocation loc) {
        return loc.x + (loc.y << 6);
    }

    public static MapLocation intToLoc(int i) {
        return new MapLocation(i % 64, (i >> 6) % 64);
    }
    
    public static void panicReportEnemy(RobotController rc, MapLocation loc) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return;
        array[ENEMY_LOCATION] = (1 << 12) + locToInt(loc);
        rc.writeSharedArray(ENEMY_LOCATION, array[ENEMY_LOCATION]);
    }
    
    public static MapLocation getReportedEnemy(RobotController rc) throws GameActionException {
        return array[ENEMY_LOCATION] == 0 ? null : intToLoc(array[ENEMY_LOCATION]);
    }
    
    public static void clearReportEnemy(RobotController rc) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return;
        rc.writeSharedArray(ENEMY_LOCATION, 0);
    }

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

    public static void reportIsland(RobotController rc, MapCache cache) throws GameActionException {
        if (!rc.canWriteSharedArray(0, 0)) return;
        for (int i = ISLAND_REPORT_START; i < ISLAND_REPORT_START + ISLAND_REPORT_SIZE; i++) {
            if (array[i] == 0) {
                MapCache.IslandData sample = cache.sampleIslandCache(true);
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

    public static void cycleIslands(RobotController rc, MapCache cache) throws GameActionException {
        for (int i = ISLAND_START; i < ISLAND_START + ISLAND_SIZE; i++) {
            MapCache.IslandData sample = cache.sampleIslandCache(false);
            if (sample == null) break;
            array[i] = sample.encode() + 1;
            rc.writeSharedArray(i, array[i]);
        }

        for (int i = ISLAND_REPORT_START; i < ISLAND_REPORT_START + ISLAND_REPORT_SIZE; i++) {
            array[i] = 0;
            rc.writeSharedArray(i, 0);
        }
    }

    public static void readWells(RobotController rc, MapCache cache) {
        for (int i = WELL_START; i < WELL_START + WELL_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateWellCacheFromComms(array[i] - 1);
            }
        }
    }

    public static void readIslands(RobotController rc, MapCache cache) {
        for (int i = ISLAND_START; i < ISLAND_START + ISLAND_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateIslandCacheFromComms(array[i] - 1);
            }
        }
    }

    public static void readReportingWells(RobotController rc, MapCache cache) {
        for (int i = WELL_REPORT_START; i < WELL_REPORT_START + WELL_REPORT_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateWellCacheFromComms(array[i] - 1);
            }
        }
    }

    public static void readReportingIslands(RobotController rc, MapCache cache) {
        for (int i = ISLAND_REPORT_START; i < ISLAND_REPORT_START + ISLAND_REPORT_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateIslandCacheFromComms(array[i] - 1);
            }
        }
    }
    
    /*
    public static void readEnemies(RobotController rc, MapCache cache) {
        for (int i = ENEMIES_START; i < ENEMIES_START + ENEMIES_SIZE; i++) {
            if (array[i] != 0) {
                cache.updateEnemyCacheFromComms(array[i] - 1);
            }
        }
    }
    
    static final int turnMask = 1 << 15;

    public static void incrementEnemies(RobotController rc, MapCache cache) {
        for (int i = ENEMIES_START; i < ENEMIES_START + ENEMIES_SIZE; i++) {
            if (array[i] != 0) {
                if (array[i] & turnMask) {
                    array[i] = 0;
                    rc.writeSharedArray(i, 0);
                } else {
                    array[i] |= turnMask;
                    rc.writeSharedArray(i, array[i]);
                }
            }
        }
    }

    public static void reportEnemies(RobotController rc, MapCache cache) {
        if (!rc.canWriteSharedArray(0, 0)) return;
        MapCache.EnemyData edata;
        int round = rc.getRoundNum();
        int idx = 0;
        for (int i = 0; i < ENEMY_CACHE_SIZE; i++) {
            edata = enemyCache[i];
            if (edata != null && edata.roundSeen == round) {
                while (array[idx] != 0) {
                    
                }
            }
        }
    }*/

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

    /**
     * Gets the ith HQ's location by turn order.
     */
    public static MapLocation getHQ(RobotController rc, int i) throws GameActionException {
        if (array[HQ_INDEX + i] > 0) {
            return intToLoc(array[HQ_INDEX + i]);
        }
        return null;
    }

    public static MapLocation[] getHQs(RobotController rc) throws GameActionException {
        int numHQs = 1;
        while (array[HQ_INDEX + numHQs] > 0 && numHQs < GameConstants.MAX_STARTING_HEADQUARTERS) {
            numHQs++;
        }
        MapLocation[] hqs = new MapLocation[numHQs];
        for (int i = 0; i < numHQs; i++) {
            hqs[i] = Communications.getHQ(rc, i);
        }
        return hqs;
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
    
    public static void tryAddEnemySnipe(RobotController rc, MapLocation location) throws GameActionException {
        if (rc.canWriteSharedArray(0, 0)) { // should always be able to write
            for(int i = ENEMY_SNIPE_START; i < 64; i++) {
                if(array[i] == 0) {
                    array[i] = (1 << 12) + locToInt(location);
                    rc.writeSharedArray(i, array[i]);
                }
            }
        }
    }
}
