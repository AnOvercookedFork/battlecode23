package torus1_23_3;

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
    static boolean shouldCharge;
    static boolean shouldMoveLeader;
    static boolean dangerousEnemiesInSight;
    static MapLocation leader;
    static MapLocation target;
    
    static Team enemyTeam;
    static double robotDPS;
    static int robotActionRadius;
    static int robotActionRadiusExtended;
    static RobotInfo[] prevRobots = null;

    static double[] baseDPS = {0, 0, 0, 0, 0, 0};
    static int[] actionRadiusExtended = {0, 0, 0, 0, 0, 0};
    static int[] hurtHealth = {40, 40, 40, 40, 40, 40};


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

        baseDPS[RobotType.LAUNCHER.ordinal()] = 20;
        actionRadiusExtended[RobotType.LAUNCHER.ordinal()] = 26;
        //baseDPS[RobotType.CARRIER.ordinal()] = 1;
        //actionRadiusExtended[RobotType.CARRIER.ordinal()] = 18;
        myBaseDPS = baseDPS[myType.ordinal()];

        mi = new MicroInfo[9];
    }

    class MicroInfo {
        Direction d;
        MapLocation l; // location after moving
        MapLocation after; // location after turn ends and pushed by current
        boolean canMove;
        int minDistToEnemy = 9001;
        int minDistToDangerousEnemy = 9001;
        /*double receivedDPS = 0;
        double targetingDPS = 0;
        double allyDPS = 0;*/
        int inRange = 0;
        double myDps = 0;
        int enemiesAttacking = 0;
        int enemiesTargeting = 0;
        int potentialAttackers = 0;
        double leaderHealth = 0;
        int leaderDist = 0;
        int targetDist = 0;

        public MicroInfo(Direction d) throws GameActionException {
            this.d = d;
            l = curr.add(d);
            canMove = d == Direction.CENTER || rc.canMove(d);
            if (canMove) {
                if (rc.canSenseLocation(l)) {
                    MapInfo mi = rc.senseMapInfo(l);
                    after = l.add(mi.getCurrentDirection());
                    if (canAttack && !hurt) {
                        myDps = myBaseDPS / mi.getCooldownMultiplier(myTeam);
                    }
                } else {
                    after = l;
                    myDps = myBaseDPS;
                }

                if (leader != null) leaderDist = l.distanceSquaredTo(leader);
                if (target != null) targetDist = l.distanceSquaredTo(target);
            }
        }

        void updateEnemy(RobotInfo robot) {
            if (!canMove) return;
            if (inRange < 2 && robot.location.isWithinDistanceSquared(l, myActionRange)) inRange = 2;
            int dist = robot.location.distanceSquaredTo(after);
            if (dist < minDistToEnemy) minDistToEnemy = dist;
            if (robot.type.damage > 0) {
                if (dist < minDistToDangerousEnemy) minDistToDangerousEnemy = dist;

                if (dist <= robotActionRadius) {
                    enemiesAttacking++;
                    enemiesTargeting++;
                } else if (robot.location.add(robot.location.directionTo(after)).distanceSquaredTo(after)
                        <= robotActionRadius) {
                    enemiesTargeting++;
                }
            }
        }

        void updatePotentialEnemy(RobotInfo robot, MapLocation location) {
            if (!canMove) return;
            if (inRange == 0 && location.isWithinDistanceSquared(l, myActionRange)) inRange = 1;
            if (location.isWithinDistanceSquared(after, robotActionRadius)) potentialAttackers++;
        }

        /*void updateAlly(RobotInfo robot) {
            if (!canMove) return;
            //if (robot.location.isWithinDistanceSquared(curr, 5)) allyDPS += robotDPS;
            //allyDPS += robotDPS;
            //allyDPS += robotDPS / robot.location.distanceSquaredTo(l);
            if (robot.type == RobotType.LAUNCHER) {
                double health = robot.health + 1.0 / robot.ID;
                if (health > leaderHealth) {
                    leaderDist = robot.location.distanceSquaredTo(l);
                    leaderHealth = health;
                }
            }
        }*/

        double safety() {
            //if (hurt) {
                if (enemiesAttacking > 0) return 0.9 / enemiesAttacking;
                if (enemiesTargeting > 0) return 1 + 0.9 / enemiesTargeting;
                if (potentialAttackers > 0) return 2 + 0.9 / potentialAttackers;
                return 3;
            /*} else {
                if (enemiesAttacking > 0) return 1;
                if (!canAttack && potentialAttackers > 0) return 2;
                return 3;
            }*/
            /*if (enemiesAttacking > 0) return (d == Direction.CENTER ? 1 : 0);
            if (enemiesTargeting > 0) return (d == Direction.CENTER ? 2 : 1);
            return (d == Direction.CENTER ? 2 : 1);*/
        }

        boolean betterThan(MicroInfo other) {
            if (!canMove) return false;
            if (!other.canMove) return true;


            if (shouldCharge) {
                if (inRange > other.inRange) return true;
                if (inRange < other.inRange) return false;
            }

            if (safety() > other.safety()) return true;
            if (safety() < other.safety()) return false;

            if (enemiesAttacking > 0) {
                if (minDistToDangerousEnemy > other.minDistToDangerousEnemy) return true;
                if (minDistToDangerousEnemy < other.minDistToDangerousEnemy) return false;
            }

            if (!dangerousEnemiesInSight || shouldMoveLeader) {
                if (leaderDist < other.leaderDist) return true;
                if (leaderDist > other.leaderDist) return false;
            }


            if (inRange < 2) {
                if (minDistToEnemy < other.minDistToEnemy) return true;
                if (minDistToEnemy > other.minDistToEnemy) return false;
                if (targetDist < other.targetDist) return true;
                if (other.targetDist < targetDist) return false;
            }

            
            //if (myDps > other.myDps) return true;
            //if (myDps < other.myDps) return false;
            if (inRange == 2) return minDistToEnemy >= other.minDistToEnemy;
            return minDistToEnemy <= other.minDistToEnemy;
        }
    }


    boolean doMicro(MapLocation targ, MapLocation lead, RobotInfo[] prev) throws GameActionException {
        curr = rc.getLocation();
        hurt = rc.getHealth() <= hurtHealth[myType.ordinal()];
        canAttack = rc.isActionReady();
        shouldCharge = canAttack && rc.getRoundNum() % 3 == 1 && !hurt && lead != null;
        if (lead != null) shouldMoveLeader = canAttack && rc.getRoundNum() % 3 != 0 && curr.distanceSquaredTo(lead) >= 2;
        else shouldMoveLeader = false;
        leader = lead;
        target = targ;
        dangerousEnemiesInSight = false;
        prevRobots = prev;

        StringBuilder robotIDs = new StringBuilder();

        mi[0] = new MicroInfo(Direction.NORTH);
        mi[1] = new MicroInfo(Direction.NORTHEAST);
        mi[2] = new MicroInfo(Direction.EAST);
        mi[3] = new MicroInfo(Direction.SOUTHEAST);
        mi[4] = new MicroInfo(Direction.SOUTH);
        mi[5] = new MicroInfo(Direction.SOUTHWEST);
        mi[6] = new MicroInfo(Direction.WEST);
        mi[7] = new MicroInfo(Direction.NORTHWEST);
        mi[8] = new MicroInfo(Direction.CENTER);

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot : nearbyRobots) {
            //if (robot.team != myTeam) {
            /*
                //robotDPS = baseDPS[robot.type.ordinal()] / rc.senseMapInfo(robot.location).getCooldownMultiplier(myTeam);
                //robotDPS = baseDPS[robot.type.ordinal()];
                mi[0].updateAlly(robot);
                mi[1].updateAlly(robot);
                mi[2].updateAlly(robot);
                mi[3].updateAlly(robot);
                mi[4].updateAlly(robot);
                mi[5].updateAlly(robot);
                mi[6].updateAlly(robot);
                mi[7].updateAlly(robot);
                mi[8].updateAlly(robot);
            } else {*/
                //robotDPS = baseDPS[robot.type.ordinal()] / rc.senseMapInfo(robot.location).getCooldownMultiplier(enemyTeam);
                //robotDPS = baseDPS[robot.type.ordinal()];
                if (robot.type.damage > 0) {
                    dangerousEnemiesInSight = true;
                    robotIDs.append(robot.ID).append("|");
                }
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
            //}
        }

        if (prevRobots != null && !canAttack) {
            for (RobotInfo robot : prevRobots) {
                if (robot.type.damage > 0 && robotIDs.indexOf(Integer.toString(robot.ID)) < 0) {
                    robotActionRadius = robot.type.actionRadiusSquared;
                    robotActionRadiusExtended = actionRadiusExtended[robot.type.ordinal()];

                    MapLocation l = robot.location;
                    /*do {
                        if (!rc.canSenseLocation(l)) break;
                        switch (robot.location.directionTo(curr)) {
                            case NORTH:
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case NORTHEAST:
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case EAST:
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case SOUTHEAST:
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case SOUTH:
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case SOUTHWEST:
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case WEST:
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                            case NORTHWEST:
                                l = robot.location.add(Direction.NORTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.WEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.NORTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHWEST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.EAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTH);
                                if (!rc.canSenseLocation(l)) break;
                                l = robot.location.add(Direction.SOUTHEAST);
                                if (!rc.canSenseLocation(l)) break;
                                l = null;
                                break;
                        }
                    } while (false);*/
                    if (!rc.canSenseLocation(l)) {
                        System.out.println("updating potential enemies");
                        mi[0].updatePotentialEnemy(robot, l);
                        mi[1].updatePotentialEnemy(robot, l);
                        mi[2].updatePotentialEnemy(robot, l);
                        mi[3].updatePotentialEnemy(robot, l);
                        mi[4].updatePotentialEnemy(robot, l);
                        mi[5].updatePotentialEnemy(robot, l);
                        mi[6].updatePotentialEnemy(robot, l);
                        mi[7].updatePotentialEnemy(robot, l);
                        mi[8].updatePotentialEnemy(robot, l);
                    }
                }
            }
        }
        
        /*
        if (mi[0].canMove) rc.setIndicatorDot(mi[0].l, 0, 60 * mi[0].safety(), 0);
        if (mi[1].canMove) rc.setIndicatorDot(mi[1].l, 0, 60 * mi[1].safety(), 0);
        if (mi[2].canMove) rc.setIndicatorDot(mi[2].l, 0, 60 * mi[2].safety(), 0);
        if (mi[3].canMove) rc.setIndicatorDot(mi[3].l, 0, 60 * mi[3].safety(), 0);
        if (mi[4].canMove) rc.setIndicatorDot(mi[4].l, 0, 60 * mi[4].safety(), 0);
        if (mi[5].canMove) rc.setIndicatorDot(mi[5].l, 0, 60 * mi[5].safety(), 0);
        if (mi[6].canMove) rc.setIndicatorDot(mi[6].l, 0, 60 * mi[6].safety(), 0);
        if (mi[7].canMove) rc.setIndicatorDot(mi[7].l, 0, 60 * mi[7].safety(), 0);
        if (mi[8].canMove) rc.setIndicatorDot(mi[8].l, 0, 60 * mi[8].safety(), 0);
        */

        MicroInfo best = mi[8];
        
        if (mi[7].betterThan(best)) best = mi[7];
        if (mi[6].betterThan(best)) best = mi[6];
        if (mi[5].betterThan(best)) best = mi[5];
        if (mi[4].betterThan(best)) best = mi[4];
        if (mi[3].betterThan(best)) best = mi[3];
        if (mi[2].betterThan(best)) best = mi[2];
        if (mi[1].betterThan(best)) best = mi[1];
        if (mi[0].betterThan(best)) best = mi[0];
        
        rc.setIndicatorString("Best Dir: " + best.d);

        if (best.d == Direction.CENTER) return false;
        if (rc.canMove(best.d)) {
            rc.move(best.d);
            return true;
        }
        return false;
    }
}
