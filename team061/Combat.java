package team061;

import battlecode.common.*;

public class Combat extends Bot {
	public static void shootAtNearbyEnemies(RobotType type) throws GameActionException {
		int attackRadiusSq = rc.getType().attackRadiusSquared;
		RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, them);
		RobotInfo[] zombies = rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE);

		RobotInfo target = chooseTarget(enemies);
		RobotInfo zombieTarget = chooseTarget(zombies);
		if (zombieTarget != null
				&& (type == RobotType.GUARD || target == null || zombieTarget.type == RobotType.ZOMBIEDEN)) {
			target = zombieTarget;
		}

		if (target != null) {
			rc.attackLocation(target.location);
		}
	}

	public static RobotInfo chooseTarget(RobotInfo[] enemies) {
		double minHealth = 999999;
		double maxPower = -1;
		RobotInfo target = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type.attackPower > maxPower) {
				maxPower = enemy.type.attackPower;
				minHealth = enemy.health;
				target = enemy;
			} else if (enemy.attackPower == maxPower && enemy.health < minHealth) {
				minHealth = enemy.health;
				target = enemy;
			}
		}
		return target;
	}

	public static boolean isSafe(MapLocation loc) {

		RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
		for (RobotInfo enemy : potentialAttackers) {
			if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
				return false;
			}
		}

		return true;
	}

	public static void retreat(MapLocation[] enemyArchons) throws GameActionException {
		Direction[] retreatDirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
				Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST };

		for (Direction dir : retreatDirs) {
			if (rc.canMove(dir) && isSafe(here.add(dir))) {
				rc.move(dir);
				return;
			}
		}
	}
}