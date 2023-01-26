package torus1_25_2;

import battlecode.common.*;

public strictfp class RobotPlayer {

    static int turnCount = 0;
    
    public static void run(RobotController rc) throws GameActionException {
        Robot robot = null;
        switch (rc.getType()) {
            case HEADQUARTERS:
                robot = new HeadquartersRobot(rc);
                break;
            case CARRIER:
                robot = new CarrierRobot(rc);
                break;
            case LAUNCHER:
                robot = new LauncherRobot(rc);
                break;
            case BOOSTER:
                robot = new BoosterRobot(rc);
                break;
            case DESTABILIZER:
                robot = new DestabilizerRobot(rc);
                break;
            case AMPLIFIER:
                robot = new AmplifierRobot(rc);
                break;
        }

        while (true) {
            int round = 0;
            try {
                round = rc.getRoundNum();
                robot.run();
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                if (round != rc.getRoundNum()) {
                    System.out.println("Oh no! I've run out of bytecodes :O");
                }
                Clock.yield();
            }
            turnCount += 1;
        }
    }

}
