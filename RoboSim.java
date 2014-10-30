/**
 * RoboSim: Main simulator logic class.
 */
import java.util.ArrayList; //1301 students: it's an array, you just use "get(i)" instead of "[i]"
import java.util.Random;
import java.lang.reflect.Constructor;
public class RoboSim
{
     /**Exception class*/
     public static class RoboSimExecutionException extends Exception
     {
          public String player;

          public RoboSimExecutionException(String message, String player)
               {
                    this(message,player,-1,-1,-1,-1);
               }

          public RoboSimExecutionException(String message, String player, Robot.GridCell cell)
               {
                    this(message,player,cell.x_coord,cell.y_coord,-1,-1);
               }

          public RoboSimExecutionException(String message, String player, Robot.GridCell cell, Robot.GridCell cell2)
               {
                    this(message,player,cell.x_coord,cell.y_coord,cell2.x_coord,cell2.y_coord);
               }

          public RoboSimExecutionException(String message, String player, int x1, int y1, int x2, int y2)
               {
                    super("Player "+player_+" "+message+(x1!=-1 ? " with robot at coordinates ["+x1+"]["+y1+"]" ? "")+(x2!=-1 ? ", coordinates of invalid cell are ["+x2+"]["+y2+"]"));
                    player = player_;
               }
     }

     private static class RobotData
     {
          public SimGridCell assoc_cell;
          public Robot.Robot_Specs specs;
          public Robot.Robot_Status status;
          public Robot robot;
          public String player;
     }

     private static class SimGridCell extends Robot.GridCell
     {
          public RobotData occupant_data;
          public int wallforthealth;
     }

     //RoboSim environmental constants
     private final int WALL_HEALTH = 50;
     private final int WALL_DEFENSE = 10;

     //RoboSim execution data (world grid, turn order, GUI reference, etc.)
     private SimGrid[][] worldGrid;
     private ArrayList<RobotData> turnOrder;
     private int turnOrder_pos;
     private SimulatorGUI gui;

     //Always good to have an RNG handy
     Random generator;

     private static Robot_Specs checkSpecsValid(Robot_Specs proposed, String player, int skill_points) throws RoboSimExecutionException
          {
               if(proposed.attack + proposed.defense + proposed.power + proposed.charge != skill_points)
                    throw new RoboSimExecutionException("attempted to create invalid robot!",player);
               return proposed;
          }

     /**
      * Constructor for RoboSim:
      * @param combatants array of String objects containing the names of
      *                   the Robot classes for each combatant
      * @param initial_robots_per_combatant how many robots each team starts
      *                                     out with
      * @param skill_points skill points per combatant
      * @param length length of arena
      * @param width width of arena
      * @param obstacles number of obstacles on battlefield
      */
     public RoboSim(String[] combatants, int initial_robots_per_combatant, int skill_points, int length, int width, int obstacles) throws RoboSimExecutionException
          {
               //Create grid
               for(int i=0; i<length; i++)
               {
                    worldGrid[i] = new SimGridCell[width];
                    for(int j=0; j<width; j++)
                    {
                         worldGrid[i][j] = new SimGridCell();
                         worldGrid[i][j].x_coord = i;
                         worldGrid[i][j].y_coord = j;
                         worldGrid[i][j].contents = SimGridCell.GridObject.EMPTY;
                    }
               }

               //Random number generator
               generator = new Random();

               //Initialize array to hold turn order
               turnOrder = new ArrayList<RobotData>(combatants.length*initial_robots_per_combatant);

               //Position in turnOrder
               turnOrder_pos = 0;

               //Add robots for each combatant
               for(String player : combatants)
               {
                    Class playerRobotClass = Class.forName(player);
                    Constructor<Robot> gen_robot = playerRobotClass.getConstructor();
                    for(int i=0; i<initial_robots_per_combatant; i++)
                    {
                         int x_pos,y_pos;
                         do
                         {
                              x_pos = generator.nextInt(length);
                              y_pos = generator.nextInt(width);
                         } while(worldGrid[x_pos][y_pos].contents!=SimGridCell.GridObject.EMPTY);

                         worldGrid[x_pos][y_pos].contents = SimGridCell.GridObject.SELF;
                         RobotData data = worldGrid[x_pos][y_pos].occupant_data = new RobotData();
                         data.assoc_cell = worldGrid[x_pos][y_pos];
                         data.robot = gen_robot.newInstance();
                         data.player = player;
                         byte[] creation_message = new byte[64];
                         byte[1] = turnOrder_pos % 256;
                         byte[0] = turnOrder_pos / 256;
                         data.specs = checkSpecsValid(data.robot.createRobot(null, skill_points, creation_message), player, skill_points);
                         data.status.charge = data.status.health = data.specs.power*10;
                         turnOrder.set(turnOrder_pos++,data);
                    }
               }

               //Add obstacles to battlefield
               for(int i=0 i<obstacles; i++)
               {
                    do
                    {
                         x_pos = generator.nextInt(length);
                         y_pos = generator.nextInt(width);
                    } while(worldGrid[x_pos][y_pos].contents!=SimGridCell.GridObject.EMPTY);
                    worldGrid[x_pos][y_pos].contents = SimGridCell.GridObject.WALL;
                    worldGrid[x_pos][y_pos].wallforthealth = WALL_HEALTH;
               }
          }

     /**
      * The implementing class for the WorldAPI reference.
      * We can't just use ourselves for this because students
      * could downcast us to our real type and manipulate the
      * simulator in untoward ways (not that any of you would
      * do that, and I would catch it when I reviewed your
      * code anyway, but still).
      * 
      * NOTE: THIS IS A *NON-STATIC* INNER CLASS!
      */
     private class RoboAPIImplementor implements WorldAPI
     {
          private RobotData actingRobot;

          /**
           * Private constructor
           * @param actingRobot_ data for robot attempting to act in the simulator
           */
          private RoboAPIImplementor(RobotData actingRobot_) { actingRobot = actingRobot_; }

          /**
           * Utility method to check whether student's robot is adjacent to cell it is attacking.
           * Note: diagonal cells are not adjacent.
           * @param adjacent_cell cell to compare with student's robot's position
           */
          private boolean isAdjacent(Robot.GridCell adjacent_cell)
               {
                    return (Math.abs(actingRobot.assoc_cell.x_coord-adjacent_cell.x_coord)==1 &&
                              actingRobot.assoc_cell.y_coord == adjacent_cell.y_coord ||
                            Math.abs(actingRobot.assoc_cell.y_coord-adjacent_cell.y_coord)==1 &&
                              actingRobot.assoc_cell.x_coord == adjacent_cell.x_coord);
               }

          /**
           * Did the attacker hit the defender?
           * @param attack attack skill of attacker (including bonuses/penalties)
           * @param defense defense skill of defender (including bonuses)
           * @return whether the attacker hit
           */
          private boolean calculateHit(int attack, int defense)
               {
                    int luckOfAttacker = generator.nextInt(10);
                    return luckOfAttacker+attack-defense>=5;
               }

          /**
           * Process attack, assigning damage and deleting destroyed objects if necessary.
           * @param attack attack skill of attacker (including bonuses/penalties)
           * @param cell_to_attack cell attacker is attacking containing enemy or obstacle
           */
          private void processAttack(int attack, SimGridCell cell_to_attack)
               {
                    //Calculate defense skill of opponent
                    int defense = 0;
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.SELF:
                         defense = cell_to_attack.occupant_data.specs.defense + cell_to_attack.occupant_data.status.defense_boost;
                         break;

                    case SimGridCell.GridObject.FORT:
                    case SimGridCell.GridObject.WALL:
                         defense = 10;
                         break;
                    }


                    //If we hit, damage the opponent
                    if(calculateHit(attack,defense))
                         if(cell_to_attack.occupant_data!=null)
                         {
                              //we're a robot
                              for(int i=0; true; i++)
                                        if(turnOrder.get(i)==cell_to_attack.occupant_data.robot)
                                             if((cell_to_attack.occupant_data.status-=power)<=0)
                                             {
                                                  //Handle cell
                                                  cell_to_attack.occupant_data = null;
                                                  cell_to_attack.contents = cell_to_attack.wallforthealth>0 ? SimGridCell.GridObject.FORT : SimGridCell.GridObject.EMPTY;

                                                  //Handle turnOrder position
                                                  turnOrder.erase(i);
                                                  if(i<turnOrder_pos)
                                                       turnOrder_pos--;

                                                  break;
                                             }
                         }
                         else
                              if((cell_to_attack.wallforthealth-=power)<=0)
                              {
                                   cell_to_attack.wallforthealth = 0;
                                   cell_to_attack.contents = SimGridCell.GridObject.EMPTY;
                              }
                                                  
               }

          public Robot.AttackResult meleeAttack(int power, Robot.GridCell adjacent_cell) throws RoboSimExecutionException
               {
                    //Lots of error checking here (as everywhere...)
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null as argument to meleeAttack()");

                    //Check that we're using a valid amount of power
                    if(power > actingRobot.specs.power || power > actingRobot.status.charge ||
                       power > actingRobot.specs.attack || power < 1)
                         throw new RoboSimExecutionException("attempted melee attack with illegal power level");

                    //Are cells adjacent?
                    if(!isAdjacent(adjacent_cell))
                         throw new RoboSimExecutionException("attempted to melee attack nonadjacent cell with robot",actingRobot.player,actingRobot.assoc_cell);

                    //Does cell exist in grid?
                    //(could put this in isAdjacent() method but want to give students more useful error messages)
                    if(adjacent_cell.x_coord > worldGrid.length || adjacent_cell.y_coord > worldGrid[0].length ||
                       adjacent_cell.x_coord < 0 || adjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to meleeAttack()",actingRobot.player,actingRobot.assoc_cell,adjacent_cell);

                    //Safe to use this now, checked for oob condition from student
                    SimGridCell cell_to_attack = worldGrid[adjacent_cell.x_coord][adjacent_cell.y_coord];

                    //Is there an enemy, fort, or wall at the cell's location?
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.EMPTY:
                         throw new RoboSimExecutionException("attempted to attack empty cell",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.BLOCKED:
                         throw new RoboSimExecutionException("attempted to attack blocked tile",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.SELF:
                         if(cell_to_attack.occupant_data.player.equals(actingRobot.player))
                              throw new RoboSimExecutionException("attempted to attack ally",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.CAPSULE:
                         throw new RoboSimExecutionException("attempted to attack energy capsule",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.ALLY:
                         throw new RuntimeException("ERROR in RoboSim.RoboAPIImplementor.meleeAttack().  This is probably not the student's fault.  Contact Patrick Simmons about this message.  (Not the Doobie Brother...)");
                    }

                    //Okay, if we haven't thrown an exception, the cell is valid to attack.  Perform the attack.
                    //Update this robot's charge status.
                    actingRobot.status.power-=power;

                    //Begin calculation of our attack power
                    int raw_attack = actingRobot.specs.attack;

                    //If we're outside a fort attacking someone in the fort, range penalty applies
                    if(cell_to_attack.wallforthealth > 0 && cell_to_attack.occupant_data!=null)
                         raw_attack/=2;

                    //Attack adds power of attack to raw skill
                    int attack = raw_attack + power;

                    //Process attack
                    processAttack(attack,cell_to_attack);
               }

          public Robot.AttackResult rangedAttack(int power, Robot.GridCell nonadjacent_cell) throws RoboSimExecutionException
               {
                    //Lots of error checking here (as everywhere...)
                    if(adjacent_cell==null)
                         throw new RoboSimExecutionException("passed null as argument to rangedAttack()");


                    //Check that we're using a valid amount of power
                    if(power > actingRobot.specs.power || power > actingRobot.status.charge ||
                       power > actingRobot.specs.attack || power < 1)
                         throw new RoboSimExecutionException("attempted ranged attack with illegal power level");

                    //Does cell exist in grid?
                    //(could put this in isAdjacent() method but want to give students more useful error messages)
                    if(nonadjacent_cell.x_coord > worldGrid.length || nonadjacent_cell.y_coord > worldGrid[0].length ||
                       nonadjacent_cell.x_coord < 0 || nonadjacent_cell.y_coord < 0)
                         throw new RoboSimExecutionException("passed invalid cell coordinates to rangedAttack()",actingRobot.player,actingRobot.assoc_cell,nonadjacent_cell);

                    //Are cells nonadjacent?
                    if(isAdjacent(nonadjacent_cell))
                         throw new RoboSimExecutionException("attempted to range attack adjacent cell with robot",actingRobot.player,actingRobot.assoc_cell);

                    //Safe to use this now, checked for oob condition from student
                    SimGridCell cell_to_attack = worldGrid[nonadjacent_cell.x_coord][nonadjacent_cell.y_coord];

                    //Do we have a "clear shot"?

                    //Is there an enemy, fort, or wall at the cell's location?
                    switch(cell_to_attack.contents)
                    {
                    case SimGridCell.GridObject.EMPTY:
                         throw new RoboSimExecutionException("attempted to attack empty cell",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.BLOCKED:
                         throw new RoboSimExecutionException("attempted to attack blocked tile",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.SELF:
                         if(cell_to_attack.occupant_data.player.equals(actingRobot.player))
                              throw new RoboSimExecutionException("attempted to attack ally",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.CAPSULE:
                         throw new RoboSimExecutionException("attempted to attack energy capsule",actingRobot.player,actingRobot.assoc_cell,cell_to_attack);
                    case SimGridCell.GridObject.ALLY:
                         throw new RuntimeException("ERROR in RoboSim.RoboAPIImplementor.meleeAttack().  This is probably not the student's fault.  Contact Patrick Simmons about this message.  (Not the Doobie Brother...)");
                    }

                    //Okay, if we haven't thrown an exception, the cell is valid to attack.  Perform the attack.
                    //Update this robot's charge status.
                    actingRobot.status.power-=power;

                    //Begin calculation of our attack power
                    int raw_attack = actingRobot.specs.attack/2;

                    //Attack adds power of attack to raw skill
                    int attack = raw_attack + power;

                    //Process attack
                    processAttack(attack,cell_to_attack);
               }
     }

     /**
      * Executes one timestep of the simulation.
      * @return the winner, if any, or null
      */
     public String executeSingleTimeStep() throws RoboSimExecutionException
          {
               
          }
}
