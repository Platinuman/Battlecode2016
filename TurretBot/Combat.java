package TurretBot;

import battlecode.common.*;
import TurretBot.Util;

public class Combat extends Bot {
	public static void shootAtNearbyEnemies() throws GameActionException {
		RobotType type = rc.getType();
		int attackRadiusSq = type.attackRadiusSquared;
		RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE);
		RobotInfo target = chooseTarget(enemies,0);
		if (target == null){
			target = chooseTarget(rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE),0);
		}
		if (target == null){
			target = chooseTarget(rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE),0);
		}
		if (target != null) {
			rc.attackLocation(target.location);
		}
	}

	public static RobotInfo chooseTarget(RobotInfo[] enemies, int isScout) {
		double minHealth = 999999;
		double maxPower = -1;
		RobotInfo target = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type.attackPower > maxPower && (rc.canAttackLocation(enemy.location)) || isScout == 1) {
				maxPower = enemy.type.attackPower;
				minHealth = enemy.health;
				target = enemy;
			} else if (enemy.attackPower == maxPower && enemy.health < minHealth && (rc.canAttackLocation(enemy.location)) || isScout == 1) {
				minHealth = enemy.health;
				target = enemy;
			}
		}
		return target;
	}

	public static boolean isSafe(MapLocation loc) {

		RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, type.sensorRadiusSquared, them);
		for (RobotInfo enemy : potentialAttackers) {
			if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
				return false;
			}
		}

		return true;
	}

	public static void retreat() throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canMove(dir) && isSafe(here.add(dir))) {
				rc.move(dir);
				return;
			}
		}
	}
}