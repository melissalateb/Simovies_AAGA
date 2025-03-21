package algorithms.hunters;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import algorithms.hunters.brains.core.SecondaryBotBaseBrain;
import algorithms.hunters.brains.core.dto.Const;
import algorithms.hunters.statemachine.impl.State;
import algorithms.hunters.statemachine.interfaces.IState;
import characteristics.IFrontSensorResult;
import characteristics.IFrontSensorResult.Types;
import characteristics.IRadarResult;
import characteristics.Parameters;

public class HunterSecondary extends SecondaryBotBaseBrain {
  Boolean collisionDetected = false;
  protected double targetHeading;
  private boolean detected;
  private int moveacount = 200;
  private int angleturn = 1;
  private int move_back_count = 100;
  private boolean bullet_detected = false;

  @Override
  protected IState buildStateMachine() {
    IState initState = new State();
    initState.setDescription("Init State");
    IState MoveState = new State();
    MoveState.setDescription("Move State");
    IState TurnTowardEnemies = new State();
    TurnTowardEnemies.setDescription("Turn Right");
    IState DetecState = new State();
    DetecState.setDescription("Detect State");
    IState MoveStateTORedect = new State();
    IState STMoveWest = new State();
    STMoveWest.setDescription("Move West");
    IState STTurnWest = new State();
    STTurnWest.setDescription("Turn West");
    IState DeblocState = new State();
    DeblocState.setDescription("Debloc State");
    IState STTurnEast = new State();
    STTurnEast.setDescription("Turn East");
    IState STTurnNorth = new State();
    STTurnNorth.setDescription("Turn North");
    IState STTurnSouth = new State();
    STTurnSouth.setDescription("Turn South");
    MoveStateTORedect.setDescription("Move State To Redect");

    double initStateAngleTarget;

    if (this.leftSide) {
      if (this.currentRobot == Robots.SRUP) {
        initStateAngleTarget = this.getHeading() - (Math.PI / 3);
      } else {
        initStateAngleTarget = this.getHeading() + (Math.PI / 3);
      }
    } else {
      if (this.currentRobot == Robots.SRUP) {
        initStateAngleTarget = this.getHeading() + (Math.PI / 3);
      } else {
        initStateAngleTarget = this.getHeading() - (Math.PI / 3);
      }
    }

    initState.addNext(MoveState, () -> isSameDirection(getHeading(), initStateAngleTarget));
    initState.setStateAction(() -> {
      if (leftSide) {
        if (this.currentRobot == Robots.SRUP) {
          turnLeft();
        } else {
          turnRight();
        }
      } else {
        if (this.currentRobot == Robots.SRUP) {
          turnRight();
        } else {
          turnLeft();
        }
      }
    });

    MoveState.addNext(TurnTowardEnemies, () -> moveacount == 0);
    MoveState.setStateAction(() -> {
      move();
      moveacount--;
    });

    double turnTowardEnemiesTargetDirection;
    if (this.leftSide) {
      turnTowardEnemiesTargetDirection = Parameters.EAST;
    } else {
      turnTowardEnemiesTargetDirection = Parameters.WEST;
    }
    TurnTowardEnemies.addNext(DetecState, () -> isSameDirection(getHeading(), turnTowardEnemiesTargetDirection));
    TurnTowardEnemies.setStateAction(() -> {
      if (this.leftSide) {
        if (this.currentRobot == Robots.SRUP) {
          turnRight();
        } else {
          turnLeft();
        }
      } else {
        if (this.currentRobot == Robots.SRUP) {
          turnLeft();
        } else {
          turnRight();
        }
      }
    });

    DetecState.setStateAction(() -> {
      move_back_count = 100;
      detected = false;
      int nombre_robot_vivivant = 0;
      for (IRadarResult radarResult : detectRadar()) {
        // current hour-minute-second
        if (isOpponentBot(radarResult) && isNotDead(radarResult)) {
          double opponentPosX = this.robotX
              + radarResult.getObjectDistance() * Math.cos(radarResult.getObjectDirection());
          double opponentPosY = this.robotY
              + radarResult.getObjectDistance() * Math.sin(radarResult.getObjectDirection());
          String message = buildOpponentPosMessage(radarResult, opponentPosX, opponentPosY);
          // logger.info(message);
          broadcast(message);
          detected = true;
          nombre_robot_vivivant++;
        }

        if (radarResult.getObjectType() == IRadarResult.Types.BULLET) {
          // bullet_detected = true;
        }
      }
      if (!detected) {
        move();
      }

      while (bullet_detected && move_back_count != 0) {
        System.out.println("move back");
        // moveBack();
        move_back_count--;
      }
      move_back_count = 100;
      bullet_detected = false;
    });

    // Detect wall faire le tour du terain et non pas des aller retour
    if (this.leftSide) {
      DetecState.addNext(STTurnNorth, () -> detectWall() && isSameDirection(getHeading(), Parameters.EAST));
      DetecState.addNext(STTurnNorth,
          () -> detectWall() && isSameDirection(getHeading(), Parameters.NORTH) && currentRobot != Robots.SRUP);
      DetecState.addNext(STTurnWest,
          () -> detectWall() && isSameDirection(getHeading(), Parameters.NORTH) && currentRobot == Robots.SRUP);
      DetecState.addNext(STTurnEast, () -> detectWall() && isSameDirection(getHeading(), Parameters.SOUTH));
      DetecState.addNext(STTurnEast, () -> detectWall() && isSameDirection(getHeading(), Parameters.WEST));
      STTurnNorth.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.NORTH));
      STTurnNorth.setStateAction(() -> {
        turnLeft();
      });
      STTurnWest.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.WEST));
      STTurnWest.setStateAction(() -> {
        turnLeft();
      });
      STTurnEast.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.EAST));
      STTurnEast.setStateAction(() -> {
        turnRight();
      });
    } else {
      DetecState.addNext(STTurnNorth, () -> detectWall() && isSameDirection(getHeading(), Parameters.EAST));
      DetecState.addNext(STTurnSouth,
          () -> detectWall() && isSameDirection(getHeading(), Parameters.SOUTH) && currentRobot == Robots.SRUP);
      DetecState.addNext(STTurnEast,
          () -> detectWall() && isSameDirection(getHeading(), Parameters.SOUTH) && currentRobot != Robots.SRUP);
      DetecState.addNext(STTurnEast, () -> detectWall() && isSameDirection(getHeading(), Parameters.SOUTH));
      DetecState.addNext(STTurnSouth, () -> detectWall() && isSameDirection(getHeading(), Parameters.WEST));
      STTurnSouth.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.SOUTH));
      STTurnSouth.setStateAction(() -> {
        turnLeft();
      });
      STTurnWest.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.WEST));
      STTurnWest.setStateAction(() -> {
        turnLeft();
      });
      STTurnEast.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.EAST));
      STTurnEast.setStateAction(() -> {
        turnRight();
      });
      STTurnNorth.addNext(DetecState, () -> isSameDirection(getHeading(), Parameters.WEST));
      STTurnNorth.setStateAction(() -> {  turnRight(); });
    }

    return initState;
  }

  private String buildOpponentPosMessage(IRadarResult radarResult, double opponentPosX, double opponentPosY) {

    String opponentType = radarResult.getObjectType() == IRadarResult.Types.OpponentMainBot ? "main" : "secondary";
    return Const.OPPONENT_POS_MSG_SIGN
        + Const.MSG_SEPARATOR
        + opponentPosY
        + Const.MSG_SEPARATOR
        + opponentPosX
        + Const.MSG_SEPARATOR
        + getHealth()
        + Const.MSG_SEPARATOR
        + opponentType;
  }

  @Override
  protected void beforeEachStep() {
    super.beforeEachStep();
    this.logRobotPosition();
    sendOpponentPositions();
  }

  private void sendOpponentPositions() {
    List<IRadarResult> opponents = detectOpponents();
    for (IRadarResult radarResult : opponents) {
      double opponentPosX = this.robotX
          + radarResult.getObjectDistance() * Math.cos(radarResult.getObjectDirection());
      double opponentPosY = this.robotY
          + radarResult.getObjectDistance() * Math.sin(radarResult.getObjectDirection());
      String message = buildOpponentPosMessage(radarResult, opponentPosX, opponentPosY);
      broadcast(message);
    }
  }

  /**
   * Verifie si j'ai un ennemi devant mois
   * 
   * @return
   */
  private boolean opponentFrontOfMe() {
    // 1. detecte radar: objets
    // 2. si un enemi est devant moi avec une ouverture de 45°, return vrai
    // 3. sinon return faux
    for (IRadarResult radar : detectRadar()) {
      if (radar.getObjectType() == IRadarResult.Types.OpponentMainBot
          || radar.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
        double direction = normalize(radar.getObjectDirection());
        if (direction > -Math.PI / 8 && direction < Math.PI / 8) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected void afterEachStep() {
    super.afterEachStep();
    this.logRobotPosition();
    sendLogMessage(this.currentState.toString());
  }

}
