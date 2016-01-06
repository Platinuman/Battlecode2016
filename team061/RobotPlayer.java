package team061;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController rc) throws Exception {
		Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
				Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
		RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
				RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET };
		Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();
		switch (rc.getType()) {
		case ARCHON:
			BotArchon.loop(rc);
			break;

		case GUARD:
			BotGuard.loop(rc);
			break;

		case SCOUT:
			BotScout.loop(rc);
			break;

		case SOLDIER:
			BotSoldier.loop(rc);
			break;

		case TURRET:
			BotTurret.loop(rc);
			break;

		case VIPER:
			BotViper.loop(rc);
			break;
		default:
			throw new Exception("Unknown robot type!");
		}
	}
}
