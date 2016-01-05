package team061;

import java.util.Random;

import battlecode.common.*;

public class BotGuard extends Bot {
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		// Debug.init("micro");

		while (true) {
			try {
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		// This is a loop to prevent the run() method from returning. Because of
		// the Clock.yield()
		// at the end of it, the loop will iterate once per game round.
		Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
				Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
		RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
				RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET };
		Random rand = new Random(rc.getID());
		int fate = rand.nextInt(1000);
		int myAttackRange = 0;
		Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();

		boolean shouldAttack = false;

		// If this robot type can attack, check for enemies within range and
		// attack one
		if (myAttackRange > 0) {
			RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
			RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
			if (enemiesWithinRange.length > 0) {
				shouldAttack = true;
				// Check if weapon is ready
				if (rc.isWeaponReady()) {
					rc.attackLocation(enemiesWithinRange[rand.nextInt(enemiesWithinRange.length)].location);
				}
			} else if (zombiesWithinRange.length > 0) {
				shouldAttack = true;
				// Check if weapon is ready
				if (rc.isWeaponReady()) {
					rc.attackLocation(zombiesWithinRange[rand.nextInt(zombiesWithinRange.length)].location);
				}
			}
		}

		if (!shouldAttack) {
			if (rc.isCoreReady()) {
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
			}
		}
	}
}