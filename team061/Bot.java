package team061;

import java.util.*;

import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static MapLocation here; // bot classes are responsible for keeping this up to date
    protected static Random rand;
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();

		here = rc.getLocation();
		rand = new Random(rc.getID());
		//System.out.println(MessageEncode.TURRET_TARGET.decode(MessageEncode.TURRET_TARGET.encode(new int[]{69, RobotType.SOLDIER.ordinal(), 1234,4321}))[3]);
	}
	/*
    public static boolean inEnemyTowerOrHQRange(MapLocation loc, MapLocation[] enemyArchons) {
        if (loc.distanceSquaredTo(theirHQ) <= 52) {
            switch (enemyArchons.length) {
                case 6:
                case 5:
                    // enemy HQ has range of 35 and splash
                    if (loc.add(loc.directionTo(theirHQ)).distanceSquaredTo(theirHQ) <= 35) return true;
                    break;

                case 4:
                case 3:
                case 2:
                    // enemy HQ has range of 35 and no splash
                    if (loc.distanceSquaredTo(theirHQ) <= 35) return true;
                    break;

                case 1:
                case 0:
                default:
                    // enemyHQ has range of 24;
                    if (loc.distanceSquaredTo(theirHQ) <= 24) return true;
                    break;
            }
        }

        for (MapLocation archon : enemyArchons) {
            if (loc.distanceSquaredTo(archon) <= RobotType.ARCHON.attackRadiusSquared) return true;
        }

        return false;
	 */
}