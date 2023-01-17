package sphere;

import battlecode.common.*;

public strictfp class Micro {

    static MicroInfo[] mi;
    static RobotController rc;
    static MapLocation curr;
    
    static RobotType myType;
    static Team myTeam;
    static int myActionRange;
    static double myBaseDPS;
    static boolean hurt;
    static boolean canAttack;
    
    static Team enemyTeam;
    static double robotDPS;
    static int robotActionRadius;
    static int robotActionRadiusExtended;

    static double[] baseDPS = {0, 0, 0, 0, 0, 0};
    static int[] actionRadiusExtended = {0, 0, 0, 0, 0, 0};
    static int[] hurtHealth = {6, 6, 6, 6, 6, 6};


    static Direction[] dirs = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER,
    };

    public Micro(RobotController rc) {
        this.rc = rc;
        myType = rc.getType();
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        switch (myType) {
            case LAUNCHER:
                myActionRange = RobotType.LAUNCHER.actionRadiusSquared;
                break;
        }

        baseDPS[RobotType.LAUNCHER.ordinal()] = 6;
        actionRadiusExtended[RobotType.LAUNCHER.ordinal()] = 26;
        //baseDPS[RobotType.CARRIER.ordinal()] = 1;
        //actionRadiusExtended[RobotType.CARRIER.ordinal()] = 18;
        myBaseDPS = baseDPS[myType.ordinal()];

        mi = new MicroInfo[9];
    }

    class MicroInfo {
        Direction d;
        MapLocation l;
        boolean canMove;
        int minDistToEnemy = 9001;
        double receivedDPS = 0;
        double targetingDPS = 0;
        double allyDPS = 0;

        public MicroInfo(Direction d) throws GameActionException {
            this.d = d;
            l = curr.add(d);
            canMove = d == Direction.CENTER || rc.canMove(d);
            if (canMove && !hurt) {
                allyDPS = myBaseDPS / rc.senseMapInfo(l).getCooldownMultiplier(myTeam);
            }
        }

        void updateEnemy(RobotInfo robot) {
            if (!canMove) return;
            int dist = robot.location.distanceSquaredTo(l);
            if (dist < minDistToEnemy) minDistToEnemy = dist;
            if (dist <= robotActionRadiusExtended) {
                targetingDPS += robotDPS;
                if (dist <= robotActionRadius) receivedDPS += robotDPS;
            }
        }

        void updateAlly(RobotInfo robot) {
            if (!canMove) return;
            allyDPS += robotDPS / robot.location.distanceSquaredTo(l);
        }

        int safety() {
            if (receivedDPS > 0) return 0;
            if (targetingDPS < allyDPS) return 1;
            return 2;
        }

        boolean betterThan(MicroInfo other) {
            if (!other.canMove) return true;
            if (safety() > other.safety()) return true;
            boolean inRange = hurt || minDistToEnemy < myActionRange;
            boolean otherInRange = hurt || other.minDistToEnemy < myActionRange;
            if (inRange && !otherInRange) return true;
            if (!inRange && otherInRange) return false;
            if (!hurt) {
                if (allyDPS > other.allyDPS) return true;
                if (allyDPS < other.allyDPS) return false;
            }
            if (inRange) return minDistToEnemy >= other.minDistToEnemy;
            return minDistToEnemy <= other.minDistToEnemy;
        }
    }


    boolean doMicro() throws GameActionException {
        curr = rc.getLocation();
        hurt = rc.getHealth() <= hurtHealth[myType.ordinal()];
        canAttack = rc.isActionReady();
        mi[0] = new MicroInfo(Direction.NORTH);
        mi[1] = new MicroInfo(Direction.NORTHEAST);
        mi[2] = new MicroInfo(Direction.EAST);
        mi[3] = new MicroInfo(Direction.SOUTHEAST);
        mi[4] = new MicroInfo(Direction.SOUTH);
        mi[5] = new MicroInfo(Direction.SOUTHWEST);
        mi[6] = new MicroInfo(Direction.WEST);
        mi[7] = new MicroInfo(Direction.NORTHWEST);
        mi[8] = new MicroInfo(Direction.CENTER);

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
        for (RobotInfo robot : nearbyRobots) {
            if (robot.team == myTeam) {
                robotDPS = baseDPS[robot.type.ordinal()] / rc.senseMapInfo(robot.location).getCooldownMultiplier(myTeam);
                mi[0].updateAlly(robot);
                mi[1].updateAlly(robot);
                mi[2].updateAlly(robot);
                mi[3].updateAlly(robot);
                mi[4].updateAlly(robot);
                mi[5].updateAlly(robot);
                mi[6].updateAlly(robot);
                mi[7].updateAlly(robot);
                mi[8].updateAlly(robot);
            } else {
                robotDPS = baseDPS[robot.type.ordinal()] / rc.senseMapInfo(robot.location).getCooldownMultiplier(enemyTeam);
                robotActionRadius = robot.type.actionRadiusSquared;
                robotActionRadiusExtended = actionRadiusExtended[robot.type.ordinal()];
                mi[0].updateEnemy(robot);
                mi[1].updateEnemy(robot);
                mi[2].updateEnemy(robot);
                mi[3].updateEnemy(robot);
                mi[4].updateEnemy(robot);
                mi[5].updateEnemy(robot);
                mi[6].updateEnemy(robot);
                mi[7].updateEnemy(robot);
                mi[8].updateEnemy(robot);
            }
        }

        MicroInfo best = mi[8];
        if (mi[7].betterThan(best)) best = mi[7];
        if (mi[6].betterThan(best)) best = mi[6];
        if (mi[5].betterThan(best)) best = mi[5];
        if (mi[4].betterThan(best)) best = mi[4];
        if (mi[3].betterThan(best)) best = mi[3];
        if (mi[2].betterThan(best)) best = mi[2];
        if (mi[1].betterThan(best)) best = mi[1];
        if (mi[0].betterThan(best)) best = mi[0];

        if (best.d == Direction.CENTER) return false;
        if (best.safety() > mi[8].safety() || rc.getRoundNum() % 2 == 0) {
            if (rc.canMove(best.d)) {
                rc.move(best.d);
                return true;
            }
        }
        return false;
    }
}
