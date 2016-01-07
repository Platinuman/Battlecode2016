package team061;

import battlecode.common.*;
import team061.Util;

public class Combat extends Bot {
	public static RobotInfo scoutChooseTarget(){
		RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, them);
		RobotInfo[] zombies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, Team.ZOMBIE);
		RobotInfo target = chooseTarget(enemies, 1);
		RobotInfo zombieTarget = chooseTarget(zombies, 1);
		if (zombieTarget != null
				&& (target == null || zombieTarget.type == RobotType.ZOMBIEDEN)) {
			target = zombieTarget;
		}
		return target;
	}
	public static void shootBestEnemyTakingIntoAccountScoutInfo(MapLocation[] locations, double[] healths, RobotType[] types) throws GameActionException{
		shootAtNearbyEnemies();
		if(rc.isWeaponReady()){
			double minHealth = 999999;
			double maxPower = -1;
			MapLocation targetLocation = null;
			for (int i = locations.length; i-- > 0;) {
				double attackPower = types[i].attackPower;
				double health = healths[i];
				Team team = Util.getTeam(types[i]);
				MapLocation location = locations[i];
				if (attackPower > maxPower && rc.canAttackLocation(location)) {
					maxPower = attackPower;
					minHealth = health;
					targetLocation = locations[i];
				} else if (attackPower == maxPower && health < minHealth && rc.canAttackLocation(location)) {
					minHealth = health;
					targetLocation = location;
				}
			}
			if(targetLocation != null){
				rc.attackLocation(targetLocation);
			}
		}
	}
	public static void shootAtNearbyEnemies() throws GameActionException {
		RobotType type = rc.getType();
		int attackRadiusSq = type.attackRadiusSquared;
		RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, them);
		RobotInfo[] zombies = rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE);

		RobotInfo target = chooseTarget(enemies,0);
		RobotInfo zombieTarget = chooseTarget(zombies,0);
		if (zombieTarget != null
				&& (type == RobotType.GUARD || target == null || zombieTarget.type == RobotType.ZOMBIEDEN)) {
			target = zombieTarget;
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
