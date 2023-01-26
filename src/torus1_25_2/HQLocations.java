package torus1_25_2;

import battlecode.common.*;

public strictfp class HQLocations {

    static MapLocation[] hqs;

    static MapLocation[] hqs_vsymm;
    static MapLocation[] hqs_hsymm;
    static MapLocation[] hqs_rsymm;
    static int[] vsymm_visited;
    static int[] hsymm_visited;
    static int[] rsymm_visited;

    static Team team;

    static boolean v; // true if it is possible to be vertically symmetric
    static boolean h; // true if it is possible to be horizontally symmetric
    static boolean r; // true if it is possible to be rotationally symmetric
    static int rushIndex;
    static RobotController rc;
    
    static StringBuilder sensed;
    static StringBuilder passable;
    static StringBuilder notPassable;
    static StringBuilder clouds;
    static StringBuilder noClouds;
    static int mapWidth;
    static int mapHeight;

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

        vsymm_visited = new int[hqs.length];
        hsymm_visited = new int[hqs.length];
        rsymm_visited = new int[hqs.length];

        for (int i = hqs.length; --i >= 0;) {
            hqs_vsymm[i] = reflectVertical(hqs[i]);
            hqs_hsymm[i] = reflectHorizontal(hqs[i]);
            hqs_rsymm[i] = reflectRotational(hqs[i]);
        }

        passable = new StringBuilder();
        notPassable = new StringBuilder();
        clouds = new StringBuilder();
        noClouds = new StringBuilder();
        sensed = new StringBuilder();
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
    }

    public void updateHQSymms(RobotController rc) throws GameActionException {
        if (isSymmetryDetermined()) return;
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

    public void eliminateSymms(RobotController rc) throws GameActionException {
        if (isSymmetryDetermined()) return;
        MapLocation m;
        int rx;
        int ry;
        MapLocation curr = rc.getLocation();
        for (MapInfo mi : rc.senseNearbyMapInfos()) {
            m = mi.getMapLocation();

            if (m.distanceSquaredTo(curr) < 13) continue;

            if (sensed.indexOf(m.toString()) >= 0) continue;

            sensed.append(m.toString()).append("|");
            
            rx = mapWidth - m.x - 1;
            ry = mapHeight - m.y - 1;
            if (mi.isPassable()) {
                passable.append(m);

                if (r && notPassable.indexOf(new MapLocation(rx, ry).toString()) >= 0) {
                    r = false;
                    //System.out.println(m.toString() + " passable eliminated rotational (" + rx + ", " + (ry60 / 60) + " not passable)");
                }
                if (v && notPassable.indexOf(new MapLocation(m.x, ry).toString()) >= 0) {
                    v = false;
                    //System.out.println(m.toString() + " passable eliminated vertical (" + m.x + ", " +  (ry60 / 60) + " not passable)");
                }
                if (h && notPassable.indexOf(new MapLocation(rx, m.y).toString()) >= 0) {
                    h = false;
                    //System.out.println(m.toString() + " passable eliminated horizontal (" + rx + ", " + m.y + " not passable)");
                }
            } else {
                notPassable.append(m);

                if (r && passable.indexOf(new MapLocation(rx, ry).toString()) >= 0) {
                    r = false;
                    //System.out.println(m.toString() + " not passable eliminated rotational (" + rx + ", " + (ry60 / 60) + " passable)");
                }
                if (v && passable.indexOf(new MapLocation(m.x, ry).toString()) >= 0) {
                    v = false;
                    //System.out.println(m.toString() + " not passable eliminated vertical (" + m.x + ", " +  (ry60 / 60) + " passable)");
                }
                if (h && passable.indexOf(new MapLocation(rx, m.y).toString()) >= 0) {
                    h = false;
                    //System.out.println(m.toString() + " not passable eliminated horizontal (" + rx + ", " + m.y + " passable)");
                }
            }
            /*if (mi.hasCloud()) {
                clouds.append(m.x + 60 * m.y).append("|");

                if (r && noClouds.indexOf((rx + 60 * ry)) >= 0) {
                    r = false;
                }
                if (v && noClouds.indexOf((m.x + 60 * ry)) >= 0) {
                    v = false;
                }
                if (h && noClouds.indexOf((rx + 60 * m.y)) >= 0) {
                    h = false;
                }
            } else {
                noClouds.append(m.x + 60 * m.y).append("|");

                if (r && clouds.indexOf((rx + 60 * ry)) >= 0) {
                    r = false;
                }
                if (v && clouds.indexOf((m.x + 60 * ry)) >= 0) {
                    v = false;
                }
                if (h && clouds.indexOf((rx + 60 * m.y)) >= 0) {
                    h = false;
                }
            }*/
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
        MapLocation ret = null;
        MapLocation curr = rc.getLocation();
        double bestScore = 1000000;
        double score;
        if (r) {
            for (int i = hqs.length; i-->0;) {
                score = rsymm_visited[i] - 0.9 / hqs_rsymm[i].distanceSquaredTo(curr);
                if (score < bestScore) {
                    ret = hqs_rsymm[i];
                    bestScore = score;
                }
            }
        }
        if (h) {
            for (int i = hqs.length; i-->0;) {
                score = hsymm_visited[i] - 0.9 / hqs_hsymm[i].distanceSquaredTo(curr);
                if (score < bestScore) {
                    ret = hqs_hsymm[i];
                    bestScore = score;
                }
            }
        }
        if (v) {
            for (int i = hqs.length; i-->0;) {
                score = vsymm_visited[i] - 0.9 / hqs_vsymm[i].distanceSquaredTo(curr);
                if (score < bestScore) {
                    ret = hqs_vsymm[i];
                    bestScore = score;
                }
            }
        }
        return ret;
    }

    public void markVisited(MapLocation l) {
        for (int i = hqs.length; i-->0;) {
            if (r && hqs_rsymm[i].equals(l)) {
                rsymm_visited[i]++;
            }
            if (h && hqs_hsymm[i].equals(l)) {
                hsymm_visited[i]++;
            }
            if (v && hqs_vsymm[i].equals(l)) {
                vsymm_visited[i]++;
            }
        }
    }

    public void debugSymms() {
        if (r && !h && !v) {
            System.out.println("Determined symmetry to be rotational!");
        }
        if (!r && h && !v) {
            System.out.println("Determined symmetry to be horizontal!");
        }
        if (!r && !h && v) {
            System.out.println("Determined symmetry to be vertical!");
        }
        if (!r && !h && !v) {
            System.out.println("Something's wrong, all symmetries have been eliminated!");
        }
    }

    public void updateSymmsFromComms() throws GameActionException {
        if (Communications.isRotationalSymmetryEliminated(rc)) {
            r = false;
        } else if (!r) {
            Communications.eliminateRotationalSymmetry(rc);
        }
        if (Communications.isHorizontalSymmetryEliminated(rc)) {
            h = false;
        } else if (!h) {
            Communications.eliminateHorizontalSymmetry(rc);
        }
        if (Communications.isVerticalSymmetryEliminated(rc)) {
            v = false;
        } else if (!v) {
            Communications.eliminateVerticalSymmetry(rc);
        }
    }

    public boolean isSymmetryDetermined() {
        return (r? !h && !v : !h || !v);
        /*if (r) {
            return !h && !v;
        } else {
            return !h || !v;
        }*/
    }

}
