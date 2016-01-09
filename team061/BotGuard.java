package team061;

import battlecode.common.*;
import java.util.Random;

public class BotGuard extends Bot {
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		// Debug.init("micro");
		while (true) {
			try {
				turn(rand);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void turn(Random rand) throws GameActionException {
		here = rc.getLocation();
		int myAttackRange = rc.getType().attackRadiusSquared;
		Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();
		RobotInfo[] enemies = rc.senseNearbyRobots(myAttackRange,them);
		RobotInfo[] zombies = rc.senseNearbyRobots(myAttackRange,Team.ZOMBIE);

		// If this robot type can attack, check for enemies within range and
		// attack one
		if (rc.isWeaponReady()) {
			Combat.shootAtNearbyEnemies();
		}
/*
		if (rc.isCoreReady() && enemies.length == 0 && zombies.length == 0) {
			if (fate < 600) {
				// Choose a random direction to try to move in
				Direction dirToMove = directions[fate % 8];
				// Check the rubble in that direction
				if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
					// Too much rubble, so I should clear it
					rc.clearRubble(dirToMove);
					// Check if I can move in this direction
				} else if (rc.canMove(dirToMove)) {
					// Move
					rc.move(dirToMove);
				}
			}
		}*/

	}
}