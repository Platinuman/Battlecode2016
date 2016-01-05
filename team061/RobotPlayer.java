package team061;

import battlecode.common.*;

public class RobotPlayer {
    
    public static void run(RobotController rc) throws exception {
        Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
	switch (rc.getType()){
	case ARCHON:
		botArchon.lopp(rc);
		break;
	
	case GUARD:
		botGuard.lopp(rc);
		break;

	case SCOUT:
		botScout.lopp(rc);
		break;
	
	case SOLDIER:
		botSoldier.lopp(rc);
		break;

	case TURRET:
		botTurret.lopp(rc);
		break;

	case VIPER:
		botViper.lopp(rc);
		break;
	default:
		throw new Exception("Unknown robot type!");
        }
	}
    }
}
