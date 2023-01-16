package sphere;

import battlecode.common.*;

public strictfp class HQLocations {

    MapLocation[] hqs;

    MapLocation[] hqs_vsymm;
    MapLocation[] hqs_hsymm;
    MapLocation[] hqs_rsymm;

    Team team;

    boolean v; // true if it is possible to be vertically symmetric
    boolean h; // true if it is possible to be horizontally symmetric
    boolean r; // true if it is possible to be rotationally symmetric
    int rushIndex;
    RobotController rc;

    public HQLocations(RobotController rc) throws GameActionException {
        Communications.readArray(rc);

        hqs = Communications.getHQs(rc);

        v = true;
        h = true;
        r = true;

        this.team = rc.getTeam();
        this.rc = rc;

        hqs_vsymm = new MapLocation[hqs.length];
        hqs_hsymm = new MapLocation[hqs.length];
        hqs_rsymm = new MapLocation[hqs.length];

        for (int i = hqs.length; --i >= 0;) {
            hqs_vsymm[i] = reflectVertical(hqs[i]);
            hqs_hsymm[i] = reflectHorizontal(hqs[i]);
            hqs_rsymm[i] = reflectRotational(hqs[i]);
        }

    }

    public void updateHQSymms(RobotController rc) throws GameActionException {
        RobotInfo sensed;
        for (int i = hqs.length; --i >= 0;) {
            if (v && rc.canSenseLocation(hqs_vsymm[i])) {
                sensed = rc.senseRobotAtLocation(hqs_vsymm[i]);
                if (sensed == null || sensed.type != RobotType.HEADQUARTERS || sensed.team == team) {
                    v = false;
                }
            }
            if (h && rc.canSenseLocation(hqs_hsymm[i])) {
                sensed = rc.senseRobotAtLocation(hqs_hsymm[i]);
                if (sensed == null || sensed.type != RobotType.HEADQUARTERS || sensed.team == team) {
                    h = false;
                }
            }
            if (r && rc.canSenseLocation(hqs_rsymm[i])) {
                sensed = rc.senseRobotAtLocation(hqs_rsymm[i]);
                if (sensed == null || sensed.type != RobotType.HEADQUARTERS || sensed.team == team) {
                    r = false;
                }
            }
        }
    }

    public MapLocation reflectHorizontal(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - loc.x - 1, loc.y);
    }

    public MapLocation reflectVertical(MapLocation loc) {
        return new MapLocation(loc.x, rc.getMapHeight() - loc.y - 1);
    }

    public MapLocation reflectRotational(MapLocation loc) {
        return new MapLocation(rc.getMapWidth() - loc.x - 1, rc.getMapHeight() - loc.y - 1);
    }

    public MapLocation getHQRushLocation(RobotController rc) {
        if (r) {
            MapLocation ret = hqs_rsymm[rushIndex];
            rushIndex = (rushIndex + 1) % hqs.length;
            return ret;
        }
        if (h) {
            MapLocation ret = hqs_hsymm[rushIndex];
            rushIndex = (rushIndex + 1) % hqs.length;
            return ret;
        }
        if (v) {
            MapLocation ret = hqs_vsymm[rushIndex];
            rushIndex = (rushIndex + 1) % hqs.length;
            return ret;
        }
        return null;
    }

}
